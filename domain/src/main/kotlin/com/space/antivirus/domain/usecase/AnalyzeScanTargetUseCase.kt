package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.domain.UseCase
import com.space.antivirus.domain.analyzer.AnalysisOutcomeAggregator
import com.space.antivirus.domain.analyzer.ThreatAnalyzerRegistry
import com.space.antivirus.domain.analyzer.identifier
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

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
 * reserved for an analyzer that's supposed to run but can't. It's
 * reported as AnalysisOutcome.Inconclusive with an honest reason, so a
 * future caller can't mistake "nothing is looking at this" for "this was
 * checked and found clean".
 */
class AnalyzeScanTargetUseCase @Inject constructor(
    private val registry: ThreatAnalyzerRegistry,
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

        val outcomes = mutableListOf<AnalysisOutcome>()
        for (analyzer in analyzers) {
            when (val result = analyzer.analyze(params)) {
                is AppResult.Success -> outcomes += result.data
                is AppResult.Failure -> return result
                AppResult.Loading -> return AppResult.Loading
            }
        }

        return AppResult.Success(aggregator.aggregate(outcomes))
    }
}
