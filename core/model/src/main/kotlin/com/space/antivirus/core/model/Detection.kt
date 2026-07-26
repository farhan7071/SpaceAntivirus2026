package com.space.antivirus.core.model

/**
 * A single piece of raw evidence produced by one analysis step (e.g. one
 * matched signature, one flagged permission). Multiple Detections can be
 * aggregated into a single user-facing Threat by whatever produces one
 * (that aggregation logic belongs to a later sprint's UseCases, not
 * here — this is only the shape).
 *
 * `evidenceDescription` must be a plain, specific, verifiable statement
 * (Sprint 002.75 §17: "always show evidence") — never a vague label like
 * "dangerous behavior detected".
 *
 * `analyzerId` was added in Sprint 004C — see ADR 0015 for why this is a
 * deliberate breaking change to an already-shipped model rather than a
 * defaulted/optional field: a Detection with no known source is a data
 * integrity gap this project shouldn't silently allow, especially once
 * multiple analyzer types (signature, heuristic, AI, cloud) can all
 * produce Detections against the same target.
 *
 * `confidence` was added in Sprint 027 — RiskScorer's own KDoc (Sprint
 * 004C) had already anticipated this exact addition ("a scoring strategy
 * that weighs analyzer confidence, once that concept exists"). Unlike
 * `analyzerId`, this DOES get a default (MODERATE) rather than being a
 * breaking change — this sprint's own constraints explicitly rule out
 * introducing breaking architectural changes, and dozens of existing
 * Detection construction sites across this codebase would otherwise all
 * need updating for a field most of them have no reason to specify
 * explicitly. See ADR 0041.
 */
data class Detection(
    val id: String,
    val analyzerId: AnalyzerId,
    val threatType: ThreatType,
    val evidenceDescription: String,
    val riskLevel: RiskLevel,
    val confidence: Confidence = Confidence.MODERATE,
)
