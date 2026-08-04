package com.space.antivirus.core.protection

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.domain.repository.BackgroundProtectionPreferences
import com.space.antivirus.domain.repository.BackgroundScanScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Sprint 042. The ordering is the point of this class, so it is what
 * these tests are about — and specifically the failure paths, since the
 * happy path can look correct under any ordering.
 *
 * Hand-written fakes rather than mockk: several of these tests assert on
 * the *sequence* of calls across three collaborators, and a shared
 * recorded log reads far more clearly than interleaved verify blocks.
 */
class ProtectionManagerImplTest {

    private val callLog = mutableListOf<String>()

    private inner class FakeScheduler(
        private val scheduleResult: AppResult<Unit> = AppResult.Success(Unit),
        private val cancelResult: AppResult<Unit> = AppResult.Success(Unit),
    ) : BackgroundScanScheduler {
        var scheduledIntervalHours: Long? = null

        override suspend fun schedulePeriodicScan(intervalHours: Long): AppResult<Unit> {
            callLog += "schedule"
            scheduledIntervalHours = intervalHours
            return scheduleResult
        }

        override suspend fun cancelScheduledScan(): AppResult<Unit> {
            callLog += "cancel"
            return cancelResult
        }
    }

    private inner class FakePreferences : BackgroundProtectionPreferences {
        val enabled = MutableStateFlow(false)
        val interval = MutableStateFlow(24L)
        val lastScheduledAt = MutableStateFlow<Long?>(null)
        val notify = MutableStateFlow(false)

        override val isEnabled: Flow<Boolean> = enabled
        override val intervalHours: Flow<Long> = interval
        override val lastScheduledAtEpochMillis: Flow<Long?> = lastScheduledAt
        override val notifyAfterScan: Flow<Boolean> = notify

        override suspend fun recordEnabled(intervalHours: Long, scheduledAtEpochMillis: Long) {
            callLog += "persistEnabled"
            enabled.value = true
            interval.value = intervalHours
            lastScheduledAt.value = scheduledAtEpochMillis
        }

        override suspend fun recordDisabled() {
            callLog += "persistDisabled"
            enabled.value = false
        }

        override suspend fun setIntervalHours(hours: Long) {
            interval.value = hours
        }

        override suspend fun setNotifyAfterScan(enabled: Boolean) {
            notify.value = enabled
        }
    }

    private inner class FakeNotifier : com.space.antivirus.domain.protection.ProtectionNotifier {
        var scanCompletedCalls = 0
        var lastNextScanEpochMillis: Long? = null

        override fun showProtectionActive(earliestNextScanEpochMillis: Long?) {
            callLog += "notifyActive"
            lastNextScanEpochMillis = earliestNextScanEpochMillis
        }

        override fun hideProtectionActive() {
            callLog += "hideActive"
        }

        override fun showScanCompleted(threatsFound: Int, highRiskFound: Int) {
            callLog += "notifyScanComplete"
            scanCompletedCalls++
        }

        override fun areNotificationsPermitted(): Boolean = true
    }

    private fun manager(
        scheduler: BackgroundScanScheduler,
        preferences: BackgroundProtectionPreferences,
        notifier: FakeNotifier,
    ) = ProtectionManagerImpl(scheduler, preferences, notifier)

    // -- Enabling ------------------------------------------------------

    @Test
    fun `enabling schedules first, then persists, then notifies`() = runTest {
        val preferences = FakePreferences()
        val notifier = FakeNotifier()

        val result = manager(FakeScheduler(), preferences, notifier).enable(24L)

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat(callLog).containsExactly("schedule", "persistEnabled", "notifyActive").inOrder()
    }

    /**
     * The failure that matters most. If scheduling fails, preferences
     * must still say disabled and nothing may tell the user their device
     * is being monitored — otherwise the app claims protection that
     * WorkManager rejected.
     */
    @Test
    fun `a failed schedule persists nothing and notifies nothing`() = runTest {
        val preferences = FakePreferences()
        val notifier = FakeNotifier()
        val scheduler = FakeScheduler(scheduleResult = AppResult.Failure(AppError.StorageUnavailable))

        val result = manager(scheduler, preferences, notifier).enable(24L)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat(callLog).containsExactly("schedule")
        assertThat(preferences.enabled.value).isFalse()
    }

    @Test
    fun `enabling without an interval uses the persisted one`() = runTest {
        val preferences = FakePreferences().apply { interval.value = 168L }
        val scheduler = FakeScheduler()

        manager(scheduler, preferences, FakeNotifier()).enable(null)

        assertThat(scheduler.scheduledIntervalHours).isEqualTo(168L)
    }

    // -- Disabling -----------------------------------------------------

    /**
     * Inverted deliberately: a stale "protection active" notification on
     * a device where protection is off is the worse of the two failure
     * modes, so it goes first and goes regardless.
     */
    @Test
    fun `disabling removes the notification before cancelling`() = runTest {
        val preferences = FakePreferences()

        manager(FakeScheduler(), preferences, FakeNotifier()).disable()

        assertThat(callLog).containsExactly("hideActive", "cancel", "persistDisabled").inOrder()
    }

    @Test
    fun `a failed cancel does not persist disabled`() = runTest {
        val preferences = FakePreferences().apply { enabled.value = true }
        val scheduler = FakeScheduler(cancelResult = AppResult.Failure(AppError.StorageUnavailable))

        val result = manager(scheduler, preferences, FakeNotifier()).disable()

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat(preferences.enabled.value).isTrue()
    }

    // -- Boot ----------------------------------------------------------

    /**
     * WorkManager reschedules its own persisted work after boot, so
     * re-enqueueing here would replace a live schedule and reset its
     * interval window — a user who reboots often would see scans
     * repeatedly deferred.
     */
    @Test
    fun `restoring after boot re-posts the notification without rescheduling`() = runTest {
        val preferences = FakePreferences().apply {
            enabled.value = true
            lastScheduledAt.value = 1_000L
        }

        manager(FakeScheduler(), preferences, FakeNotifier()).restoreAfterBoot()

        assertThat(callLog).containsExactly("notifyActive")
    }

    @Test
    fun `restoring after boot does nothing when protection is off`() = runTest {
        manager(FakeScheduler(), FakePreferences(), FakeNotifier()).restoreAfterBoot()

        assertThat(callLog).isEmpty()
    }

    // -- Scan completion -----------------------------------------------

    @Test
    fun `a completed scan notifies only when the user asked to be told`() = runTest {
        val preferences = FakePreferences().apply {
            enabled.value = true
            notify.value = true
        }
        val notifier = FakeNotifier()

        manager(FakeScheduler(), preferences, notifier).onScheduledScanCompleted(3, 1)

        assertThat(notifier.scanCompletedCalls).isEqualTo(1)
    }

    @Test
    fun `a completed scan stays silent by default`() = runTest {
        val preferences = FakePreferences().apply { enabled.value = true }
        val notifier = FakeNotifier()

        manager(FakeScheduler(), preferences, notifier).onScheduledScanCompleted(3, 1)

        assertThat(notifier.scanCompletedCalls).isEqualTo(0)
    }

    @Test
    fun `a completed scan stays silent when protection has since been disabled`() = runTest {
        val preferences = FakePreferences().apply { notify.value = true }
        val notifier = FakeNotifier()

        manager(FakeScheduler(), preferences, notifier).onScheduledScanCompleted(3, 1)

        assertThat(notifier.scanCompletedCalls).isEqualTo(0)
    }

    // -- Derived state -------------------------------------------------

    @Test
    fun `next scan estimate is derived from the real schedule time`() = runTest {
        val preferences = FakePreferences().apply {
            enabled.value = true
            interval.value = 24L
            lastScheduledAt.value = 1_000L
        }

        val state = manager(FakeScheduler(), preferences, FakeNotifier()).state.first()

        assertThat(state.earliestNextScanEpochMillis).isEqualTo(1_000L + 24 * 3_600_000L)
    }

    /** Never a fabricated time when protection is off. */
    @Test
    fun `next scan estimate is null when protection is disabled`() = runTest {
        val preferences = FakePreferences().apply { lastScheduledAt.value = 1_000L }

        val state = manager(FakeScheduler(), preferences, FakeNotifier()).state.first()

        assertThat(state.earliestNextScanEpochMillis).isNull()
    }
}
