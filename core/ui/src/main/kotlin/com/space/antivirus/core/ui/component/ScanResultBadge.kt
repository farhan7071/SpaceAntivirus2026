package com.space.antivirus.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.designsystem.theme.SeverityColors

/**
 * Sprint 034 (Part 7 — "Use a colored badge indicating overall scan
 * result... Examples: Safe, Attention, Suspicious, High Risk"). A scan
 * session's own result badge, distinct from StatusChip: a session is
 * either clean (no findings at all — a genuinely different concept from
 * "how severe is this specific finding," which is what Severity's three
 * tiers measure) or it has findings, in which case its result is
 * exactly the highest Severity among those findings, and this delegates
 * straight to the existing StatusChip rather than duplicating its
 * rendering.
 *
 * Fixes a real, pre-existing bug found while building this: History's
 * ScanHistoryEntryCard previously called StatusChip(Severity.INFO) for
 * clean sessions — "Informational" was never the right word for "found
 * nothing," it just happened to be the mildest existing severity value
 * available to reuse. This badge is the honest fix: a clean session
 * gets its own, correctly-labeled "Safe" treatment, not a severity tier
 * being pressed into service for a concept it was never meant to cover.
 *
 * "Suspicious," named in the reference design, is deliberately not a
 * distinct badge here — the same reasoning Severity's own KDoc gives
 * for staying at three tiers applies identically to this component: no
 * analyzer or scorer in this project computes a "suspicious, but not
 * quite high-risk" signal distinct from ACTION_NEEDED, so there is
 * nothing real to badge as a separate category.
 */
@Composable
fun ScanResultBadge(isClean: Boolean, highestSeverity: Severity, modifier: Modifier = Modifier) {
    if (!isClean) {
        StatusChip(severity = highestSeverity, modifier = modifier)
        return
    }

    val spacing = LocalSpacing.current
    val isDark = isSystemInDarkTheme()
    val color = if (isDark) SeverityColors.SafeDark else SeverityColors.SafeLight
    val onColor = if (isDark) Color.Black else Color.White

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(color)
            .padding(horizontal = spacing.small, vertical = spacing.tight),
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = onColor,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Safe",
            color = onColor,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = spacing.tight),
        )
    }
}
