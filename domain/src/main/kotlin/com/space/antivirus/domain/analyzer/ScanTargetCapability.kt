package com.space.antivirus.domain.analyzer

import com.space.antivirus.core.model.AnalyzerCapability
import com.space.antivirus.core.model.ScanTarget

/**
 * The one place the ScanTarget -> AnalyzerCapability mapping is defined —
 * small and pure, but worth centralizing so every future
 * ThreatAnalyzerRegistry implementation (and any test doubles for one)
 * routes targets identically rather than each reimplementing this `when`.
 */
val ScanTarget.requiredCapability: AnalyzerCapability
    get() = when (this) {
        is ScanTarget.FileTarget -> AnalyzerCapability.FILE_ANALYSIS
        is ScanTarget.ApplicationTarget -> AnalyzerCapability.APPLICATION_ANALYSIS
    }
