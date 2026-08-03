package com.space.antivirus.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

/**
 * A consistent section title for screens that group content into several
 * distinct blocks. Closes the one gap `docs/design/SDS_COMPONENT_CATALOG.md`
 * (Sprint 035) recorded under **Planned Components** as `SpaceSectionHeader`
 * — named `AppSectionHeader` here to match this project's real, consistently
 * applied `App*` prefix rather than the catalog's aspirational `Space*`
 * naming, exactly as that catalog's own "A note on naming" section says
 * every entry should be read.
 *
 * Built now, in Sprint 038, specifically because the bar for extraction is
 * finally met: Home (Sprint 036, three headings) and the Cleaner (this
 * sprint, two headings) both genuinely need it *today*. That bar is not
 * incidental — Sprint 037 round 2 deleted an earlier `core:ui` extraction
 * (`AppStatGroup`) precisely because it had exactly one caller and was
 * justified by hypothetical future reuse. Home's own private
 * `SectionHeading` is removed in this same sprint and switched over to this
 * component, so this is a genuine consolidation of two call sites, not a
 * third parallel implementation.
 *
 * The optional trailing action is the catalog's own proposed anatomy ("a
 * title text, optionally with a trailing action (e.g. 'See all')"). It is
 * deliberately opt-in and absent by default — no current caller uses it, so
 * every existing heading renders exactly as it did before.
 *
 * Deliberately carries no vertical padding of its own: both callers already
 * position it inside a `Column` with an explicit `Arrangement.spacedBy`,
 * and baking in a second source of spacing would mean two different systems
 * fighting over the same gap.
 */
@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val showAction = actionText != null && onActionClick != null
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (showAction) Arrangement.SpaceBetween else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        // Checked directly rather than via `showAction` so both parameters
        // smart-cast to non-null inside the branch.
        if (actionText != null && onActionClick != null) {
            // AppTextButton already meets the touch-target and ripple
            // requirements; no bespoke clickable Text here.
            AppTextButton(text = actionText, onClick = onActionClick)
        }
    }
}
