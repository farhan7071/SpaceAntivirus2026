package com.space.antivirus.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
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
import com.space.antivirus.core.designsystem.theme.LocalSpacing

/**
 * Sprint 034 (Part 1 — "build a dashboard card... immediately answer
 * 'Is my phone safe?' without requiring the user to scroll"). Lives in
 * core:ui, not feature:security, specifically because it needs extended
 * Material icons (Shield, Search, CheckCircle, Timer) — feature:security
 * has no compose-material-icons-extended dependency and never has (ADR
 * 0031's standing caution deliberately keeps feature modules restricted
 * to Icons.Default.Warning), matching exactly why ThreatSummaryCard,
 * AppIcon, and EvidenceIcon all already live here instead of in a
 * feature module. Deliberately takes simple, already-formatted UI
 * parameters (String/Int/Boolean), never domain types or ScanSummary
 * itself — the same zero-dependency-on-domain shape ThreatSummaryCard
 * already established for this module; SecurityCenterScreen.kt maps its
 * own ScanSummary into these parameters, the same mapping responsibility
 * it already has for ThreatSummary -> ThreatSummaryCard's parameters.
 *
 * Every value shown is exactly what ScanSummary/SecurityCenterUiState
 * already compute (Sprint 033/32.1), plus one purely UI-layer
 * aggregation the caller performs before calling this (counting visible
 * threats by RiskLevel) — no new business logic, no new ViewModel
 * computation.
 *
 * The Shield icon's exact identity (Icons.Filled.Shield) has not been
 * verified against a real compiler in this sandbox, unlike
 * Icons.Default.Warning (confirmed safe since Sprint 017), used here for
 * the non-protected state and for the Findings stat. Same honesty
 * EvidenceIcon's own KDoc already uses for its similarly-unverified
 * choices (Search, CheckCircle, Timer) — if any is wrong, it's an
 * isolated, one-line fix in this file only.
 */
@Composable
fun ScanSummaryCard(
    isProtected: Boolean,
    lastScanText: String,
    appsScanned: Int,
    findingsCount: Int,
    trustedCount: Int,
    infoCount: Int,
    attentionCount: Int,
    highRiskCount: Int,
    ignoredCount: Int,
    scanDurationLabel: String,
    highestSeverityLabel: String,
    averageConfidenceLabel: String,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(spacing.small),
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isProtected) Icons.Filled.Shield else Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
                Column(modifier = Modifier.padding(start = spacing.small)) {
                    Text(
                        text = if (isProtected) "All good!" else "Needs your attention",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Last scan: $lastScanText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn(icon = Icons.Filled.Search, value = "$appsScanned", label = "Apps scanned")
                StatColumn(icon = Icons.Default.Warning, value = "$findingsCount", label = "Findings")
                StatColumn(icon = Icons.Filled.CheckCircle, value = "$trustedCount", label = "Trusted")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn(value = "$infoCount", label = "Info")
                StatColumn(value = "$attentionCount", label = "Attention")
                StatColumn(value = "$highRiskCount", label = "High Risk")
                StatColumn(value = "$ignoredCount", label = "Ignored")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn(icon = Icons.Filled.Timer, value = scanDurationLabel, label = "Scan duration")
                StatColumn(value = highestSeverityLabel, label = "Highest severity")
                StatColumn(value = averageConfidenceLabel, label = "Avg. confidence")
            }
        }
    }
}

/**
 * Sprint 034 (Part 1) — one dashboard stat: an optional icon, a large
 * value, and a small label beneath it. Icon is nullable — some stats
 * here (Info/Attention/High Risk/Ignored counts, Highest severity, Avg.
 * confidence) don't have a confidently-verified icon available and are
 * shown as value+label only, honestly, rather than guessing at more
 * icon names than necessary.
 */
@Composable
private fun StatColumn(value: String, label: String, icon: ImageVector? = null) {
    val spacing = LocalSpacing.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
