package com.space.antivirus.core.model

/**
 * A single piece of raw evidence produced by one analysis step (e.g. one
 * matched signature, one flagged permission). Multiple Detections can be
 * aggregated into a single user-facing Threat by whatever produces one
 * (that aggregation logic belongs to Sprint 004B's analyzers, not here —
 * this is only the shape).
 *
 * `evidenceDescription` must be a plain, specific, verifiable statement
 * (Sprint 002.75 §17: "always show evidence") — never a vague label like
 * "dangerous behavior detected".
 */
data class Detection(
    val id: String,
    val threatType: ThreatType,
    val evidenceDescription: String,
    val riskLevel: RiskLevel,
)
