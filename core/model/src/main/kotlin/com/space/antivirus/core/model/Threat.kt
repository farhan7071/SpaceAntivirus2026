package com.space.antivirus.core.model

/**
 * A user-facing threat finding — the aggregate of one or more Detections
 * against a single scan target. `title`/`description` must follow the
 * Sprint 002.75 Vocabulary Dictionary (§4) and Security Messaging Guide
 * (§7): plain, specific, proportionate to riskLevel, never inflated.
 *
 * `targetIdentifier` is intentionally a plain String (a file path or
 * package name) rather than a richer ScanTarget model — introducing file
 * enumeration/target-modeling concerns is explicitly out of scope for
 * Sprint 004A ("Do not implement: file enumeration").
 */
data class Threat(
    val id: String,
    val targetIdentifier: String,
    val threatType: ThreatType,
    val riskLevel: RiskLevel,
    val title: String,
    val description: String,
    val detections: List<Detection>,
    val discoveredAtEpochMillis: Long,
) {
    init {
        require(detections.isNotEmpty()) {
            "A Threat must be backed by at least one Detection — a threat " +
                "with no evidence is exactly the unsupported claim Sprint " +
                "002.75 §17 (\"show evidence\") prohibits."
        }
    }
}
