package com.space.antivirus.domain.repository

import com.space.antivirus.core.common.AppResult

/**
 * Contract for the "automatic background protection" scheduling
 * capability (Phase D, Sprint 024) — same discipline as every prior
 * repository in this project: pure Kotlin, no Android/WorkManager type
 * ever crosses this boundary. The real implementation
 * (WorkManagerBackgroundScanScheduler, core:workmanager) is the only
 * place WorkManager APIs are touched — UseCases and anything above them
 * never see them directly.
 *
 * Deliberately just two operations — schedule and cancel. This
 * interface describes WHAT can be scheduled, not under WHAT conditions
 * (device idle, battery state); those stay implementation details of the
 * real scheduler, kept out of the domain contract so they can change
 * without touching this interface or anything depending on it.
 *
 * Sprint 025: schedulePeriodicScan's interval became configurable rather
 * than a hardcoded implementation constant — this is what "user-
 * configurable scheduling infrastructure" means at this layer: the
 * CAPABILITY to request a specific interval exists and is fully real,
 * even though nothing yet lets a user choose one (no Settings UI exists
 * — same "build the foundation, not the activation" discipline ADR 0037
 * already established for scheduling itself). See ADR 0038.
 */
interface BackgroundScanScheduler {

    /** Idempotent — scheduling an already-scheduled periodic scan
     *  replaces the existing schedule rather than creating a duplicate,
     *  matching WorkManager's own KEEP/REPLACE policy semantics the real
     *  implementation chooses.
     *
     *  Returns AppError.InvalidScheduleConfiguration if intervalHours is
     *  below MIN_INTERVAL_HOURS — validated at this layer rather than
     *  trusting WorkManager's own internal clamping, since a caller
     *  should get an honest, explicit rejection rather than silently
     *  having their requested interval altered underneath them. */
    suspend fun schedulePeriodicScan(intervalHours: Long = DEFAULT_INTERVAL_HOURS): AppResult<Unit>

    /** Idempotent — cancelling when nothing is scheduled is a no-op
     *  success, not a failure. */
    suspend fun cancelScheduledScan(): AppResult<Unit>

    companion object {
        /** Unchanged from Sprint 024's original hardcoded value — no
         *  product requirement specified a cadence; this remains a
         *  deliberate, reasonable default. */
        const val DEFAULT_INTERVAL_HOURS = 24L

        /** Well above WorkManager's own hard 15-minute platform minimum
         *  for periodic work (PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS)
         *  — expressed in whole hours since that's this contract's own
         *  unit, so even the smallest valid value here is safely clear
         *  of the platform floor regardless of WorkManager's exact
         *  internal clamping behavior. */
        const val MIN_INTERVAL_HOURS = 1L
    }
}
