package com.space.antivirus.core.cleaningdata

import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.database.dao.CleanupRecordDao
import com.space.antivirus.core.model.CleanupRecord
import com.space.antivirus.domain.repository.CleanupHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed cleanup history — Sprint 039. Same shape as
 * TrustedItemRepositoryImpl (Sprint 012): a single DAO, mapping
 * delegated to CleanupRecordEntityMappers, no orchestration.
 */
class CleanupHistoryRepositoryImpl @Inject constructor(
    private val cleanupRecordDao: CleanupRecordDao,
) : CleanupHistoryRepository {

    override suspend fun record(record: CleanupRecord): AppResult<Unit> = safeCall {
        cleanupRecordDao.insert(record.toEntity())
        AppResult.Success(Unit)
    }

    override fun observeHistory(): Flow<List<CleanupRecord>> =
        cleanupRecordDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun latest(): AppResult<CleanupRecord?> = safeCall {
        AppResult.Success(cleanupRecordDao.latest()?.toDomain())
    }

    /** Same CancellationException discipline as every repository in this
     *  project since ADR 0024. */
    private suspend fun <T> safeCall(block: suspend () -> AppResult<T>): AppResult<T> =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unexpected(e))
        }
}
