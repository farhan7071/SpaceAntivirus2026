package com.space.antivirus.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * Space Design System v1.0 — Layout Tokens.
 *
 * A category distinct from Spacing: Spacing.kt names the *gaps* between
 * elements (the 8dp grid); this file names structural dimensions of the
 * layout itself — how wide a screen's content area gets, how tall a
 * list row is, what the smallest a tappable element is allowed to be.
 * Neither is a subset of the other; a screen's horizontal padding and
 * the gap between two cards in a list both happen to be 16dp today, but
 * they answer different design questions and can diverge independently
 * without this file and Spacing.kt needing to change together.
 *
 * Defined here as reusable constants for a future screen-adoption phase
 * to consume — this sprint does not apply any of these to an existing
 * screen (Phase 1 is the design system itself, not a screen redesign).
 */
object LayoutTokens {
    /**
     * The horizontal margin every screen's content should sit within,
     * on phone-width layouts. Equal to Spacing.medium's value (16.dp)
     * today by coincidence of both being on the same 8dp grid, not by
     * dependency — this is the layout answer to "how far from the
     * screen edge does content start," Spacing.medium is the spacing
     * answer to "how far apart are two unrelated elements."
     */
    val screenHorizontalPadding = 16.dp

    /**
     * The maximum width a screen's main content column should grow to
     * on large screens (tablets, foldables unfolded) — content is
     * centered within this width rather than stretching edge-to-edge
     * indefinitely, which is what "Adaptive Layouts" (this sprint's own
     * Android UI/UX Architect skill listing) means for a single-column
     * screen like this app's: bounding line length and touch-target
     * reach, not introducing a multi-pane layout, which none of this
     * app's existing screens are structured for.
     */
    val contentMaxWidth = 600.dp

    /**
     * The minimum size of any tappable element, in either dimension —
     * WCAG 2.1 AA / Material accessibility guidance (this sprint's own
     * Part 13). Every interactive component (buttons, badges if they
     * become tappable, icon buttons) should size its touch target to at
     * least this, even if the visible icon or text inside it is
     * smaller — pad the touch area, don't shrink the requirement.
     */
    val minTouchTarget = 48.dp

    /** A single-line list row's standard height (M3 list-item guidance). */
    val listItemHeightSingleLine = 56.dp

    /** A two-line list row's standard height (e.g. a title + subtitle row). */
    val listItemHeightTwoLine = 72.dp

    /** A three-line list row's standard height (e.g. title + two-line supporting text). */
    val listItemHeightThreeLine = 88.dp
}
