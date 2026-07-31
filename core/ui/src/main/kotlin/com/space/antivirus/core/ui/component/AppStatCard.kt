package com.space.antivirus.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.space.antivirus.core.designsystem.theme.Elevation
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.designsystem.theme.ShapeTokens

/**
 * Sprint 036 — a single labeled statistic in its own compact card: an
 * optional icon, a large value, a small label beneath it. Extracted as a
 * genuinely shared core:ui component specifically to avoid rebuilding
 * this exact pattern a second time — ScanSummaryCard (Sprint 033/034)
 * already has an internal, private `StatColumn` composable doing the
 * same visual job inside its own dashboard layout, but that composable
 * isn't exported and couldn't be reused from a different screen without
 * either duplicating it or extracting it. This component is the
 * extraction, used by HomeScreen's Security Summary section — deliberate
 * "avoid duplicate design patterns" (Sprint 035/036's own SDS Compliance
 * goal), not a second, independently-drifting version of the same idea.
 *
 * ScanSummaryCard's own internal StatColumn was deliberately left
 * untouched rather than refactored to delegate to this component —
 * Sprint 036's own scope is Home, not Security Center, and touching an
 * already-shipped, already-tested component in a different feature's
 * screen carries real, unnecessary risk for no benefit this sprint
 * needs. A future sprint touching Security Center is the right place to
 * make that consolidation, with that screen's own tests as the safety
 * net.
 */
@Composable
fun AppStatCard(value: String, label: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val spacing = LocalSpacing.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeTokens.card,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(bottom = spacing.tight),
                )
            }
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
