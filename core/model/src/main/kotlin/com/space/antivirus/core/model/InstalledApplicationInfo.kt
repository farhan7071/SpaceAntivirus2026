package com.space.antivirus.core.model

/**
 * What we know about one installed application after enumeration —
 * identity and install metadata only. No certificate inspection, no
 * risk classification; those still belong to a later sprint's analyzers,
 * operating ON this shape, not inside it.
 *
 * `requestedPermissions` was added in Sprint 014 — a real gap found
 * while implementing this project's first ThreatAnalyzer: this model's
 * own KDoc had said permission analysis belonged to "a later sprint's
 * analyzers, operating on this shape," but as of Sprint 013 the shape
 * itself carried no permission data at all, and neither did the
 * enumerator that produces it. This is a necessary, minimal extension
 * within Phase A's own stated scope (detection capability), not a
 * roadmap-level architectural change — flagged directly rather than
 * worked around. See ADR 0027.
 */
data class InstalledApplicationInfo(
    val packageName: String,
    val appLabel: String,
    val versionName: String?,
    val versionCode: Long,
    val installedAtEpochMillis: Long,
    val isSystemApp: Boolean,
    val apkPath: String,
    val requestedPermissions: List<String>,
) {
    init {
        require(packageName.isNotBlank()) { "packageName cannot be blank" }
        require(versionCode >= 0) { "versionCode cannot be negative" }
    }
}
