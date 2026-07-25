package com.space.antivirus.core.workmanager

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.workmanager.worker.ScanWorker
import com.space.antivirus.domain.repository.BackgroundScanScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * The real BackgroundScanScheduler implementation — the only place in
 * this project that touches WorkManager APIs directly, matching the same
 * "contract in domain, real implementation in its own module" pattern
 * every prior repository has followed since Sprint 004B.
 *
 * SCAN_INTERVAL (Sprint 025): now a caller-provided parameter, validated
 * against BackgroundScanScheduler.MIN_INTERVAL_HOURS, rather than a
 * hardcoded implementation constant. DEFAULT_INTERVAL_HOURS (still 24,
 * unchanged from Sprint 024's original value — no product requirement
 * specified a cadence) lives on the domain interface's companion object
 * now, not here, since "what the default is" is a contract-level fact
 * callers need to know, not an implementation detail.
 *
 * CONSTRAINTS: setRequiresBatteryNotLow(true) — RunScanRequestUseCase's
 * entire pipeline is on-device (no network calls anywhere in it, Sprints
 * 004B–021), so a network constraint would be an incorrect restriction,
 * not a sensible one. setRequiresStorageNotLow(true) added in Sprint 025
 * — reasonable general hygiene for any background work, not specific to
 * scan content, since this app also does file enumeration work
 * (EnumerationRepository) that shouldn't run when storage is already
 * critically constrained.
 *
 * BACKOFF (Sprint 025, made explicit rather than left as an implicit
 * WorkManager default): EXPONENTIAL starting at WorkRequest.MIN_BACKOFF_MILLIS
 * — the standard, conservative choice for a background task where a
 * failure is more likely transient (Result.retry() in ScanWorker is only
 * reached for failures ADR 0037 judged plausibly self-resolving) than
 * something that benefits from hammering the retry immediately.
 *
 * ExistingPeriodicWorkPolicy.REPLACE — matches the domain contract's own
 * stated idempotency: re-scheduling replaces rather than duplicates.
 * Chosen over the newer UPDATE policy specifically because REPLACE has
 * been stable and available since WorkManager's earliest releases,
 * removing any doubt about exact version-introduced API availability at
 * this project's pinned WorkManager version.
 *
 * Neither method awaits WorkManager's Operation result — enqueueing (or
 * cancelling) without an immediate exception is treated as success. Doing
 * otherwise would need either a manual suspendCancellableCoroutine
 * wrapper around Operation's ListenableFuture or an additional
 * kotlinx-coroutines-guava dependency this sprint doesn't otherwise need;
 * WorkManager's own enqueue/cancel calls are reliable enough in practice
 * that this is the standard, accepted pattern for this integration.
 */
class WorkManagerBackgroundScanScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : BackgroundScanScheduler {

    override suspend fun schedulePeriodicScan(intervalHours: Long): AppResult<Unit> {
        if (intervalHours < BackgroundScanScheduler.MIN_INTERVAL_HOURS) {
            return AppResult.Failure(
                AppError.InvalidScheduleConfiguration(
                    "intervalHours must be at least ${BackgroundScanScheduler.MIN_INTERVAL_HOURS}, " +
                        "got $intervalHours",
                ),
            )
        }

        return try {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<ScanWorker>(intervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                ScanWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request,
            )
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unexpected(e))
        }
    }

    override suspend fun cancelScheduledScan(): AppResult<Unit> = try {
        WorkManager.getInstance(context).cancelUniqueWork(ScanWorker.UNIQUE_WORK_NAME)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(AppError.Unexpected(e))
    }
}
