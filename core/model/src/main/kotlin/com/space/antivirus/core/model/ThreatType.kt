package com.space.antivirus.core.model

/**
 * Category of a detected threat. Deliberately a small, generic,
 * evidence-describable set — not a marketing taxonomy. Every future
 * detector (Sprint 004B+) must be able to justify which category a
 * finding belongs to; this enum doesn't grow just because a new
 * marketing term exists.
 *
 * SUSPICIOUS_APP_CONFIGURATION was added in Sprint 027 for findings that
 * aren't about WHAT PERMISSIONS an app requests (SUSPICIOUS_PERMISSION_USAGE)
 * or WHO it claims to be (POTENTIALLY_UNWANTED_APPLICATION), but about
 * HOW the app itself is built or installed — debuggable in a release
 * build, installed from an unrecognized source, holding device
 * administrator privileges on its own. These are a genuinely distinct
 * evidence category, not a stretch of an existing one. Confirmed before
 * adding: exactly one exhaustive `when (ThreatType)` exists in this
 * codebase (ProductionThreatDescriptionProvider) — the compiler forces
 * that file to add real copy for this case, not just compile around it.
 */
enum class ThreatType {
    MALWARE,
    POTENTIALLY_UNWANTED_APPLICATION,
    SUSPICIOUS_PERMISSION_USAGE,
    SUSPICIOUS_APP_CONFIGURATION,
    UNKNOWN,
}
