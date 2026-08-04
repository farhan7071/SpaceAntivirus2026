package com.space.antivirus.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.designsystem.theme.SeverityColors
import com.space.antivirus.core.designsystem.theme.ShapeTokens

/**
 * Every empty state affirms the positive rather than reading as blank —
 * Sprint 002.5 §15 / Sprint 002.75 §10. This is the ONE component every
 * feature's empty state must use, so that principle can't be silently
 * dropped screen-by-screen.
 */
/**
 * Sprint 041. This was a genuine stub: an untinted, unsized `Icon` and a
 * `Text` with no style at all, stacked with no spacing between them —
 * every empty state in the app (Security Center, History, Settings,
 * Cleaner, Home's Recent Activity) rendered through it and inherited
 * that. Polishing empty states, as this sprint asks, meant fixing the
 * component rather than working around it in each caller.
 *
 * `tone` exists because of a specific honesty problem this sprint found:
 * "No threats found" was rendering behind a warning triangle. A clean
 * result is good news, and dressing good news in the same iconography as
 * a problem is the low-grade scare-tactic pattern ADR 0015 rules out —
 * it just happened to be showing up in an empty state rather than a
 * finding.
 *
 * `title` is optional and off by default, so every existing caller
 * renders exactly as before apart from the typography fix.
 *
 * `fillMaxSize()` is kept deliberately, unchanged: Home's Recent
 * Activity already relies on the current sizing behavior, and this
 * sprint is not scoped to redesign Home.
 */
@Composable
fun AppEmptyState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    tone: EmptyStateTone = EmptyStateTone.NEUTRAL,
) {
    val spacing = LocalSpacing.current
    val isDark = isSystemInDarkTheme()
    val accent = when (tone) {
        EmptyStateTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        EmptyStateTone.POSITIVE -> if (isDark) SeverityColors.SafeDark else SeverityColors.SafeLight
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(EMPTY_STATE_BADGE_SIZE)
                .clip(ShapeTokens.iconBadge)
                .background(accent.copy(alpha = if (isDark) 0.24f else 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(EMPTY_STATE_ICON_SIZE),
            )
        }
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.medium),
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.small),
        )
    }
}

private val EMPTY_STATE_BADGE_SIZE = 72.dp
private val EMPTY_STATE_ICON_SIZE = 32.dp

/**
 * Whether an empty state describes an absence (NEUTRAL) or a genuinely
 * good outcome (POSITIVE). Deliberately only two values: this is about
 * how the state should read, not a severity scale, and this project
 * keeps exactly one severity scale (`Severity`, three tiers).
 */
enum class EmptyStateTone {
    NEUTRAL,
    POSITIVE,
}
