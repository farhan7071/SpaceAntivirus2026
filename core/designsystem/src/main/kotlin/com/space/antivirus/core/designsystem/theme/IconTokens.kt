package com.space.antivirus.core.designsystem.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning

/**
 * Space Design System v1.0, Part 7 — Iconography.
 *
 * One icon family only: Icons.Filled.* everywhere, per this sprint's
 * own "never mix outlined/rounded/filled" instruction — matches every
 * icon choice already made throughout this project since Sprint 030
 * (StatusChip, EvidenceIcon, ScanSummaryCard, ScanResultBadge all
 * already use Filled exclusively; this sprint doesn't introduce a new
 * convention, it names the one already in consistent use).
 *
 * A semantic mapping — component code should reference IconTokens.security,
 * not Icons.Filled.Shield directly, so a future icon change (a redesign,
 * or discovering one of these names doesn't compile) happens in exactly
 * one place rather than at every call site that happened to know which
 * literal icon "security" meant.
 *
 * Confidence varies by icon, same discipline EvidenceIcon.kt (core:ui)
 * already established for its own choices — none of these have been
 * verified against a real compiler in this sandbox. `warning` and
 * `settings` are near-certain (Warning is this project's one icon
 * verified safe since Sprint 017; Settings is among the most
 * long-standing, basic Material icons in existence). The rest are
 * chosen for being well-established, commonly-used extended icon names,
 * but carry real, if small, risk. If any is wrong, it is an isolated,
 * one-line fix in this file only — every consumer references the named
 * token, not the underlying icon directly.
 */
object IconTokens {
    /** Security / protection status. Used by ScanSummaryCard already. */
    val security = Icons.Filled.Shield

    /** A scan in progress or the scan action itself. */
    val scan = Icons.Filled.Search

    /** The Cleaner feature and its actions. */
    val cleaner = Icons.Filled.CleaningServices

    /** Trusted / safe status. Used by ScanResultBadge/ScanSummaryCard already. */
    val trusted = Icons.Filled.CheckCircle

    /** General warning / attention. The one icon confirmed safe since Sprint 017. */
    val warning = Icons.Filled.Warning

    /**
     * High risk. Deliberately the same icon as `warning`, not a
     * distinct glyph — matches StatusChip's own existing decision
     * (Sprint 034) that ATTENTION and ACTION_NEEDED share one icon,
     * with color and text label (not the icon) carrying the distinction
     * between them.
     */
    val highRisk = Icons.Filled.Warning

    /** A specific permission (camera, SMS, overlay...) being referenced generically. */
    val permission = Icons.Filled.Lock

    /** Scan History. */
    val history = Icons.Filled.History

    /** Settings. */
    val settings = Icons.Filled.Settings

    /** A piece of evidence within a threat report. */
    val evidence = Icons.Filled.Description

    /** A recommendation. Used by ThreatSummaryCard's recommendation section already. */
    val recommendation = Icons.Filled.Info
}
