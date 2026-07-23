package com.space.antivirus.domain.fake

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.AnalyzerCapability
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.domain.analyzer.ThreatAnalyzer

/**
 * Local to :domain's own test source set — same reasoning as
 * FakeSecurityRepository (Sprint 004A): :domain cannot depend on
 * core:testing, which is an Android module.
 */
class FakeThreatAnalyzer(
    override val id: AnalyzerId,
    override val capabilities: Set<AnalyzerCapability>,
    private val result: AppResult<AnalysisOutcome>,
) : ThreatAnalyzer {
    override suspend fun analyze(target: ScanTarget): AppResult<AnalysisOutcome> = result
}
