package com.space.antivirus.domain.repository

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.StorageStatistics

/**
 * Device storage totals — Sprint 039.
 *
 * Sprint 038 omitted the reference design's storage overview because no
 * such capability existed anywhere in the project. It turns out to need
 * no permission at all (`StatFs` against an app-private path reports the
 * whole volume), so this closes that gap honestly rather than leaving a
 * documented omission standing longer than it had to.
 */
interface StorageStatisticsRepository {
    suspend fun getStorageStatistics(): AppResult<StorageStatistics>
}
