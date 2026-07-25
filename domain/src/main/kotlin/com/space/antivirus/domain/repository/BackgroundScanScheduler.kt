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
 * interface describes WHAT can be scheduled, not HOW OFTEN or under
 * WHAT conditions; those are implementation details of the real
 * scheduler (currently: once daily, requires the device to be idle —
 * see WorkManagerBackgroundScanScheduler's own KDoc), kept out of the
 * domain contract so they can change without touching this interface
 * or anything depending on it.
 */
interface BackgroundScanScheduler {

    /** Idempotent — scheduling an already-scheduled periodic scan
     *  replaces the existing schedule rather than creating a duplicate,
     *  matching WorkManager's own KEEP/REPLACE policy semantics the real
     *  implementation chooses. */
    suspend fun schedulePeriodicScan(): AppResult<Unit>

    /** Idempotent — cancelling when nothing is scheduled is a no-op
     *  success, not a failure. */
    suspend fun cancelScheduledScan(): AppResult<Unit>
}
