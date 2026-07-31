package com.space.antivirus.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Space Design System v1.0, Part 6 — Elevation.
 *
 * A genuinely new file — before this sprint, elevation values were
 * hardcoded per call site (e.g. `CardDefaults.cardElevation(defaultElevation
 * = 2.dp)` written directly in ThreatSummaryCard, ScanSummaryCard, each
 * with its own literal 2.dp), the exact "inconsistent shadows" this
 * sprint's own Part 6 goal asks to avoid — not because any of those
 * values were wrong, but because there was nowhere for a future
 * component to look up what value to reach for at a given "how raised
 * does this feel" tier, at all.
 *
 * Five tiers, matching this sprint's own list exactly: Flat (no
 * elevation — inline content, list items directly on the background),
 * Card (this project's standard resting-card elevation — matches the
 * literal 2.dp value ThreatSummaryCard/ScanSummaryCard already used, so
 * adopting this constant changes no existing visual), Floating (a
 * raised, attention-drawing element — an FAB or a floating action row,
 * not currently used by any existing component but named for when one
 * needs it), Dialog (M3's own standard dialog elevation), Overlay
 * (the highest tier — a dropdown menu, bottom sheet, or anything meant
 * to visually sit above everything else on screen).
 */
object Elevation {
    val flat: Dp = 0.dp
    val card: Dp = 2.dp
    val floating: Dp = 4.dp
    val dialog: Dp = 6.dp
    val overlay: Dp = 8.dp
}
