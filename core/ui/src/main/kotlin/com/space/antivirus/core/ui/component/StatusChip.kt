package com.space.antivirus.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.designsystem.theme.SeverityColors
import com.space.antivirus.core.designsystem.theme.ShapeTokens

/**
 * The ONLY three severity tiers this app uses, per Sprint 002.5 §17 and
 * Sprint 002.75 §4 — deliberately not a 5-tier scale. Sprint 034's
 * reference design showed five visual levels (Trusted/Informational/
 * Attention/Suspicious/High Risk); this enum deliberately did NOT grow
 * to match. "Suspicious" as a genuinely distinct fourth severity would
 * need CumulativeRiskScorer or an analyzer to actually compute a
 * meaningfully different signal for it — inventing a visual distinction
 * with nothing real underneath it would be presentation dishonesty, the
 * opposite of "user trust," one of this same sprint's own stated design
 * goals. "Trusted"/green has no per-Threat equivalent at all — a Threat
 * only exists once something has already been flagged, so there is no
 * "trusted Threat" to badge; that color is used instead for the overall
 * protected/no-findings state (SecurityCenterScreen's Scan Summary,
 * History's session badges), never for this enum. ACTION_NEEDED's label
 * changed from "Action needed" to "High Risk" to match the reference
 * design's terminology more closely — a pure copy change, same
 * enum, same underlying RiskLevel it's mapped from.
 *
 * Text label is always present (never color alone) — accessibility
 * requirement, Sprint 002.5 §11. Sprint 034 added a matching icon per
 * tier for the same reason color alone was never enough — icon and
 * color together carry the meaning, not either one in isolation.
 */
enum class Severity(val label: String, val icon: ImageVector) {
    INFO("Informational", Icons.Filled.Info),
    ATTENTION("Attention", Icons.Filled.Warning),
    ACTION_NEEDED("High Risk", Icons.Filled.Error),
}

/**
 * Sprint 030 fix (real-device report — "the current chip appears
 * clickable but performs no action"): this used to be a Material3
 * AssistChip with an empty onClick lambda. AssistChip is inherently an
 * interactive component — ripple, press states, the works — so wrapping
 * a no-op around it produced exactly the reported symptom: something
 * that visually promises an action it never performs. Rewritten as a
 * genuinely non-interactive display element (no onClick parameter
 * exists on this composable at all — there is nothing to wire up
 * accidentally, unlike removing an AssistChip's onClick and leaving the
 * component type itself still clickable-by-default). If a future sprint
 * wants this to open a severity explanation instead, that's a
 * deliberate, additive change to a component that's honest about having
 * no behavior today, not a fix to a chip that already looked
 * interactive without being asked to.
 *
 * Also the first real use of SeverityColors (designsystem, defined but
 * never actually wired into any component until now) — light/dark
 * variants selected via isSystemInDarkTheme(), the same mechanism
 * Compose's own MaterialTheme uses internally, so this stays correct
 * automatically under system theme changes without this component
 * needing to know anything about how the surrounding theme was built.
 *
 * Sprint 034 (Part 3 — "Each badge should have: Icon, Color, Rounded
 * capsule, Consistent padding"): gained Severity's own icon, shown
 * before the label inside the same capsule.
 *
 * Sprint 041: ACTION_NEEDED and ATTENTION no longer share one glyph.
 * They did because Sprint 034 was cautious about which icons were safe
 * in this environment (ADR 0031) — but that caution applies to FEATURE
 * modules, and this file is in core:ui, which has carried
 * compose-material-icons-extended since Sprint 030. Two tiers rendering
 * an identical triangle meant the middle and top of a three-tier scale
 * were distinguished by color alone at a glance, which is exactly the
 * distinction this sprint was asked to strengthen — and the weakest
 * distinction for anyone with a color-vision deficiency. Icon, color and
 * text label now all three differ per tier.
 *
 * The scale itself is still three tiers, and deliberately so. Sprint
 * 041's brief mentions "Suspicious" as though it were a fourth: it is
 * not one, for the same reason it wasn't in Sprint 034. No analyzer and
 * no CumulativeRiskScorer output computes a signal distinct from these
 * three, so a fourth badge would be a visual distinction with nothing
 * real underneath it.
 */
@Composable
fun StatusChip(severity: Severity, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    val isDark = isSystemInDarkTheme()
    val color = when (severity) {
        Severity.INFO -> if (isDark) SeverityColors.InfoDark else SeverityColors.InfoLight
        Severity.ATTENTION -> if (isDark) SeverityColors.AttentionDark else SeverityColors.AttentionLight
        Severity.ACTION_NEEDED -> if (isDark) SeverityColors.ActionNeededDark else SeverityColors.ActionNeededLight
    }
    val onColor = if (isDark) Color.Black else Color.White

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(ShapeTokens.chip)
            .background(color)
            .padding(horizontal = spacing.small, vertical = spacing.tight),
    ) {
        Icon(
            imageVector = severity.icon,
            contentDescription = null,
            tint = onColor,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = severity.label,
            color = onColor,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = spacing.tight),
        )
    }
}
