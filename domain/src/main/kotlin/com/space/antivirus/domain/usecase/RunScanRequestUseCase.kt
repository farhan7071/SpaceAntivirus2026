package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.ScanRequest
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.core.model.ScanStatistics
import com.space.antivirus.core.model.Threat
import com.space.antivirus.domain.UseCase
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

/**
 * The top-level orchestration UseCase — the concrete proof that Sprints
 * 004A (persistence), 004B (enumeration), and 004C (analysis), each built
 * and tested independently, actually compose into one working pipeline.
 *
 * Flow: resolve the request's ScanScopes into ScanTargets (004B) -> start
 * a ScanSession (004A) -> analyze each target (004C) -> build a Threat
 * for every Flagged outcome, count every Inconclusive one honestly (see
 * ADR 0017 for why that counting exists) -> persist the completed
 * ScanResult (004A).
 *
 * Fail-fast: the first AppResult.Failure from any step aborts the whole
 * run. A more resilient per-target error policy is real future work, not
 * invented here without a concrete case driving that design (ADR 0017).
 */
class RunScanRequestUseCase @Inject constructor(
    private val resolveScanTargets: ResolveScanTargetsUseCase,
    private val analyzeScanTarget: AnalyzeScanTargetUseCase,
    private val buildThreat: BuildThreatUseCase,
    private val startScanSession: StartScanSessionUseCase,
    private val completeScanSession: CompleteScanSessionUseCase,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<ScanRequest, ScanResult>(dispatcher) {

    override suspend fun execute(params: ScanRequest): AppResult<ScanResult> {
        val startedAtMillis = System.currentTimeMillis()

        val session = when (val sessionResult = startScanSession(params.scanType)) {
            is AppResult.Success -> sessionResult.data
            is AppResult.Failure -> return sessionResult
            AppResult.Loading -> return AppResult.Loading
        }

        val targets = when (val targetsResult = resolveScanTargets(params)) {
            is AppResult.Success -> targetsResult.data
            is AppResult.Failure -> return targetsResult
            AppResult.Loading -> return AppResult.Loading
        }

        val threats = mutableListOf<Threat>()
        var inconclusiveCount = 0

        for (target in targets) {
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
}
