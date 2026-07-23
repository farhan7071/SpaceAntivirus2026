package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.domain.UseCase
import com.space.antivirus.domain.analyzer.AnalysisOutcomeAggregator
import com.space.antivirus.domain.analyzer.AnalyzerExecutor
import com.space.antivirus.domain.analyzer.ThreatAnalyzerRegistry
import com.space.antivirus.domain.analyzer.identifier
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Runs every applicable ThreatAnalyzer against one ScanTarget and
 * combines their outcomes into one. This is the concrete demonstration
 * that Sprint 004C's plug-in architecture actually works end-to-end:
 * this class never references a specific analyzer implementation, only
 * the ThreatAnalyzer/ThreatAnalyzerRegistry contracts — adding a new
 * engine later means it starts getting called here automatically, once
 * it's bound into whatever implements ThreatAnalyzerRegistry.
 *
 * A target with zero applicable analyzers registered (e.g. no engine yet
 * bound for APPLICATION_ANALYSIS) is not a failure — AppResult.Failure is
 * reserved for every applicable analyzer failing to run. It's reported
 * as AnalysisOutcome.Inconclusive with an honest reason, so a future
 * caller can't mistake "nothing is looking at this" for "this was
 * checked and found clean".
 *
 * Sprint 006 changed two things, both documented in ADR 0019:
 * - SCHEDULING: applicable analyzers now run concurrently (via
 *   AnalyzerExecutor, one coroutine each) instead of sequentially.
 * - FAULT ISOLATION: one analyzer failing (via AnalyzerExecutor's
 *   exception-catching) no longer aborts the whole target's analysis —
 *   if at least one OTHER analyzer for this target succeeds, its outcome
 *   is used. Only if EVERY applicable analyzer fails does this method
 *   return a Failure. This is a real behavior change from Sprint 004C's
 *   original fail-fast-across-analyzers semantics.
 */
class AnalyzeScanTargetUseCase @Inject constructor(
    private val registry: ThreatAnalyzerRegistry,
    private val executor: AnalyzerExecutor,
    private val aggregator: AnalysisOutcomeAggregator,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<ScanTarget, AnalysisOutcome>(dispatcher) {

    override suspend fun execute(params: ScanTarget): AppResult<AnalysisOutcome> {
        val analyzers = registry.analyzersFor(params)
        if (analyzers.isEmpty()) {
            return AppResult.Success(
                AnalysisOutcome.Inconclusive(
                    targetIdentifier = params.identifier,
                    reason = "No analyzer is currently registered for this target type.",
                ),
            )
        }

        val executions = coroutineScope {
            analyzers.map { analyzer -> async { executor.execute(analyzer, params) } }.awaitAll()
        }

        val successfulOutcomes = executions.mapNotNull { (it.result as? AppResult.Success)?.data }

        if (successfulOutcomes.isEmpty()) {
            // Fault isolation couldn't save this one — every applicable
            // analyzer failed. Surface the first failure rather than
            // silently reporting Inconclusive; a total analyzer failure
            // is a real operational problem, not a normal "nothing found"
            // result, and deserves to be visibly different from both.
            return when (val firstResult = executions.first().result) {
                is AppResult.Failure -> firstResult
                AppResult.Loading -> AppResult.Failure(AppError.EngineUnavailable)
                is AppResult.Success -> error(
                    "Unreachable: successfulOutcomes would not be empty if the first result were Success",
                )
            }
        }

        return AppResult.Success(aggregator.aggregate(successfulOutcomes))
    }
}
