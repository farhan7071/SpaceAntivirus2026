package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.ScanProgress
import com.space.antivirus.core.model.ScanRequest
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.core.model.ScanStatistics
import com.space.antivirus.core.model.Threat
import com.space.antivirus.domain.UseCase
import com.space.antivirus.domain.repository.SecurityRepository
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * The top-level orchestration UseCase — the concrete proof that Sprints
 * 004A (persistence), 004B (enumeration), and 004C (analysis), each built
 * and tested independently, actually compose into one working pipeline.
 *
 * Flow: check no scan is already running (Sprint 007, ADR 0020) -> resolve
 * the request's ScanScopes into ScanTargets (004B) -> start a ScanSession
 * (004A) -> analyze each target (004C), publishing ScanProgress after
 * every one -> build a Threat for every Flagged outcome, count every
 * Inconclusive one honestly (see ADR 0017 for why that counting exists)
 * -> persist the completed ScanResult (004A).
 *
 * Fail-fast on the CORE scan pipeline: the first AppResult.Failure from
 * checking for an active session, starting the session, resolving
 * targets, or analyzing a target aborts the whole run. Progress
 * publishing is deliberately NOT fail-fast — see
 * publishProgressBestEffort's KDoc and ADR 0018.
 *
 * CONCURRENT SCAN GUARDING (Sprint 007, ADR 0020): before anything else,
 * checks SecurityRepository.getActiveScanSession() — if a scan is already
 * PENDING or RUNNING, this call fails immediately with
 * AppError.ScanAlreadyInProgress rather than starting a second, competing
 * ScanSession. This check happens before startScanSession is ever called,
 * so there's nothing to clean up if it fails.
 *
 * CANCELLATION (Sprint 006, ADR 0019): a caller cancels a running scan
 * through ordinary structured concurrency — cancelling the coroutine/Job
 * this UseCase is running in — not through a separate stop() method.
 * `coroutineContext.ensureActive()` is checked between targets so a
 * cancelled scan actually stops promptly instead of running to
 * completion regardless. On cancellation, the session is transitioned to
 * CANCELLED (via a NonCancellable cleanup write — see the catch block's
 * comment for why that wrapper is required) before the
 * CancellationException is rethrown, so the persisted session never gets
 * stuck in RUNNING forever.
 */
class RunScanRequestUseCase @Inject constructor(
    private val resolveScanTargets: ResolveScanTargetsUseCase,
    private val analyzeScanTarget: AnalyzeScanTargetUseCase,
    private val buildThreat: BuildThreatUseCase,
    private val startScanSession: StartScanSessionUseCase,
    private val completeScanSession: CompleteScanSessionUseCase,
    private val securityRepository: SecurityRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<ScanRequest, ScanResult>(dispatcher) {

    override suspend fun execute(params: ScanRequest): AppResult<ScanResult> {
        when (val activeResult = securityRepository.getActiveScanSession()) {
            is AppResult.Success -> {
                val activeSession = activeResult.data
                if (activeSession != null) {
                    return AppResult.Failure(AppError.ScanAlreadyInProgress(activeSession.id))
                }
            }
            is AppResult.Failure -> return activeResult
            AppResult.Loading -> return AppResult.Loading
        }

        val startedAtMillis = System.currentTimeMillis()

        val session = when (val sessionResult = startScanSession(params.scanType)) {
            is AppResult.Success -> sessionResult.data
            is AppResult.Failure -> return sessionResult
            AppResult.Loading -> return AppResult.Loading
        }

        try {
            publishProgressBestEffort(ScanProgress.starting(session.id))

            val targets = when (val targetsResult = resolveScanTargets(params)) {
                is AppResult.Success -> targetsResult.data
                is AppResult.Failure -> return targetsResult
                AppResult.Loading -> return AppResult.Loading
            }
            publishProgressBestEffort(
                ScanProgress(session.id, itemsProcessed = 0, totalItems = targets.size, threatsFoundSoFar = 0),
            )

            val threats = mutableListOf<Threat>()
            var inconclusiveCount = 0

            targets.forEachIndexed { index, target ->
                coroutineContext.ensureActive()

                when (val outcomeResult = analyzeScanTarget(target)) {
                    is AppResult.Success -> {
                        when (val outcome = outcomeResult.data) {
                            is AnalysisOutcome.Flagged -> threats += buildThreat(outcome)
                            is AnalysisOutcome.Inconclusive -> inconclusiveCount++
                            is AnalysisOutcome.Clean -> Unit
                        }
                    }
                    is AppResult.Failure -> return outcomeResult
                    AppResult.Loading -> return AppResult.Loading
                }
                publishProgressBestEffort(
                    ScanProgress(
                        sessionId = session.id,
                        itemsProcessed = index + 1,
                        totalItems = targets.size,
                        threatsFoundSoFar = threats.size,
                    ),
                )
            }

            val statistics = ScanStatistics(
                itemsScanned = targets.size,
                threatsFound = threats.size,
                itemsInconclusive = inconclusiveCount,
                durationMillis = System.currentTimeMillis() - startedAtMillis,
            )

            return completeScanSession(
                CompleteScanSessionParams(
                    sessionId = session.id,
                    statistics = statistics,
                    threats = threats,
                ),
            )
        } catch (cancellation: CancellationException) {
            // We're already inside a cancelling coroutine at this point —
            // a plain suspend call here would itself be cancelled
            // immediately without running, a well-known structured-
            // concurrency pitfall. NonCancellable makes this one cleanup
            // write run to completion regardless, so the session doesn't
            // end up orphaned in RUNNING state forever.
            withContext(NonCancellable) {
                securityRepository.cancelScanSession(session.id)
            }
            throw cancellation
        }
    }

    /**
     * Progress reporting is best-effort observability, not part of the
     * scan's correctness guarantee — a dropped progress update means a
     * future UI briefly shows a stale percentage, nothing more. Aborting
     * an entire scan (discarding real analysis work already done)
     * because a progress-snapshot write failed would make the scan's
     * reliability depend on a feature whose whole purpose is secondary
     * to the scan itself. See ADR 0018 for the full reasoning; this is
     * deliberately the ONE place in this UseCase that doesn't propagate
     * AppResult.Failure.
     */
    private suspend fun publishProgressBestEffort(progress: ScanProgress) {
        securityRepository.updateScanProgress(progress)
    }
}
