package com.space.antivirus.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.designsystem.theme.SeverityColors

/**
 * The ONLY three severity tiers this app uses, per Sprint 002.5 §17 and
 * Sprint 002.75 §4 — deliberately not a 5-tier scale. Text label is
 * always present (never color alone) — accessibility requirement,
 * Sprint 002.5 §11.
 */
enum class Severity(val label: String) {
    INFO("Info"),
    ATTENTION("Attention"),
    ACTION_NEEDED("Action needed"),
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

    Text(
        text = severity.label,
        color = onColor,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(color)
            .padding(horizontal = spacing.small, vertical = spacing.tight),
    )
}
