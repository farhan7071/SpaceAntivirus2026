package com.space.antivirus.core.model

/**
 * Device storage totals for the volume the app's own data lives on —
 * Sprint 039.
 *
 * Read via `StatFs` against an app-private directory, which requires no
 * permission at all. This describes the *volume*, so the numbers match
 * what Android's own Settings > Storage reports for internal storage,
 * not merely what this app can see.
 */
data class StorageStatistics(
    val totalBytes: Long,
    val freeBytes: Long,
) {
    init {
        require(totalBytes >= 0) { "totalBytes cannot be negative" }
        require(freeBytes >= 0) { "freeBytes cannot be negative" }
        require(freeBytes <= totalBytes) {
            "freeBytes ($freeBytes) cannot exceed totalBytes ($totalBytes)"
        }
    }

    val usedBytes: Long
        get() = totalBytes - freeBytes

    /** 0f..1f, or null when totalBytes is 0 (an unreadable volume) —
     *  never a fabricated 0%. */
    val usedFraction: Float?
        get() = if (totalBytes == 0L) null else usedBytes.toFloat() / totalBytes.toFloat()
}
