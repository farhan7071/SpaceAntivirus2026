package com.space.antivirus.domain.fake

import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.domain.analyzer.ThreatAnalyzer
import com.space.antivirus.domain.analyzer.ThreatAnalyzerRegistry
import com.space.antivirus.domain.analyzer.requiredCapability

class FakeThreatAnalyzerRegistry(
    private val analyzers: List<ThreatAnalyzer>,
) : ThreatAnalyzerRegistry {
    override fun allAnalyzers(): List<ThreatAnalyzer> = analyzers

    override fun analyzersFor(target: ScanTarget): List<ThreatAnalyzer> =
        analyzers.filter { target.requiredCapability in it.capabilities }
}
