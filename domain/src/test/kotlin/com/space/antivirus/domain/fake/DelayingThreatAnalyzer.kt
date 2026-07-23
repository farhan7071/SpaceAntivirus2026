package com.space.antivirus.domain.fake

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.AnalyzerCapability
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.domain.analyzer.ThreatAnalyzer
import kotlinx.coroutines.delay

/**
 * Suspends for a controllable duration before returning — exists so
 * tests can create a deterministic pause point mid-scan (via
 * TestCoroutineScheduler's virtual time) to exercise cancellation
 * (Sprint 006 / ADR 0019). Without a real suspension point somewhere,
 * a scan with no actual delays runs to completion within a single
 * runCurrent() call, leaving no window to cancel it mid-flight.
 */
class DelayingThreatAnalyzer(
    override val id: AnalyzerId,
    override val capabilities: Set<AnalyzerCapability>,
    private val delayMillis: Long,
    private val result: AppResult<AnalysisOutcome>,
) : ThreatAnalyzer {
    override suspend fun analyze(target: ScanTarget): AppResult<AnalysisOutcome> {
        delay(delayMillis)
        return result
    }
}
