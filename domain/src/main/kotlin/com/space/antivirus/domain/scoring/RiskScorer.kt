package com.space.antivirus.domain.scoring

import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel

/**
 * Reduces a set of Detections (possibly from multiple analyzers) down to
 * one overall RiskLevel for the Threat they'll be aggregated into. A
 * contract, not just a function, so a future sprint could introduce a
 * different scoring strategy (e.g. one that weighs analyzer confidence,
 * once that concept exists) without changing every call site — same
 * plug-in reasoning as ThreatAnalyzer itself, applied to scoring instead
 * of detection.
 */
interface RiskScorer {
    fun score(detections: List<Detection>): RiskLevel
}
