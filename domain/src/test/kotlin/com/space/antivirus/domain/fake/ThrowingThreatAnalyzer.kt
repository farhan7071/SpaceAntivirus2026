package com.space.antivirus.domain.fake

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.AnalyzerCapability
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.domain.analyzer.ThreatAnalyzer

/**
 * Unlike FakeThreatAnalyzer (which returns a scripted AppResult), this
 * one throws a plain exception directly from analyze() — simulating a
 * genuinely broken third-party analyzer that doesn't honor the AppResult
 * contract. Exists specifically to test AnalyzerExecutor's fault
 * isolation (Sprint 006): a real bug in one analyzer implementation, not
 * just a well-behaved Failure result.
 */
class ThrowingThreatAnalyzer(
    override val id: AnalyzerId,
    override val capabilities: Set<AnalyzerCapability>,
) : ThreatAnalyzer {
    override suspend fun analyze(target: ScanTarget): AppResult<AnalysisOutcome> {
        throw IllegalStateException("Simulated analyzer crash — this analyzer never returns normally")
    }
}
