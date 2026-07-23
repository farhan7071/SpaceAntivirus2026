package com.space.antivirus.core.model

/**
 * What we know about one installed application after enumeration —
 * identity and install metadata only. No permission analysis, no
 * certificate inspection, no risk classification; those belong to a
 * later sprint's analyzers, operating ON this shape, not inside it.
 */
data class InstalledApplicationInfo(
    val packageName: String,
    val appLabel: String,
    val versionName: String?,
    val versionCode: Long,
    val installedAtEpochMillis: Long,
    val isSystemApp: Boolean,
    val apkPath: String,
) {
    init {
        require(packageName.isNotBlank()) { "packageName cannot be blank" }
        require(versionCode >= 0) { "versionCode cannot be negative" }
    }
}
