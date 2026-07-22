package com.space.antivirus.domain.repository

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.core.model.ScanSession
import com.space.antivirus.core.model.ScanStatistics
import com.space.antivirus.core.model.ScanType
import com.space.antivirus.core.model.Threat
import kotlinx.coroutines.flow.Flow

/**
 * Contract for everything the Security domain needs to persist and query.
 * This interface is the ONLY thing UseCases in this module know about —
 * they never see Room, DataStore, or any Android type. The implementation
 * (core:data, wiring in the real database) is Sprint 004B's job; this
 * sprint defines the shape only, per Sprint 004A's explicit scope.
 *
 * Every method that can fail returns AppResult<T> (ADR 0007) rather than
 * throwing — including "not found" cases, via AppError.ScanSessionNotFound
 * (ADR 0013), so a caller can handle "no such session" as an ordinary,
 * exhaustively-checked branch rather than a caught exception.
 */
interface SecurityRepository {

    /** Creates a new ScanSession in PENDING state and persists it. */
    suspend fun createScanSession(scanType: ScanType): AppResult<ScanSession>

    /** Transitions an existing session to RUNNING. Fails with
     *  AppError.ScanSessionNotFound if no such session exists, or
     *  AppError.InvalidScanConfiguration if the session isn't in a state
     *  that can start (see ScanSession's documented transitions). */
    suspend fun startScanSession(sessionId: String): AppResult<ScanSession>

    /** Transitions a RUNNING session to COMPLETED and persists the final
     *  report. Fails with AppError.ScanSessionNotFound if no such session
     *  exists. */
    suspend fun completeScanSession(
        sessionId: String,
        statistics: ScanStatistics,
        threats: List<Threat>,
    ): AppResult<ScanResult>

    /** Transitions a session to CANCELLED from PENDING or RUNNING. */
    suspend fun cancelScanSession(sessionId: String): AppResult<ScanSession>

    /** Transitions a RUNNING session to FAILED. */
    suspend fun failScanSession(sessionId: String, reason: String): AppResult<ScanSession>

    /** Looks up a single session by id, regardless of state. */
    suspend fun getScanSession(sessionId: String): AppResult<ScanSession>

    /** The completed-scan report for a session, once it exists.
     *  Fails with AppError.ScanSessionNotFound if the session doesn't
     *  exist, or AppError.InvalidScanConfiguration if it exists but
     *  hasn't reached a terminal state yet (no report to return). */
    suspend fun getScanResult(sessionId: String): AppResult<ScanResult>

    /** All completed scan reports, most recent first. A Flow (not a
     *  suspend function) because this backs a live-updating History
     *  screen in a later sprint. */
    fun observeScanHistory(): Flow<List<ScanResult>>

    /** The most recent completed scan's report, or Success(null) if no
     *  scan has ever completed — the Home screen's "first-run empty
     *  state" (Sprint 002.5 §15) depends on being able to tell "no scans
     *  yet" apart from "a scan failed to load", which null vs. Failure
     *  lets it do. */
    suspend fun getLatestScanResult(): AppResult<ScanResult?>

    /** Removes one session and its associated report/threats from
     *  history. */
    suspend fun deleteScanHistory(sessionId: String): AppResult<Unit>

    /** Removes all scan history. */
    suspend fun clearScanHistory(): AppResult<Unit>
}
