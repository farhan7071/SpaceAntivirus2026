package com.space.antivirus.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.designsystem.theme.SeverityColors

/**
 * Sprint 030 — the shared card component behind both SecurityCenterScreen
 * and the History screen's app-finding cards (ADR 0044). A dedicated,
 * purpose-built component rather than a variant of AppCard: AppCard's
 * own documented role (Sprint 002.5 §9, "one base Card component,
 * content slot varies") is for simple headline/supportingText/content
 * cards — Home's status cards, results summaries. This card has a
 * genuinely different structural shape (a colored left accent edge,
 * several distinct always-visible and expandable sections, built-in
 * interactive state) that doesn't fit that generic shape without
 * bloating AppCard with concerns specific to one kind of card. Building
 * it here, once, and sharing it between both screens is exactly what the
 * "both screens should share the same UI components where practical"
 * requirement asks for.
 *
 * Deliberately takes simple, UI-specific parameters (String/enum/List<String>),
 * never domain types (Threat, Detection) directly — core:ui has zero
 * dependency on domain or core:model, matching its existing shape before
 * this sprint; each screen's own ViewModel maps its domain data into
 * these parameters, the same mapping responsibility SecurityCenterViewModel
 * and HistoryViewModel have always had.
 *
 * Collapsed by default (own local, unlifted state — this is pure
 * presentation state with nothing to persist or observe, unlike the
 * domain-backed UiState above it). "View Details" toggles it; Ignore /
 * Open App Info / Uninstall live behind the overflow menu, matching the
 * reference mockup's own layout (a three-dot menu, top-right) and this
 * sprint's own "primary action / secondary action" framing — View
 * Details is the one action every card needs; the other three are less
 * frequently used, appropriately less prominent.
 *
 * Uninstall is offered unconditionally, with no isSystemApp check needed
 * here — every one of this project's eight analyzers already excludes
 * system apps before ever producing a Detection (ADR 0027 onward), so
 * any app this card can even be built for is already guaranteed
 * non-system by construction.
 *
 * Sprint 031 (ADR 0045, goal #6 — confidence transparency): gained
 * confidenceLabel, shown in the expanded state alongside the
 * recommendation. A plain String, not core:model's Confidence type —
 * consistent with this component's existing zero-dependency-on-domain
 * shape; the mapping happens in each screen's ViewModel.
 */
@Composable
fun ThreatSummaryCard(
    appLabel: String,
    packageName: String,
    severity: Severity,
    evidenceIcons: Set<EvidenceIcon>,
    shortSummary: String,
    technicalDetail: String,
    evidenceBullets: List<String>,
    recommendation: String,
    confidenceLabel: String,
    onIgnoreClick: () -> Unit,
    onOpenAppInfoClick: () -> Unit,
    onUninstallClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val spacing = LocalSpacing.current
    val isDark = isSystemInDarkTheme()
    val accentColor = when (severity) {
        Severity.INFO -> if (isDark) SeverityColors.InfoDark else SeverityColors.InfoLight
        Severity.ATTENTION -> if (isDark) SeverityColors.AttentionDark else SeverityColors.AttentionLight
        Severity.ACTION_NEEDED -> if (isDark) SeverityColors.ActionNeededDark else SeverityColors.ActionNeededLight
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                IdentityRow(
                    appLabel = appLabel,
                    packageName = packageName,
                    severity = severity,
                    menuExpanded = menuExpanded,
                    onMenuExpandedChange = { menuExpanded = it },
                    onIgnoreClick = onIgnoreClick,
                    onOpenAppInfoClick = onOpenAppInfoClick,
                    onUninstallClick = onUninstallClick,
                )

                if (evidenceIcons.isNotEmpty()) {
                    EvidenceIconRow(evidenceIcons)
                }

                Text(text = shortSummary, style = MaterialTheme.typography.bodyMedium)

                if (expanded) {
                    ExpandedDetail(
                        technicalDetail = technicalDetail,
                        evidenceBullets = evidenceBullets,
                        recommendation = recommendation,
                        confidenceLabel = confidenceLabel,
                    )
                }

                ViewDetailsButton(expanded = expanded, onToggle = { expanded = !expanded })
            }
        }
    }
}

@Composable
private fun IdentityRow(
    appLabel: String,
    packageName: String,
    severity: Severity,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onIgnoreClick: () -> Unit,
    onOpenAppInfoClick: () -> Unit,
    onUninstallClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(packageName = packageName, appLabel = appLabel)
        Spacer(modifier = Modifier.width(spacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusChip(severity)
        Box {
            IconButton(onClick = { onMenuExpandedChange(true) }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More actions")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { onMenuExpandedChange(false) }) {
                DropdownMenuItem(
                    text = { Text("Ignore") },
                    onClick = {
                        onMenuExpandedChange(false)
                        onIgnoreClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Open app info") },
                    onClick = {
                        onMenuExpandedChange(false)
                        onOpenAppInfoClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Uninstall") },
                    onClick = {
                        onMenuExpandedChange(false)
                        onUninstallClick()
                    },
                )
            }
        }
    }
}

@Composable
private fun EvidenceIconRow(icons: Set<EvidenceIcon>) {
    val spacing = LocalSpacing.current
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
        icons.forEach { icon ->
            Icon(
                imageVector = icon.imageVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ExpandedDetail(
    technicalDetail: String,
    evidenceBullets: List<String>,
    recommendation: String,
    confidenceLabel: String,
) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        Text(text = "Why it was flagged", style = MaterialTheme.typography.titleSmall)
        Text(text = technicalDetail, style = MaterialTheme.typography.bodySmall)

        evidenceBullets.forEach { bullet ->
            Text(text = "\u2022 $bullet", style = MaterialTheme.typography.bodySmall)
        }

        Text(text = "Recommendation", style = MaterialTheme.typography.titleSmall)
        Text(text = recommendation, style = MaterialTheme.typography.bodySmall)

        // Sprint 031 (ADR 0045, goal #6 — confidence transparency):
        // shown alongside the recommendation, not the evidence bullets —
        // this is about how sure the ENGINE is overall, not a property
        // of any one piece of evidence.
        Text(
            text = "Confidence: $confidenceLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ViewDetailsButton(expanded: Boolean, onToggle: () -> Unit) {
    val spacing = LocalSpacing.current
    TextButton(onClick = onToggle) {
        Text(if (expanded) "Hide details" else "View details")
        Spacer(modifier = Modifier.width(spacing.tight))
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
        )
    }
}
