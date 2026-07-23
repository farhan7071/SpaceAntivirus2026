package com.space.antivirus.core.model

/**
 * A single concrete, resolved item ready to be handed to a future
 * scanner — the output of enumeration. Deliberately a closed sealed
 * hierarchy over just the two kinds of thing this app enumerates (files
 * and applications), so any future consumer's `when` over ScanTarget is
 * compiler-checked for completeness, matching this project's established
 * AppResult/AppError pattern (ADR 0007).
 */
sealed interface ScanTarget {
    data class FileTarget(val metadata: FileMetadata) : ScanTarget
    data class ApplicationTarget(val application: InstalledApplicationInfo) : ScanTarget
}
