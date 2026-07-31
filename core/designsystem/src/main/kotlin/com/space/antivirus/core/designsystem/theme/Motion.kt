package com.space.antivirus.core.designsystem.theme

/**
 * Space Design System v1.0, Part 12 — Motion.
 *
 * Named animation duration constants, replacing the unnamed default
 * durations AnimatedVisibility already used in ThreatSummaryCard
 * (Sprint 034's expand/collapse animation) with values a future
 * component can reach for by name instead of guessing at a literal
 * millisecond count. "Animations should feel subtle and premium" (this
 * sprint's own Part 12 goal) — these durations are deliberately on the
 * shorter, snappier end of Material's own guidance (150–300ms is
 * M3's typical range for most transitions) rather than slow, decorative
 * motion, which reads as sluggish on a security app users open to get
 * an answer quickly, not to watch it animate.
 *
 * Compose's AnimatedVisibility (and the fadeIn/expandVertically/
 * fadeOut/shrinkVertically specs it composes with) accept a plain Int
 * millisecond value via `tween(durationMillis = ...)` — these are
 * defined as Int for exactly that call shape, not androidx.compose.
 * animation.core.AnimationSpec directly, so this file stays free of an
 * animation-library import for what is otherwise just a set of named
 * numbers.
 */
object MotionDurations {
    /** Expand/collapse — ThreatSummaryCard's "View details" transition. */
    const val EXPAND_COLLAPSE_MILLIS = 200

    /** Navigating between screens. */
    const val NAVIGATION_MILLIS = 250

    /** A loading indicator's own fade in/out, not the work it represents. */
    const val LOADING_MILLIS = 150

    /** Button press state changes (ripple/elevation response). */
    const val BUTTON_MILLIS = 100

    /** A card appearing (e.g. a new item entering a list). */
    const val CARD_MILLIS = 200

    /** A full-screen or large-surface transition (e.g. a dialog opening). */
    const val TRANSITION_MILLIS = 300
}
