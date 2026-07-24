package com.space.antivirus.core.securitydata

import androidx.room.withTransaction
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.database.AppDatabase
import com.space.antivirus.core.database.dao.DetectionDao
import com.space.antivirus.core.database.dao.ScanSessionDao
import com.space.antivirus.core.database.dao.ScanStatisticsDao
import com.space.antivirus.core.database.dao.ThreatDao
import com.space.antivirus.core.database.entity.ScanSessionEntity
import com.space.antivirus.core.model.ScanProgress
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.core.model.ScanSession
import com.space.antivirus.core.model.ScanSessionState
import com.space.antivirus.core.model.ScanStatistics
import com.space.antivirus.core.model.ScanType
import com.space.antivirus.core.model.Threat
import com.space.antivirus.domain.repository.SecurityRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * The real SecurityRepository implementation — the piece Sprint 004A
 * deferred and ADR 0014 named the pattern for. Built against Sprint
 * 010's Room schema (ADR 0023): entities/DAOs stay at the raw level, all
 * entity<->domain mapping lives in SecurityEntityMappers.kt, and this
 * class is the only place that orchestrates multiple DAOs together.
 *
 * ScanProgress is NOT persisted (ADR 0023's decision, honored here) — it
 * lives in an in-memory ConcurrentHashMap<sessionId, MutableStateFlow>.
 * ConcurrentHashMap specifically (not a plain mutableMapOf) because this
 * is a real Hilt @Singleton running in a genuinely multi-threaded app,
 * unlike FakeSecurityRepository's single-threaded test double — a plain
 * HashMap under concurrent getOrPut calls from multiple scan-adjacent
 * coroutines would be a real, if intermittent, bug.
 */
class SecurityRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val scanSessionDao: ScanSessionDao,
    private val scanStatisticsDao: ScanStatisticsDao,
    private val threatDao: ThreatDao,
    private val detectionDao: DetectionDao,
) : SecurityRepository {

    private val progressFlows = ConcurrentHashMap<String, MutableStateFlow<ScanProgress>>()

    override suspend fun createScanSession(scanType: ScanType): AppResult<ScanSession> = safeCall {
        val session = ScanSession(
            id = UUID.randomUUID().toString(),
            scanType = scanType,
            state = ScanSessionState.PENDING,
            startedAtEpochMillis = System.currentTimeMillis(),
        )
        scanSessionDao.upsert(session.toEntity())
        AppResult.Success(session)
    }

    override suspend fun startScanSession(sessionId: String): AppResult<ScanSession> = safeCall {
        val current = scanSessionDao.getById(sessionId)?.toDomain()
            ?: return@safeCall AppResult.Failure(AppError.ScanSessionNotFound(sessionId))
        if (current.state != ScanSessionState.PENDING) {
            return@safeCall AppResult.Failure(
                AppError.InvalidScanConfiguration(
                    "Session $sessionId must be PENDING to start (was ${current.state})",
                ),
            )
        }
        val started = current.copy(state = ScanSessionState.RUNNING)
        scanSessionDao.upsert(started.toEntity())
        AppResult.Success(started)
    }

    override suspend fun completeScanSession(
        sessionId: String,
        statistics: ScanStatistics,
        threats: List<Threat>,
    ): AppResult<ScanResult> = safeCall {
        val current = scanSessionDao.getById(sessionId)?.toDomain()
            ?: return@safeCall AppResult.Failure(AppError.ScanSessionNotFound(sessionId))

        val completed = current.copy(
            state = ScanSessionState.COMPLETED,
            completedAtEpochMillis = current.startedAtEpochMillis + statistics.durationMillis,
        )

        // Session + statistics + threats + detections must land together
        // or not at all — a session marked COMPLETED with no statistics
        // row (e.g. process death mid-write) would be a real data
        // integrity gap, not just a cosmetic one.
        appDatabase.withTransaction {
            scanSessionDao.upsert(completed.toEntity())
            scanStatisticsDao.upsert(statistics.toEntity(sessionId))
            threatDao.insertAll(threats.map { it.toEntity(sessionId) })
            threats.forEach { threat ->
                if (threat.detections.isNotEmpty()) {
                    detectionDao.insertAll(threat.detections.map { it.toEntity(threat.id) })
                }
            }
        }

        AppResult.Success(ScanResult(completed, statistics, threats))
    }

    override suspend fun cancelScanSession(sessionId: String): AppResult<ScanSession> = safeCall {
        val current = scanSessionDao.getById(sessionId)?.toDomain()
            ?: return@safeCall AppResult.Failure(AppError.ScanSessionNotFound(sessionId))
        val cancelled = current.copy(
            state = ScanSessionState.CANCELLED,
            completedAtEpochMillis = System.currentTimeMillis(),
        )
        scanSessionDao.upsert(cancelled.toEntity())
        AppResult.Success(cancelled)
    }

    override suspend fun failScanSession(sessionId: String, reason: String): AppResult<ScanSession> = safeCall {
        val current = scanSessionDao.getById(sessionId)?.toDomain()
            ?: return@safeCall AppResult.Failure(AppError.ScanSessionNotFound(sessionId))
        val failed = current.copy(
            state = ScanSessionState.FAILED,
            completedAtEpochMillis = System.currentTimeMillis(),
        )
        scanSessionDao.upsert(failed.toEntity())
        AppResult.Success(failed)
    }

    override suspend fun getScanSession(sessionId: String): AppResult<ScanSession> = safeCall {
        val entity = scanSessionDao.getById(sessionId)
            ?: return@safeCall AppResult.Failure(AppError.ScanSessionNotFound(sessionId))
        AppResult.Success(entity.toDomain())
    }

    override suspend fun getScanResult(sessionId: String): AppResult<ScanResult> = safeCall {
        val sessionEntity = scanSessionDao.getById(sessionId)
            ?: return@safeCall AppResult.Failure(AppError.ScanSessionNotFound(sessionId))
        val session = sessionEntity.toDomain()
        if (session.state !in ScanSession.TERMINAL_STATES) {
            return@safeCall AppResult.Failure(
                AppError.InvalidScanConfiguration(
                    "Session $sessionId has not reached a terminal state yet (was ${session.state})",
                ),
            )
        }
        val statisticsEntity = scanStatisticsDao.getBySessionId(sessionId)
            ?: return@safeCall AppResult.Failure(
                AppError.InvalidScanConfiguration("Session $sessionId has no statistics recorded"),
            )
        AppResult.Success(ScanResult(session, statisticsEntity.toDomain(), threatsForSession(sessionId)))
    }

    /**
     * Room's Flow invalidation tracks the scan_sessions TABLE this Flow
     * queries — this only re-emits correctly because completeScanSession
     * always writes to scan_sessions in the SAME transaction as any
     * statistics/threats change (see the withTransaction block above).
     * If a future change ever wrote statistics/threats independently of
     * their owning session row, this Flow would silently go stale. See
     * ADR 0024.
     */
    override fun observeScanHistory(): Flow<List<ScanResult>> =
        scanSessionDao.observeCompleted().map { entities -> entities.mapNotNull { assembleOrNull(it) } }

    override suspend fun getLatestScanResult(): AppResult<ScanResult?> = safeCall {
        val entity = scanSessionDao.getMostRecentCompleted()
            ?: return@safeCall AppResult.Success(null)
        AppResult.Success(assembleOrNull(entity))
    }

    override suspend fun deleteScanHistory(sessionId: String): AppResult<Unit> = safeCall {
        // CASCADE (ADR 0023) removes statistics/threats/detections in the
        // same SQLite-level atomic delete — no separate transaction needed.
        scanSessionDao.deleteById(sessionId)
        progressFlows.remove(sessionId)
        AppResult.Success(Unit)
    }

    override suspend fun clearScanHistory(): AppResult<Unit> = safeCall {
        scanSessionDao.clearAll()
        progressFlows.clear()
        AppResult.Success(Unit)
    }

    override fun observeScanProgress(sessionId: String): Flow<ScanProgress> =
        progressFlows.getOrPut(sessionId) { MutableStateFlow(ScanProgress.starting(sessionId)) }.asStateFlow()

    override suspend fun updateScanProgress(progress: ScanProgress): AppResult<Unit> = safeCall {
        progressFlows.getOrPut(progress.sessionId) { MutableStateFlow(progress) }.value = progress
        AppResult.Success(Unit)
    }

    override suspend fun getActiveScanSession(): AppResult<ScanSession?> = safeCall {
        AppResult.Success(scanSessionDao.getActive()?.toDomain())
    }

    private suspend fun threatsForSession(sessionId: String): List<Threat> =
        threatDao.getBySessionId(sessionId).map { threatEntity ->
            val detections = detectionDao.getByThreatId(threatEntity.id).map { it.toDomain() }
            threatEntity.toDomain(detections)
        }

    private suspend fun assembleOrNull(sessionEntity: ScanSessionEntity): ScanResult? {
        val statisticsEntity = scanStatisticsDao.getBySessionId(sessionEntity.id) ?: return null
        return ScanResult(sessionEntity.toDomain(), statisticsEntity.toDomain(), threatsForSession(sessionEntity.id))
    }

    /**
     * CancellationException must be caught and immediately rethrown,
     * never converted to AppResult.Failure — same rule, same reasoning,
     * as AnalyzerExecutor (ADR 0019). It extends Exception, so a naive
     * catch (e: Exception) would silently swallow structured-concurrency
     * cancellation of whatever coroutine called into this repository.
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
