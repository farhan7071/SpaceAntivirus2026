package com.space.antivirus.core.protection

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.ProtectionState
import com.space.antivirus.domain.protection.ProtectionManager
import com.space.antivirus.domain.protection.ProtectionNotifier
import com.space.antivirus.domain.repository.BackgroundProtectionPreferences
import com.space.antivirus.domain.repository.BackgroundScanScheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * The one place background protection is turned on and off — Sprint 042.
 *
 * Deliberately contains no Android framework types at all. Everything
 * platform-specific is behind `ProtectionNotifier` and
 * `BackgroundScanScheduler`, which is what makes the ordering below
 * testable on the JVM — and the ordering is the entire point of this
 * class.
 *
 * **The ordering, and why it is this way.**
 *
 * 1. Ask the scheduler to enqueue the work.
 * 2. Only if that succeeded, persist the new state.
 * 3. Only then, post the notification.
 *
 * Any other order produces a lie. Persisting first means preferences
 * claim protection is on when WorkManager rejected the request;
 * notifying first means the user is told "your device is being
 * monitored" before anything is scheduled to monitor it. Sprint 024/025
 * established this invariant for the Settings path; Sprint 042 makes it
 * structural, so the three new callers cannot get it wrong
 * independently.
 *
 * The disable path deliberately inverts step 3: the notification is
 * removed *first*. If cancellation somehow fails, a stale "protection
 * active" notification left on screen is the worse outcome of the two.
 */
@Singleton
class ProtectionManagerImpl @Inject constructor(
    private val scheduler: BackgroundScanScheduler,
    private val preferences: BackgroundProtectionPreferences,
    private val notifier: ProtectionNotifier,
) : ProtectionManager {

    override val state: Flow<ProtectionState> = combine(
        preferences.isEnabled,
        preferences.intervalHours,
        preferences.lastScheduledAtEpochMillis,
        preferences.notifyAfterScan,
    ) { isEnabled, intervalHours, lastScheduledAt, notifyAfterScan ->
        ProtectionState(
            isEnabled = isEnabled,
            intervalHours = intervalHours,
            lastScheduledAtEpochMillis = lastScheduledAt,
            notifyAfterScan = notifyAfterScan,
        )
    }

    override suspend fun enable(intervalHours: Long?): AppResult<Unit> {
        val hours = intervalHours ?: preferences.intervalHours.first()

        // Step 1. Nothing else happens unless this succeeds.
        when (val scheduled = scheduler.schedulePeriodicScan(hours)) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> return scheduled
            AppResult.Loading -> return AppResult.Loading
        }

        // Step 2. Now — and only now — the persisted state is true.
        val scheduledAt = System.currentTimeMillis()
        preferences.recordEnabled(intervalHours = hours, scheduledAtEpochMillis = scheduledAt)

        // Step 3.
        notifier.showProtectionActive(
            earliestNextScanEpochMillis = scheduledAt + hours * MILLIS_PER_HOUR,
        )
        return AppResult.Success(Unit)
    }

    override suspend fun disable(): AppResult<Unit> {
        // Removed first: a stale "protection active" notification on a
        // device where protection is off is the worse of the two failure
        // modes, so it goes even if cancellation then fails.
        notifier.hideProtectionActive()

        when (val cancelled = scheduler.cancelScheduledScan()) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> return cancelled
            AppResult.Loading -> return AppResult.Loading
        }

        preferences.recordDisabled()
        return AppResult.Success(Unit)
    }

    override suspend fun restoreAfterBoot(): AppResult<Unit> {
        val current = state.first()
        if (!current.isEnabled) return AppResult.Success(Unit)

        // Deliberately does NOT re-enqueue. WorkManager reschedules its
        // own persisted periodic work after boot — that is what the
        // manifest's RECEIVE_BOOT_COMPLETED permission has been for
        // since Sprint 025. Re-enqueueing here would replace a live
        // schedule and reset its interval window, so a user who reboots
        // often would see scans repeatedly deferred. The notification is
        // the thing that genuinely does not survive a reboot.
        notifier.showProtectionActive(current.earliestNextScanEpochMillis)
        return AppResult.Success(Unit)
    }

    override suspend fun onScheduledScanCompleted(threatsFound: Int, highRiskFound: Int) {
        val current = state.first()
        if (!current.isEnabled || !current.notifyAfterScan) return

        // Deliberately does not also re-post the ongoing status
        // notification. It is ongoing — it is already on screen — and
        // re-posting it would not make its "next scan" line any more
        // accurate: that estimate is derived from when the periodic work
        // was originally enqueued, which a completed run does not
        // change. Posting a second notification to convey nothing new is
        // exactly the noise this sprint is meant to avoid.
        notifier.showScanCompleted(threatsFound = threatsFound, highRiskFound = highRiskFound)
    }

    override suspend fun setNotifyAfterScan(enabled: Boolean) {
        preferences.setNotifyAfterScan(enabled)
    }

    private companion object {
        const val MILLIS_PER_HOUR = 3_600_000L
    }
}
