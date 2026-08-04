package com.space.antivirus.core.ui.component

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.space.antivirus.core.designsystem.theme.Elevation
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.designsystem.theme.SeverityColors
import com.space.antivirus.core.designsystem.theme.ShapeTokens

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
 *
 * Sprint 033 (Part 2 — professional threat report): gained
 * threatCategory, shown at the top of the expanded state — a short,
 * user-facing label (e.g. "Permission Usage") distinct from the
 * severity chip (how concerning) and shortSummary (what was found);
 * this answers what kind of finding it is. confidenceLabel's own values
 * changed from three tiers to four (Very High / High / Medium / Low)
 * upstream in ThreatDescriptionProvider — this component's own display
 * of it is unchanged, since it was always just a String.
 *
 * Sprint 034 (Parts 2/4/5/8 — final Security Center UI polish):
 * expand/collapse now animates (AnimatedVisibility — Part 8's own
 * "Animation... Expand/collapse transitions" request, previously an
 * abrupt if(expanded) toggle) — needed adding compose-animation as an
 * explicit core:ui dependency, since AndroidLibraryComposeConventionPlugin's
 * default set (compose-ui/material3/graphics only) doesn't include it.
 * The expanded evidence section now renders each bullet as an icon +
 * short title + description row (EvidenceRow, below) instead of plain
 * "• text" bullets — Part 4's own request, reusing EvidenceIcon's title
 * field (Sprint 034) rather than inventing new evidence-category data;
 * no analyzer's evidenceDescription text changed. The recommendation
 * section gained a light background surface and icon — Part 5's own
 * "light background, recommendation icon, clear title" request,
 * replacing the plain-text block a HorizontalDivider used to separate
 * from evidence above it.
 */
@Composable
fun ThreatSummaryCard(
    appLabel: String,
    packageName: String,
    severity: Severity,
    threatCategory: String,
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
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
        shape = ShapeTokens.card,
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

                Text(text = shortSummary, style = MaterialTheme.typography.bodyMedium)

                // Sprint 041: the icon row moved below the summary and
                // shrank. It was sitting between the app name and the
                // one sentence explaining the finding, splitting the two
                // things a user actually reads first. Category rides
                // alongside it as quiet context rather than as the
                // "Threat Category: X" line the expanded section used to
                // open with.
                if (evidenceIcons.isNotEmpty() || threatCategory.isNotBlank()) {
                    EvidenceIconRow(icons = evidenceIcons, threatCategory = threatCategory)
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    ExpandedDetail(
                        threatCategory = threatCategory,
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
                        // DIAGNOSTIC (Sprint 32.1) — temporary, remove before release
                        Log.d("OverflowMenuDiag", "Ignore: DropdownMenu click")
                        onMenuExpandedChange(false)
                        onIgnoreClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Open app info") },
                    onClick = {
                        // DIAGNOSTIC (Sprint 32.1) — temporary, remove before release
                        Log.d("OverflowMenuDiag", "OpenAppInfo: menu click")
                        onMenuExpandedChange(false)
                        onOpenAppInfoClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Uninstall") },
                    onClick = {
                        // DIAGNOSTIC (Sprint 32.1) — temporary, remove before release
                        Log.d("OverflowMenuDiag", "Uninstall: menu click")
                        onMenuExpandedChange(false)
                        onUninstallClick()
                    },
                )
            }
        }
    }
}

/**
 * Sprint 041 — secondary by construction. 20dp -> 16dp and a muted
 * tint, so the row reads as metadata about the finding rather than as
 * another thing to look at above the app name. The category label sits
 * on the same line because it answers the same "what kind of finding is
 * this?" question the icons gesture at.
 */
@Composable
private fun EvidenceIconRow(icons: Set<EvidenceIcon>, threatCategory: String) {
    val spacing = LocalSpacing.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.tight),
    ) {
        icons.forEach { icon ->
            Icon(
                imageVector = icon.imageVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(EVIDENCE_ICON_SIZE),
            )
        }
        if (threatCategory.isNotBlank()) {
            Text(
                text = threatCategory,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = if (icons.isEmpty()) 0.dp else spacing.tight),
            )
        }
    }
}

@Composable
private fun ExpandedDetail(
    threatCategory: String,
    technicalDetail: String,
    evidenceBullets: List<String>,
    recommendation: String,
    confidenceLabel: String,
) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        // Sprint 041: the "Threat Category: X" line that used to open
        // this section is gone — it now sits in the collapsed card, so
        // repeating it on expand was telling the user something they had
        // already read.
        DetailSection(title = "Why it was flagged") {
            Text(text = technicalDetail, style = MaterialTheme.typography.bodySmall)
        }

        // Part 4 (Sprint 034) — "Instead of plain bullets, use rows
        // containing: Icon, Evidence title, Short description." Each
        // bullet is still exactly the same evidenceDescription text a
        // Detection already carries (Sprint 029) — this only adds an
        // icon and a short heading above it, both derived at the UI
        // layer via EvidenceIcon.inferFrom (Sprint 030) and its title
        // field (Sprint 034); no analyzer or evidence text changed.
        if (evidenceBullets.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(spacing.small))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(spacing.small),
                verticalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                Text(text = "Evidence", style = MaterialTheme.typography.labelLarge)
                evidenceBullets.forEach { bullet -> EvidenceRow(bullet) }
            }
        }

        // Part 5 (Sprint 034) — "Use: Light background, Recommendation
        // icon, Clear title, Readable spacing." Replaces the earlier
        // HorizontalDivider-separated plain-text block (Sprint 033) with
        // a genuinely distinct surface, matching the same visual
        // treatment the evidence block above already uses, so the two
        // read as clearly separate "what was found" / "what to do"
        // sections rather than one continuous column of text.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(spacing.small))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.tight),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Recommendation",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(start = spacing.tight),
                )
            }
            Text(
                text = recommendation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        // Sprint 031 (ADR 0045, goal #6 — confidence transparency), now
        // a four-tier label (Sprint 033, Part 3) rather than three —
        // shown alongside the recommendation, not the evidence bullets —
        // this is about how sure the ENGINE is overall, not a property
        // of any one piece of evidence.
        //
        // Sprint 041 promoted it from an inline "Confidence: High" line
        // to a real section with the same heading treatment as the three
        // above it. This project treats confidence as something the user
        // is entitled to see and weigh (ADR 0045); rendering it as the
        // smallest, greyest text in the card said the opposite.
        DetailSection(title = "Confidence") {
            Text(text = confidenceLabel, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Sprint 034 (Part 4) — one evidence bullet, shown as an icon + short
 * title + the bullet's own full text, rather than a plain "• text"
 * line. Uses the FIRST icon EvidenceIcon.inferFrom infers for this
 * specific bullet — a single bullet can reasonably imply more than one
 * icon (e.g. a surveillance finding mentions both camera and
 * microphone), but this row shows one representative icon/title per
 * bullet rather than stacking several, keeping each row visually simple
 * per Part 4's own "avoid clutter" framing; the bullet's own full text
 * still names everything explicitly regardless of which single icon is
 * shown above it.
 */
@Composable
private fun EvidenceRow(bulletText: String) {
    val spacing = LocalSpacing.current
    val icon = EvidenceIcon.inferFrom(bulletText).first()
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon.imageVector,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = spacing.tight)
                .size(18.dp),
        )
        Column(modifier = Modifier.padding(start = spacing.small)) {
            Text(text = icon.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(text = bulletText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Sprint 041 — one titled block inside the expanded detail. Replaces
 * `LabeledField`'s inline "Label: value" text so that all four sections
 * this sprint's brief names (why it was flagged, evidence,
 * recommendation, confidence) carry the same heading weight and are
 * separated by the same rhythm, instead of two being headed sections and
 * two being grey sentences.
 */
@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.tight)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        content()
    }
}

/**
 * Sprint 041 — full-width and left-aligned rather than a small button
 * floating at the card's left edge with the default TextButton inset.
 * "View details" is the one action every finding card has, and this
 * sprint's brief asked for it to be easier to discover; widening the
 * target and letting the chevron sit at the opposite edge makes the row
 * read as an expander rather than as an afterthought.
 */
@Composable
private fun ViewDetailsButton(expanded: Boolean, onToggle: () -> Unit) {
    TextButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (expanded) "Hide details" else "View details",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
        )
    }
}

private val EVIDENCE_ICON_SIZE = 16.dp
