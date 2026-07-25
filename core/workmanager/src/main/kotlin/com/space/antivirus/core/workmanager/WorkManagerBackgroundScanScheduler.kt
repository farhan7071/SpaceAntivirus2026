package com.space.antivirus.core.workmanager

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
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
 * SCAN_INTERVAL: once every 24 hours. No product requirement specified a
 * cadence; this is a deliberate, reasonable default for "background
 * protection" — frequent enough to be meaningful, far above WorkManager's
 * hard 15-minute platform minimum so it doesn't behave like an
 * unreasonably aggressive battery/resource consumer for an antivirus
 * app. Not wired to any Settings UI yet (no such screen exists), so this
 * is a fixed constant for now, not a configurable value — a reasonable
 * next increment once Settings exists to expose it.
 *
 * CONSTRAINTS: only setRequiresBatteryNotLow(true) — RunScanRequestUseCase's
 * entire pipeline is on-device (no network calls anywhere in it, Sprints
 * 004B–021), so a network constraint would be an incorrect restriction,
 * not a sensible one. Battery-not-low is the one genuinely relevant
 * constraint for background work that shouldn't run during genuinely low
 * battery.
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

    override suspend fun schedulePeriodicScan(): AppResult<Unit> = try {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<ScanWorker>(SCAN_INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(constraints)
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

    override suspend fun cancelScheduledScan(): AppResult<Unit> = try {
        WorkManager.getInstance(context).cancelUniqueWork(ScanWorker.UNIQUE_WORK_NAME)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(AppError.Unexpected(e))
    }

    private companion object {
        const val SCAN_INTERVAL_HOURS = 24L
    }
}
