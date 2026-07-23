package com.space.antivirus.domain.usecase

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
import kotlinx.coroutines.CoroutineDispatcher

/**
 * The top-level orchestration UseCase — the concrete proof that Sprints
 * 004A (persistence), 004B (enumeration), and 004C (analysis), each built
 * and tested independently, actually compose into one working pipeline.
 *
 * Flow: resolve the request's ScanScopes into ScanTargets (004B) -> start
 * a ScanSession (004A) -> analyze each target (004C), publishing
 * ScanProgress after every one -> build a Threat for every Flagged
 * outcome, count every Inconclusive one honestly (see ADR 0017 for why
 * that counting exists) -> persist the completed ScanResult (004A).
 *
 * Fail-fast on the CORE scan pipeline: the first AppResult.Failure from
 * starting the session, resolving targets, or analyzing a target aborts
 * the whole run. Progress publishing is deliberately NOT fail-fast — see
 * publishProgressBestEffort's KDoc and ADR 0018.
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
        val startedAtMillis = System.currentTimeMillis()

        val session = when (val sessionResult = startScanSession(params.scanType)) {
            is AppResult.Success -> sessionResult.data
            is AppResult.Failure -> return sessionResult
            AppResult.Loading -> return AppResult.Loading
        }
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
