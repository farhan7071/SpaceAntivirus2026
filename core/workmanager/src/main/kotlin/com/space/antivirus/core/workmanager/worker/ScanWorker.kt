package com.space.antivirus.core.workmanager.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanRequest
import com.space.antivirus.core.model.ScanScope
import com.space.antivirus.core.model.ScanType
import com.space.antivirus.domain.protection.ProtectionManager
import com.space.antivirus.domain.usecase.RunScanRequestUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID

/**
 * The production background-protection worker — reuses
 * RunScanRequestUseCase (Sprint 007+) directly, the exact same
 * orchestration a manual "Scan Now" tap goes through (ScanViewModel,
 * Sprint 020). No second scan implementation exists; this class's only
 * job is triggering the same real pipeline on a schedule instead of a
 * tap, and translating its AppResult into WorkManager's own Result type.
 *
 * CoroutineWorker specifically (not the older callback-based Worker) —
 * "lifecycle-safe execution" per this sprint's own requirement comes
 * from CoroutineWorker's built-in behavior: doWork() runs on a
 * WorkManager-managed coroutine scope tied to the worker's own
 * lifecycle, automatically cancelled if the system stops the worker.
 * Nothing extra was built for this; using CoroutineWorker is what
 * "Android's recommended architecture" (this sprint's own phrase) means
 * in practice.
 *
 * Uses the same scope/type a manual scan uses (ScanScope.InstalledApplications,
 * ScanType.QUICK) — the only scope any real ThreatAnalyzer (Sprints
 * 014/015) can evaluate; automatic background scans check the same thing
 * a manual scan does, not something different or lesser.
 *
 * AppResult -> WorkManager Result mapping, each branch reasoned
 * separately rather than collapsed to one default:
 * - Success -> Result.success(): the scan ran, regardless of whether it
 *   found anything.
 * - Failure(ScanAlreadyInProgress) -> Result.success(): the concurrent-
 *   scan guard (ADR 0020) means a scan — manual or another background
 *   run — is already covering this cycle. That's the guard working as
 *   designed, not this worker failing; treating it as a failure would
 *   trigger pointless retries against a condition retrying can't fix.
 * - Failure(PermissionMissing) -> Result.failure(): retrying won't
 *   grant a permission. WorkManager should not keep re-attempting this
 *   until something external changes (the user granting it), which is
 *   exactly what Result.failure() (not retry()) communicates.
 * - Any other Failure -> Result.retry(): the honest default for
 *   failures that plausibly resolve on their own (e.g. transient
 *   storage unavailability), letting WorkManager's own backoff policy
 *   handle timing.
 */
@HiltWorker
class ScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val runScanRequest: RunScanRequestUseCase,
    private val protectionManager: ProtectionManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val request = ScanRequest(
            id = UUID.randomUUID().toString(),
            scanType = ScanType.QUICK,
            scopes = listOf(ScanScope.InstalledApplications),
            createdAtEpochMillis = System.currentTimeMillis(),
        )

        return when (val result = runScanRequest(request)) {
            is AppResult.Success -> {
                // Sprint 042: report the real outcome, from this scan's
                // own ScanResult. The manager stays silent unless the
                // user asked to be told, so this is safe to call
                // unconditionally — and passing counts rather than a
                // message means the notification cannot claim more than
                // the scan actually found.
                protectionManager.onScheduledScanCompleted(
                    threatsFound = result.data.threats.size,
                    highRiskFound = result.data.threats.count { it.riskLevel == RiskLevel.ACTION_NEEDED },
                )
                Result.success()
            }
            is AppResult.Failure -> when (result.error) {
                is AppError.ScanAlreadyInProgress -> Result.success()
                is AppError.PermissionMissing -> Result.failure()
                else -> Result.retry()
            }
            AppResult.Loading -> Result.retry()
        }
    }

    companion object {
        /** Shared with WorkManagerBackgroundScanScheduler — the same
         *  unique work name both enqueue and cancel operations must
         *  agree on for WorkManager to recognize them as the same work. */
        const val UNIQUE_WORK_NAME = "background_protection_scan"
    }
}
