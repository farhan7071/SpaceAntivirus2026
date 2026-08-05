package com.space.antivirus.core.designsystem.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Space Design System v1.0, Part 5 — Shape.
 *
 * M3's own five-tier corner-radius scale — the raw values. Cards default
 * to "medium" (per Sprint 002.5 Section 8); the scan-progress hero element
 * animates across this scale via shape morphing rather than using a
 * fixed shape — implemented in feature:security, not here (this module
 * only defines the static scale).
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Sprint 035 — semantic shape tokens: which component gets which tier
 * of AppShapes' scale, named by role rather than left for every
 * component author to guess or hardcode independently. Before this,
 * StatusChip and ScanResultBadge each wrote `RoundedCornerShape(percent
 * = 50)` directly inline — the same shape decision made twice, with no
 * shared name, and no way for a future third badge-like component to
 * discover that decision already existed rather than making its own.
 *
 * Deliberately built on top of AppShapes rather than defining new corner
 * radii — this file answers "which of the five already-defined tiers
 * does a Button/Card/Dialog/Badge use," it does not invent a sixth
 * scale. `badge` and `chip` are the one exception: a full 50%-rounded
 * capsule isn't expressible as one of AppShapes' five fixed corner
 * radii (those are constant dp values, not proportional), so it's
 * defined directly here — still a single, named, reusable token, not a
 * literal repeated at each badge call site.
 */
object ShapeTokens {
    val button = RoundedCornerShape(percent = 50)
    val card = AppShapes.medium

    /**
     * Sprint 036.5 (design-lead review pass) — the Hero Card's own,
     * deliberately distinct silhouette: AppShapes.large (16dp) rather
     * than the standard `card` token (medium, 12dp) every other card on
     * Home uses. "The Hero Card should feel intentional rather than
     * simply being a large colored rectangle" and "should become the
     * signature element of Space Antivirus" - a genuinely different
     * corner radius, not just a different color, is part of what makes
     * a screenshot of this card recognizable at a glance rather than
     * reading as one more Material3 card among several identical ones.
     */
    val heroCard = AppShapes.large
    val dialog = AppShapes.large
    val bottomSheet = RoundedCornerShape(
        // Matches AppShapes.large's corner radius (16.dp) for its two
        // rounded corners - a bottom sheet's top edge should read as the
        // same "how rounded" tier as a large card or dialog, just with
        // its bottom two corners square since it's flush with the
        // screen edge. Not expressed as "based on AppShapes.large"
        // directly, since RoundedCornerShape's four-corner constructor
        // needs each corner's own CornerSize, not another shape to
        // derive from.
        topStart = CornerSize(16.dp),
        topEnd = CornerSize(16.dp),
        bottomStart = CornerSize(0.dp),
        bottomEnd = CornerSize(0.dp),
    )
    val badge = RoundedCornerShape(percent = 50)
    val chip = RoundedCornerShape(percent = 50)
    val navigation = AppShapes.small

    /**
     * Sprint 036.5 — a decorative circular container behind a large,
     * prominent icon (e.g. the Hero Card's status icon badge). Shares
     * badge/chip's exact value (a full 50%-rounded capsule) — same
     * reasoning as this file's own precedent above: named by its own
     * semantic role (a decorative icon container, not a StatusChip-style
     * status badge) rather than reusing `badge` under a name that would
     * mislead a future reader of the call site into thinking it's the
     * same StatusChip/ScanResultBadge shape for a different reason.
     */
    val iconBadge = RoundedCornerShape(percent = 50)
}
