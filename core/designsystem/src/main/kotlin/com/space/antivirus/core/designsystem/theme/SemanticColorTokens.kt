package com.space.antivirus.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Space Design System v1.0 — Semantic Color Tokens.
 *
 * Colors named by what they *mean*, not what they look like — the
 * counterpart to BrandColorTokens.kt's raw palette. Split into its own
 * file for the same reason: "brand tokens" and "semantic color tokens"
 * are two conceptually different layers (one is "what Space Antivirus
 * looks like," the other is "what does orange mean in this app"), and
 * keeping them in one file made that distinction easy to lose.
 *
 * Semantic severity colors — mapped onto (not replacing) M3 roles, per
 * Sprint 002.75 §4: severity is communicated by icon + text label first,
 * color second. Only three tiers exist by design (Sprint 002.5 §17).
 *
 * Sprint 034: Safe added alongside the three severity colors, but
 * deliberately NOT a fourth severity tier — Severity (core:ui) still
 * has exactly three values. Safe represents the absence of any finding
 * (a clean scan session, zero threats), a genuinely different concept
 * from "how severe is this specific finding," which is what Severity's
 * three tiers actually measure. Used by ScanResultBadge (core:ui) for
 * History's per-session result badge and ScanSummaryCard's protected
 * status — never by StatusChip, which stays scoped to Severity's three
 * values exactly as before.
 *
 * Sprint 035: Suspicious added as a defined color token (matching the
 * reference design's own five-badge vocabulary), but — same discipline
 * as Safe not being a fourth Severity tier — deliberately NOT wired
 * into Severity or any analyzer. No analyzer or scorer in this project
 * computes a "suspicious, but not quite high-risk" signal distinct from
 * ACTION_NEEDED (ADR 0048's reasoning, unchanged here). Defining the
 * token without wiring it to fake data keeps the design system's own
 * documented vocabulary complete without inventing a distinction the
 * detection engine doesn't actually make. Available for a future
 * screen that has a genuine, non-severity use for an orange status
 * color (this sprint does not decide what that is — SDS v1.0 defines
 * tokens, it does not redesign screens).
 */
object SeverityColors {
    val InfoLight = Color(0xFF3D6373)
    val AttentionLight = Color(0xFF7B5800)
    val ActionNeededLight = Color(0xFFBA1A1A)
    val SafeLight = Color(0xFF2E7D32)
    val SuspiciousLight = Color(0xFFB84F00)

    val InfoDark = Color(0xFFA6CBE0)
    val AttentionDark = Color(0xFFF6BD3F)
    val ActionNeededDark = Color(0xFFFFB4AB)
    val SafeDark = Color(0xFF8BD08F)
    val SuspiciousDark = Color(0xFFFFB68C)
}

/**
 * Sprint 035 (Part 2) — the design system's own general-purpose
 * semantic color vocabulary, distinct from SeverityColors (which is
 * scoped specifically to detection severity/scan-result badges).
 * SpaceColors names the same underlying tokens by their general-purpose
 * semantic role, for use anywhere in the app that needs a "success" or
 * "informational" accent that isn't specifically a threat-severity
 * badge — e.g. a settings toggle confirmation, a non-threat-related
 * status message. Deliberately reuses SeverityColors' exact values
 * rather than defining new ones — one set of real color decisions,
 * named twice for two different call-site vocabularies, not two
 * independent palettes that could drift apart.
 */
object SpaceColors {
    val securityGreenLight = SeverityColors.SafeLight
    val securityGreenDark = SeverityColors.SafeDark
    val successLight = SeverityColors.SafeLight
    val successDark = SeverityColors.SafeDark
    val informationLight = SeverityColors.InfoLight
    val informationDark = SeverityColors.InfoDark
    val attentionLight = SeverityColors.AttentionLight
    val attentionDark = SeverityColors.AttentionDark
    val suspiciousLight = SeverityColors.SuspiciousLight
    val suspiciousDark = SeverityColors.SuspiciousDark
    val highRiskLight = SeverityColors.ActionNeededLight
    val highRiskDark = SeverityColors.ActionNeededDark
}
