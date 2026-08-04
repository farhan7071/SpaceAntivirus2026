package com.space.antivirus.domain.protection

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.ProtectionState
import kotlinx.coroutines.flow.Flow

/**
 * The single owner of background protection — Sprint 042.
 *
 * Before this sprint, "background protection" meant a scheduler, a
 * preferences store, and five use cases that `SettingsViewModel` was
 * responsible for calling in the right order. That worked while Settings
 * was the only caller. Sprint 042 adds three more (Home's toggle, the
 * boot receiver, the worker reporting its own result), and four callers
 * each re-deriving "schedule, then persist only on confirmed success,
 * then update the notification" is how they end up disagreeing.
 *
 * So the ordering lives here, once, and every caller gets one method.
 * The existing use cases and `BackgroundScanScheduler` are unchanged and
 * still do the work — this coordinates them rather than replacing them.
 *
 * **Persist only on confirmed success** is inherited from Sprint 024/025
 * and is the invariant that makes `ProtectionState` trustworthy: nothing
 * is written to preferences, and no notification is posted, until the
 * scheduler has actually confirmed the work is enqueued. A UI reading
 * this state is reading what WorkManager really has, not what the app
 * intended.
 */
interface ProtectionManager {

    /** The live state every protection-aware screen observes. */
    val state: Flow<ProtectionState>

    /**
     * Schedules periodic scanning, persists the new state, and posts the
     * ongoing status notification — in that order, and only advancing on
     * success.
     *
     * Idempotent: enabling while already enabled re-schedules under the
     * same unique work name, which WorkManager replaces rather than
     * duplicating.
     */
    suspend fun enable(intervalHours: Long? = null): AppResult<Unit>

    /** Cancels the scheduled work, persists disabled, and removes the
     *  ongoing notification. */
    suspend fun disable(): AppResult<Unit>

    /**
     * Re-establishes protection after a device restart.
     *
     * WorkManager already reschedules its own persisted periodic work
     * after boot (that is what the manifest's RECEIVE_BOOT_COMPLETED
     * permission has been for since Sprint 025), so this deliberately
     * does NOT re-enqueue work — doing so would replace a live schedule
     * and reset its interval window for no reason. What genuinely does
     * not survive a reboot is the ongoing notification, which is what
     * this restores.
     *
     * A no-op when protection is disabled.
     */
    suspend fun restoreAfterBoot(): AppResult<Unit>

    /**
     * Called by the scheduled worker once a scan has finished, with the
     * real counts from that scan's own `ScanResult`.
     *
     * Silent unless the user asked to be told. Notifying after every
     * scan by default is the single easiest way to become a security app
     * people mute.
     */
    suspend fun onScheduledScanCompleted(threatsFound: Int, highRiskFound: Int)

    /** Persists the notify-after-scan preference. */
    suspend fun setNotifyAfterScan(enabled: Boolean)
}
