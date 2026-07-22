package com.space.antivirus.core.ui.component

import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

@Composable
fun StatusChip(severity: Severity, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = {},
        label = { Text(severity.label) },
        modifier = modifier,
    )
}
