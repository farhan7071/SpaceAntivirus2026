package com.space.antivirus.core.model

/**
 * Category of a detected threat. Deliberately a small, generic,
 * evidence-describable set — not a marketing taxonomy. Every future
 * detector (Sprint 004B+) must be able to justify which category a
 * finding belongs to; this enum doesn't grow just because a new
 * marketing term exists.
 */
enum class ThreatType {
    MALWARE,
    POTENTIALLY_UNWANTED_APPLICATION,
    SUSPICIOUS_PERMISSION_USAGE,
    UNKNOWN,
}
