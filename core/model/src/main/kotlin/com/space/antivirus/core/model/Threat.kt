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
 *
 * `appLabel` was added in Sprint 029 — a real, verified root-cause fix,
 * not a cosmetic addition. Before this field existed, nothing on Threat
 * carried the application's actual display name, and the UI showed
 * threat.title (a generic, threatType-derived category label like
 * "Unusual permission combination") as its headline instead. Different
 * apps triggering the same threatType showed identical headline text —
 * indistinguishable from literal duplication in a list. Defaulted to an
 * empty string, not a breaking change, matching this project's standing
 * pattern for additive model changes since Sprint 027; BuildThreatUseCase
 * always populates it for real threats. See ADR 0043.
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
    val appLabel: String = "",
) {
    init {
        require(detections.isNotEmpty()) {
            "A Threat must be backed by at least one Detection — a threat " +
                "with no evidence is exactly the unsupported claim Sprint " +
                "002.75 §17 (\"show evidence\") prohibits."
        }
    }
}
