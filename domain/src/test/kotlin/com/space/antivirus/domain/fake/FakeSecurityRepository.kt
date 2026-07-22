package com.space.antivirus.domain.fake

import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.core.model.ScanSession
import com.space.antivirus.core.model.ScanSessionState
import com.space.antivirus.core.model.ScanStatistics
import com.space.antivirus.core.model.ScanType
import com.space.antivirus.core.model.Threat
import com.space.antivirus.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory test double, local to :domain's own test source set (not
 * core:testing — that's an Android module and :domain cannot depend on
 * one, per ADR 0011 / ADR 0005). Deliberately minimal: only enough state
 * management to exercise the UseCase layer's coordination logic, not a
 * realistic persistence simulation.
 */
class FakeSecurityRepository : SecurityRepository {

    private var nextId = 0
    private val sessions = mutableMapOf<String, ScanSession>()
    private val results = mutableMapOf<String, ScanResult>()
    private val historyFlow = MutableStateFlow<List<ScanResult>>(emptyList())

    var forcedFailure: AppError? = null

    override suspend fun createScanSession(scanType: ScanType): AppResult<ScanSession> {
        forcedFailure?.let { return AppResult.Failure(it) }
        val session = ScanSession(
            id = "session-${nextId++}",
            scanType = scanType,
            state = ScanSessionState.PENDING,
            startedAtEpochMillis = 0L,
        )
        sessions[session.id] = session
        return AppResult.Success(session)
    }

    override suspend fun startScanSession(sessionId: String): AppResult<ScanSession> {
        forcedFailure?.let { return AppResult.Failure(it) }
        val session = sessions[sessionId]
            ?: return AppResult.Failure(AppError.ScanSessionNotFound(sessionId))
        val started = session.copy(state = ScanSessionState.RUNNING)
        sessions[sessionId] = started
        return AppResult.Success(started)
    }

    override suspend fun completeScanSession(
        sessionId: String,
        statistics: ScanStatistics,
        threats: List<Threat>,
    ): AppResult<ScanResult> {
        forcedFailure?.let { return AppResult.Failure(it) }
        val session = sessions[sessionId]
            ?: return AppResult.Failure(AppError.ScanSessionNotFound(sessionId))
        val completed = session.copy(
            state = ScanSessionState.COMPLETED,
            completedAtEpochMillis = session.startedAtEpochMillis + statistics.durationMillis,
        )
        sessions[sessionId] = completed
        val result = ScanResult(completed, statistics, threats)
        results[sessionId] = result
        historyFlow.value = listOf(result) + historyFlow.value
        return AppResult.Success(result)
    }

    override suspend fun cancelScanSession(sessionId: String): AppResult<ScanSession> {
        val session = sessions[sessionId]
            ?: return AppResult.Failure(AppError.ScanSessionNotFound(sessionId))
        val cancelled = session.copy(
            state = ScanSessionState.CANCELLED,
            completedAtEpochMillis = session.startedAtEpochMillis,
        )
        sessions[sessionId] = cancelled
        return AppResult.Success(cancelled)
    }

    override suspend fun failScanSession(sessionId: String, reason: String): AppResult<ScanSession> {
        val session = sessions[sessionId]
            ?: return AppResult.Failure(AppError.ScanSessionNotFound(sessionId))
        val failed = session.copy(
            state = ScanSessionState.FAILED,
            completedAtEpochMillis = session.startedAtEpochMillis,
        )
        sessions[sessionId] = failed
        return AppResult.Success(failed)
    }

    override suspend fun getScanSession(sessionId: String): AppResult<ScanSession> =
        sessions[sessionId]?.let { AppResult.Success(it) }
            ?: AppResult.Failure(AppError.ScanSessionNotFound(sessionId))

    override suspend fun getScanResult(sessionId: String): AppResult<ScanResult> =
        results[sessionId]?.let { AppResult.Success(it) }
            ?: AppResult.Failure(AppError.ScanSessionNotFound(sessionId))

    override fun observeScanHistory() = historyFlow.asStateFlow()

    override suspend fun getLatestScanResult(): AppResult<ScanResult?> =
        AppResult.Success(historyFlow.value.firstOrNull())

    override suspend fun deleteScanHistory(sessionId: String): AppResult<Unit> {
        results.remove(sessionId)
        sessions.remove(sessionId)
        historyFlow.value = historyFlow.value.filterNot { it.session.id == sessionId }
        return AppResult.Success(Unit)
    }

    override suspend fun clearScanHistory(): AppResult<Unit> {
        results.clear()
        sessions.clear()
        historyFlow.value = emptyList()
        return AppResult.Success(Unit)
    }
}
