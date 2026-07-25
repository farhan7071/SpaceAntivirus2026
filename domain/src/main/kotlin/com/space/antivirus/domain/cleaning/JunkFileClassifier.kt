package com.space.antivirus.domain.cleaning

import com.space.antivirus.core.model.CleanableCategory
import com.space.antivirus.core.model.CleanableItem
import com.space.antivirus.core.model.FileMetadata
import javax.inject.Inject

/**
 * The production junk-file detection policy — this project's first, and
 * deliberately conservative by construction, same discipline as the
 * SuspiciousPermissionPatternAnalyzer/AppIdentityImpersonationAnalyzer
 * heuristics (Sprints 014/015): every rule here is a well-established,
 * low-ambiguity signal, not an invented pattern, and `nowEpochMillis` is
 * an explicit parameter (never read internally via
 * System.currentTimeMillis()) so classification stays fully
 * deterministic and testable — same input, including "now", always
 * produces the same output.
 *
 * Four rules, in order of precedence, each independently testable:
 *
 * 1. CACHE_FILE — path contains a "/cache/" segment. Files inside a
 *    cache directory are, by Android platform convention, always safe to
 *    clear; this is the single least-ambiguous signal available.
 * 2. TEMPORARY_FILE — extension is one of a small, well-known temp-file
 *    set (tmp, temp, bak, old). Not a location-based signal, so it
 *    applies regardless of where the file lives.
 * 3. LOG_FILE — extension is "log".
 * 4. LEFTOVER_INSTALLER — a .apk file inside a Downloads-like path,
 *    AND unmodified for at least LEFTOVER_INSTALLER_AGE_THRESHOLD_MILLIS.
 *    The age requirement exists specifically to avoid flagging an APK
 *    the user just downloaded and may be about to install — a location
 *    + extension match alone isn't a strong enough signal on its own,
 *    unlike the other three rules.
 *
 * Directories are never classified — this policy only ever identifies
 * individual files, never asks a caller to consider removing a whole
 * directory tree.
 */
class JunkFileClassifier @Inject constructor() {

    fun classify(file: FileMetadata, nowEpochMillis: Long): CleanableItem? {
        if (file.isDirectory) return null

        val pathLower = file.path.lowercase()
        val extension = file.name.substringAfterLast('.', missingDelimiterValue = "").lowercase()

        return when {
            pathLower.contains("/cache/") -> CleanableItem(
                path = file.path,
                name = file.name,
                sizeBytes = file.sizeBytes,
                category = CleanableCategory.CACHE_FILE,
                reason = "Located in a cache directory — cache contents are safe to clear by Android convention.",
            )

            extension in TEMPORARY_EXTENSIONS -> CleanableItem(
                path = file.path,
                name = file.name,
                sizeBytes = file.sizeBytes,
                category = CleanableCategory.TEMPORARY_FILE,
                reason = "File extension \".$extension\" is commonly used for temporary files.",
            )

            extension == LOG_EXTENSION -> CleanableItem(
                path = file.path,
                name = file.name,
                sizeBytes = file.sizeBytes,
                category = CleanableCategory.LOG_FILE,
                reason = "Log file (.log extension).",
            )

            extension == APK_EXTENSION &&
                pathLower.contains(DOWNLOAD_PATH_SEGMENT) &&
                isOlderThan(file, nowEpochMillis, LEFTOVER_INSTALLER_AGE_THRESHOLD_MILLIS) -> CleanableItem(
                path = file.path,
                name = file.name,
                sizeBytes = file.sizeBytes,
                category = CleanableCategory.LEFTOVER_INSTALLER,
                reason = "Downloaded app installer (.apk) in Downloads, unmodified for over 24 hours — " +
                    "likely already installed and no longer needed.",
            )

            else -> null
        }
    }

    private fun isOlderThan(file: FileMetadata, nowEpochMillis: Long, thresholdMillis: Long): Boolean =
        nowEpochMillis - file.lastModifiedEpochMillis >= thresholdMillis

    private companion object {
        val TEMPORARY_EXTENSIONS = setOf("tmp", "temp", "bak", "old")
        const val LOG_EXTENSION = "log"
        const val APK_EXTENSION = "apk"
        const val DOWNLOAD_PATH_SEGMENT = "/download"
        const val LEFTOVER_INSTALLER_AGE_THRESHOLD_MILLIS = 24L * 60 * 60 * 1000 // 24 hours
    }
}
