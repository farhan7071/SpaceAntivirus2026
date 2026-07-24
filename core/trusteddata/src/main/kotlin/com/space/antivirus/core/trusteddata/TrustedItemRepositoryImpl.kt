package com.space.antivirus.core.trusteddata

import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.database.dao.TrustedItemDao
import com.space.antivirus.core.model.TrustedItem
import com.space.antivirus.core.model.TrustedItemType
import com.space.antivirus.domain.repository.TrustedItemRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The real TrustedItemRepository implementation — same treatment
 * SecurityRepository got in Sprint 011 (ADR 0024), applied to a much
 * simpler schema: a single standalone table, no foreign keys, no
 * multi-row writes needing transactional atomicity. That's why this
 * module (unlike core:securitydata) doesn't depend on Room directly —
 * it never calls a Room framework API itself, only plain suspend
 * functions on TrustedItemDao. See ADR 0025.
 */
class TrustedItemRepositoryImpl @Inject constructor(
    private val trustedItemDao: TrustedItemDao,
) : TrustedItemRepository {

    override suspend fun addTrustedItem(
        identifier: String,
        type: TrustedItemType,
        reason: String?,
    ): AppResult<TrustedItem> = safeCall {
        // Idempotency (ADR 0021's contract requirement): check first,
        // return the existing item rather than creating a duplicate row.
        trustedItemDao.findByIdentifierAndType(identifier, type.name)?.let {
            return@safeCall AppResult.Success(it.toDomain())
        }

        val item = TrustedItem(
            id = UUID.randomUUID().toString(),
            identifier = identifier,
            type = type,
            addedAtEpochMillis = System.currentTimeMillis(),
            reason = reason,
        )
        trustedItemDao.insert(item.toEntity())
        AppResult.Success(item)
    }

    override suspend fun removeTrustedItem(id: String): AppResult<Unit> = safeCall {
        val deletedCount = trustedItemDao.deleteById(id)
        if (deletedCount == 0) {
            return@safeCall AppResult.Failure(AppError.TrustedItemNotFound(id))
        }
        AppResult.Success(Unit)
    }

    override suspend fun isTrusted(identifier: String): AppResult<Boolean> = safeCall {
        AppResult.Success(trustedItemDao.existsByIdentifier(identifier))
    }

    override fun observeTrustedItems(): Flow<List<TrustedItem>> =
        trustedItemDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    /**
     * Same CancellationException discipline as SecurityRepositoryImpl
     * (ADR 0024) and AnalyzerExecutor (ADR 0019) — caught and immediately
     * rethrown before the general catch, since it extends Exception and
     * a naive catch would silently swallow structured-concurrency
     * cancellation. Third time this exact pattern has been needed in
     * this project; all three now cite each other.
     */
    private suspend fun <T> safeCall(block: suspend () -> AppResult<T>): AppResult<T> =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unexpected(e))
        }
}
