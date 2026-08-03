package com.space.antivirus.domain.repository

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.CleanupRecord
import kotlinx.coroutines.flow.Flow

/**
 * Persistence for completed cleanups — Sprint 039, and the reason the
 * Cleaner can finally show a real "Last cleanup" line that Sprint 038
 * had to omit for want of any stored data.
 *
 * Records outcome totals only, never file paths — see `CleanupRecord`.
 */
interface CleanupHistoryRepository {

    suspend fun record(record: CleanupRecord): AppResult<Unit>

    /** Most recent first. */
    fun observeHistory(): Flow<List<CleanupRecord>>

    /** The most recent cleanup, or null if the user has never run one. */
    suspend fun latest(): AppResult<CleanupRecord?>
}
