package com.space.antivirus.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.space.antivirus.core.designsystem.theme.Elevation
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.designsystem.theme.SeverityColors
import com.space.antivirus.core.designsystem.theme.ShapeTokens

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
 * already compute (Sprint 033/32.1), plus purely UI-layer aggregation
 * the caller performs before calling this (counting visible threats by
 * RiskLevel, and picking the highest) — no new business logic, no new
 * ViewModel computation.
 *
 * The Shield icon's exact identity (Icons.Filled.Shield) has not been
 * verified against a real compiler in this sandbox, unlike
 * Icons.Default.Warning (confirmed safe since Sprint 017). If it's
 * wrong, it's an isolated one-line fix in this file only.
 *
 * Sprint 041 rebuilt the layout for hierarchy. The Sprint 034 version
 * rendered ten stats across three rows of identical visual weight, so
 * "467 apps scanned" and "3 ignored" competed for the same attention and
 * the card answered no question quickly. Now: one status headline, three
 * primary numbers, the highest severity as a real badge in the app's own
 * severity language, and everything else demoted to quiet secondary
 * lines.
 *
 * Nothing the user could previously see was removed — the severity
 * breakdown, trusted, ignored and average confidence are all still
 * rendered, just no longer as large numerals. Separation is by spacing
 * and type scale rather than dividers, per this sprint's own brief.
 *
 * `highestSeverity` replaced `highestSeverityLabel: String` so the badge
 * can be rendered. Mapping RiskLevel -> Severity is the same purely
 * UI-layer aggregation SecurityCenterScreen and HistoryScreen already
 * perform; no ViewModel or domain type changed.
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
    highestSeverity: Severity?,
    averageConfidenceLabel: String,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val isDark = isSystemInDarkTheme()
    val statusColor = if (isProtected) {
        if (isDark) SeverityColors.SafeDark else SeverityColors.SafeLight
    } else {
        if (isDark) SeverityColors.AttentionDark else SeverityColors.AttentionLight
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
        shape = ShapeTokens.card,
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            StatusHeader(
                isProtected = isProtected,
                lastScanText = lastScanText,
                statusColor = statusColor,
            )

            PrimaryStatsRow(
                appsScanned = appsScanned,
                findingsCount = findingsCount,
                scanDurationLabel = scanDurationLabel,
            )

            // Highest severity earns a real badge rather than a fourth
            // stat column: it is the one metric here that is a severity,
            // and rendering it in the app's own severity language makes
            // it readable at a glance instead of as another number.
            // Absent when there is nothing to rank — "Highest severity:
            // None" is noise on a clean result.
            highestSeverity?.let { severity ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Highest severity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = spacing.small),
                    )
                    StatusChip(severity)
                }
            }

            // The severity breakdown and the trusted/ignored/confidence
            // figures are all still here — nothing the user could see
            // before was removed. They are demoted to quiet single lines
            // rather than two more rows of large numbers competing with
            // the three that actually answer "what happened in my last
            // scan?". Separation by spacing, not dividers.
            if (findingsCount > 0) {
                QuietLine("$infoCount informational \u00B7 $attentionCount attention \u00B7 $highRiskCount high risk")
            }
            QuietLine("$trustedCount trusted \u00B7 $ignoredCount ignored \u00B7 $averageConfidenceLabel confidence")
        }
    }
}

/**
 * Sprint 041 — the card's single primary focus. Status headline, and the
 * one piece of context that qualifies it (when this was measured). The
 * icon is tinted by outcome rather than always `primary`, so a result
 * needing attention no longer reads as brand-coloured reassurance.
 */
@Composable
private fun StatusHeader(isProtected: Boolean, lastScanText: String, statusColor: Color) {
    val spacing = LocalSpacing.current
    val isDark = isSystemInDarkTheme()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(STATUS_BADGE_SIZE)
                .clip(ShapeTokens.iconBadge)
                .background(statusColor.copy(alpha = if (isDark) 0.24f else 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isProtected) Icons.Filled.Shield else Icons.Default.Warning,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(STATUS_ICON_SIZE),
            )
        }
        Column(modifier = Modifier.padding(start = spacing.medium)) {
            Text(
                text = if (isProtected) "All good!" else "Needs your attention",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Last scan: $lastScanText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Sprint 041 — the three metrics that answer "what did the last scan
 * actually do?": how much was checked, what came back, how long it took.
 * Previously these sat in the first of three identical-weight rows of
 * ten stats, which is what made the card hard to scan.
 */
@Composable
private fun PrimaryStatsRow(appsScanned: Int, findingsCount: Int, scanDurationLabel: String) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        StatColumn(value = "$appsScanned", label = "Apps scanned", modifier = Modifier.weight(1f))
        StatColumn(value = "$findingsCount", label = "Findings", modifier = Modifier.weight(1f))
        StatColumn(value = scanDurationLabel, label = "Duration", modifier = Modifier.weight(1f))
    }
}

/**
 * Sprint 041 — one primary dashboard stat. The icons the Sprint 034
 * version put above each value are gone: with only three stats left in
 * this row, each already labelled in words, the icons were decoration
 * competing with the numbers rather than aiding recognition.
 */
@Composable
private fun StatColumn(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Secondary detail: present and readable, but never competing with the
 *  numbers above it. */
@Composable
private fun QuietLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val STATUS_BADGE_SIZE = 56.dp
private val STATUS_ICON_SIZE = 28.dp
