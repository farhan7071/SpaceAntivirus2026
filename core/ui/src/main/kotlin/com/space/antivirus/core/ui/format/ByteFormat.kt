package com.space.antivirus.core.ui.format

/**
 * Human-readable file sizes — Sprint 040.
 *
 * Extracted from `CleanScreen.kt`'s private `formatSize` (Sprint 022,
 * carried through 038/039) now that Home needs the identical formatting
 * for its "Last cleanup" line. Two feature modules formatting bytes
 * their own way would drift, and "482 MB" appearing on one screen as
 * "482.0 MB" on another is exactly the kind of small inconsistency a
 * shared design system exists to prevent.
 *
 * Built now rather than earlier for the same reason `AppSectionHeader`
 * was (Sprint 038): the bar this project sets for a `core:ui` extraction
 * is 2+ callers that genuinely need it today, and Sprint 037 round 2
 * deleted an earlier extraction (`AppStatGroup`) for failing it. Until
 * Home needed this, the Cleaner was the only caller and it correctly
 * stayed local.
 *
 * Decimal units (1 MB = 1,000,000 bytes), matching what Android's own
 * Settings > Storage reports, so a "482 MB freed" claim here lines up
 * with what the user sees the system say. Not a rounding choice made for
 * bigger-looking numbers — it is the platform's convention, and matching
 * it is what makes the figure checkable.
 */
fun formatBytes(sizeBytes: Long): String = when {
    sizeBytes >= 1_000_000_000 -> "%.1f GB".format(sizeBytes / 1_000_000_000.0)
    sizeBytes >= 1_000_000 -> "%.1f MB".format(sizeBytes / 1_000_000.0)
    sizeBytes >= 1_000 -> "%.1f KB".format(sizeBytes / 1_000.0)
    else -> "$sizeBytes B"
}
