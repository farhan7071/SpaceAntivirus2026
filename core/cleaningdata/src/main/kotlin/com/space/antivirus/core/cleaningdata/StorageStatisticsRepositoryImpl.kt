package com.space.antivirus.core.cleaningdata

import android.content.Context
import android.os.StatFs
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.StorageStatistics
import com.space.antivirus.domain.repository.StorageStatisticsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Device storage totals via `StatFs` — Sprint 039.
 *
 * Measured against the app's own `filesDir`, which needs no permission
 * and reports the volume that directory lives on. The numbers therefore
 * describe internal storage as a whole (matching Android's own Settings
 * > Storage), not merely this app's share of it — `StatFs` reports the
 * filesystem, not the caller's quota.
 *
 * `availableBytes` is used rather than `freeBytes`: the latter includes
 * blocks reserved for the system that an app can never actually use, so
 * reporting it would overstate what a cleanup could ever reclaim.
 */
class StorageStatisticsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : StorageStatisticsRepository {

    override suspend fun getStorageStatistics(): AppResult<StorageStatistics> = try {
        val statFs = StatFs(context.filesDir.absolutePath)
        AppResult.Success(
            StorageStatistics(
                totalBytes = statFs.totalBytes,
                freeBytes = statFs.availableBytes,
            ),
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (e: IllegalArgumentException) {
        // StatFs throws this for a path it cannot stat — an unreadable
        // or unmounted volume, which is exactly StorageUnavailable.
        AppResult.Failure(AppError.StorageUnavailable)
    } catch (e: Exception) {
        AppResult.Failure(AppError.Unexpected(e))
    }
}
