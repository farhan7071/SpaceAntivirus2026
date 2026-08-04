package com.space.antivirus.core.designsystem.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StarRate
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

    /**
     * Sprint 038 — one icon per `CleanableCategory` (`core:model`), added
     * here rather than in `feature:clean` for the reason ADR 0031 already
     * established and this file's own KDoc depends on: feature modules
     * deliberately do NOT depend on `compose-material-icons-extended`,
     * only `core:designsystem` and `core:ui` do. A feature module that
     * needs a non-baseline icon reaches for a token here; it does not add
     * the icon dependency to itself.
     *
     * Deliberately no color is attached to any of these — a cache file is
     * not a security concern (`CleanableCategory`'s own KDoc), so nothing
     * in the Cleaner's visual language should borrow the severity palette.
     */
    val cacheFile = Icons.Filled.Layers

    /** Temporary file. Same glyph as `evidence`, different semantic role —
     *  same precedent as `warning`/`highRisk` sharing one glyph above. */
    val temporaryFile = Icons.Filled.Description

    /** Log file. */
    val logFile = Icons.Filled.Article

    /** A leftover `.apk` installer sitting in Downloads. */
    val leftoverInstaller = Icons.Filled.Android

    /** Expand a collapsed section. Baseline icon, no extended dependency needed. */
    val expand = Icons.Filled.KeyboardArrowDown

    /** Collapse an expanded section. Baseline icon. */
    val collapse = Icons.Filled.KeyboardArrowUp

    /**
     * Sprint 043A — the Settings hub's vocabulary.
     *
     * Added here rather than in `feature:settings` for the reason this
     * file's own KDoc and ADR 0031 already establish: feature modules
     * deliberately do NOT depend on compose-material-icons-extended.
     * `chevronRight` is used by `SettingsRow` in core:ui; the rest name
     * the hub's sections.
     */
    val chevronRight = Icons.Filled.ChevronRight

    /** Scheduled / recurring work. */
    val schedule = Icons.Filled.Schedule

    /** Notifications. */
    val notifications = Icons.Filled.Notifications

    /** Battery. */
    val battery = Icons.Filled.BatteryFull

    /** Privacy and policy documents. */
    val privacy = Icons.Filled.PrivacyTip

    /** Help / support. */
    val support = Icons.Filled.HelpOutline

    /** Rating the app. */
    val rate = Icons.Filled.StarRate

    /** Sharing. */
    val share = Icons.Filled.Share

    /** Feedback by email. */
    val feedback = Icons.Filled.Email

    /** Terms / legal document. */
    val document = Icons.Filled.Description
}
