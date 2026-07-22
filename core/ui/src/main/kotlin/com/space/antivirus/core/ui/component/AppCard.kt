package com.space.antivirus.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.space.antivirus.core.designsystem.theme.LocalSpacing

/**
 * The single base Card used everywhere (status, results, recent activity,
 * premium benefits, history) per Sprint 002.5 §9 — "one base Card
 * component, content slot varies." Headline + supporting-reason line
 * matches Sprint 002.75 §5's card microcopy standard.
 */
@Composable
fun AppCard(
    headline: String,
    supportingText: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val spacing = LocalSpacing.current
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            Text(text = headline)
            supportingText?.let { Text(text = it) }
            content()
        }
    }
}
