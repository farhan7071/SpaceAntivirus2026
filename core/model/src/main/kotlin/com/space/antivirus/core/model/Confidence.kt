package com.space.antivirus.core.model

/**
 * How sure a SPECIFIC analyzer is about a SPECIFIC finding — distinct
 * from RiskLevel, which represents HOW SEVERE the finding would be IF
 * true. A LOW-confidence, ACTION_NEEDED-severity finding and a
 * HIGH-confidence, ACTION_NEEDED-severity finding describe the same
 * potential harm with different certainty; conflating the two into one
 * scale would lose real information CumulativeRiskScorer (Sprint 027)
 * needs to reason about co-occurring signals.
 *
 * Deliberately a 3-tier closed set, same discipline as RiskLevel itself
 * (Sprint 002.75 §4 — no numeric/inflated scores). Every analyzer must
 * be able to justify its confidence choice the same way it justifies
 * riskLevel — this isn't a knob to tune for effect.
 */
enum class Confidence {
    LOW,
    MODERATE,
    HIGH,
}
