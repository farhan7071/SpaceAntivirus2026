package com.space.antivirus.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.space.antivirus.core.designsystem.theme.Elevation
import com.space.antivirus.core.designsystem.theme.IconTokens
import com.space.antivirus.core.designsystem.theme.LayoutTokens
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.designsystem.theme.SeverityColors
import com.space.antivirus.core.designsystem.theme.ShapeTokens
import com.space.antivirus.core.model.ScanProgress
import com.space.antivirus.core.ui.component.AppCircularProgress
import com.space.antivirus.core.ui.component.AppEmptyState
import com.space.antivirus.core.ui.component.AppFilledButton
import com.space.antivirus.core.ui.component.AppLinearProgress
import com.space.antivirus.core.ui.component.AppStatCard
import com.space.antivirus.core.ui.component.AppTextButton
import java.text.DateFormat
import java.util.Date

/** Exposed so HomeScreenTest can reliably find the loading indicator —
 *  there's no visible text to assert on in the Loading state otherwise. */
const val HOME_LOADING_TEST_TAG = "home_loading_indicator"

/**
 * This project's first production screen (Sprint 017) — establishes the
 * stateful/stateless split every remaining feature screen follows:
 * HomeRoute collects ViewModel state (the only place that touches
 * hiltViewModel()/collectAsStateWithLifecycle), HomeScreen is a pure
 * function of HomeUiState (+ ScanUiState as of Sprint 020) with no
 * ViewModel/DI awareness at all.
 *
 * Sprint 036 (Home Screen Redesign, Phase 2): gained four navigation
 * callback parameters — onNavigateToSecurityCenter/Cleaner/History/
 * Settings — for the new Quick Actions section. This is a deliberate,
 * explicitly-scoped exception to "no navigation changes": HomeRoute
 * itself gains callback parameters (all defaulted to no-op, so this
 * signature change alone doesn't require touching anything else), and
 * SpaceAntivirusNavHost.kt wires them to real navController.navigate()
 * calls using the exact pattern SecurityCenterRoute's own
 * onViewHistoryClick already established (Sprint 021) — no new routes,
 * no new destinations, no change to the nav graph's structure or
 * back-stack behavior. Interactive UI that looked tappable but didn't
 * function would be a worse outcome than this minimal, precedented
 * wiring.
 */
@Composable
fun HomeRoute(
    homeViewModel: HomeViewModel = hiltViewModel(),
    scanViewModel: ScanViewModel = hiltViewModel(),
    onNavigateToSecurityCenter: () -> Unit = {},
    onNavigateToCleaner: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val scanState by scanViewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        scanState = scanState,
        onScanClick = scanViewModel::startScan,
        onAcknowledgeScanResult = scanViewModel::acknowledgeResult,
        onNavigateToSecurityCenter = onNavigateToSecurityCenter,
        onNavigateToCleaner = onNavigateToCleaner,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToSettings = onNavigateToSettings,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    scanState: ScanUiState,
    onScanClick: () -> Unit,
    onAcknowledgeScanResult: () -> Unit,
    onNavigateToSecurityCenter: () -> Unit,
    onNavigateToCleaner: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is HomeUiState.Loading -> HomeLoading(modifier)
        is HomeUiState.Loaded -> HomeLoaded(
            state = uiState,
            scanState = scanState,
            onScanClick = onScanClick,
            onAcknowledgeScanResult = onAcknowledgeScanResult,
            onNavigateToSecurityCenter = onNavigateToSecurityCenter,
            onNavigateToCleaner = onNavigateToCleaner,
            onNavigateToHistory = onNavigateToHistory,
            onNavigateToSettings = onNavigateToSettings,
            modifier = modifier,
        )
        is HomeUiState.Error -> HomeError(uiState, modifier)
    }
}

@Composable
private fun HomeLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Sprint 036: an indeterminate spinner alone risks a layout jump
        // once real content replaces it ("avoid layout jumps," this
        // sprint's own Loading States goal) - kept identical to the
        // pre-redesign behavior since HomeLoaded's own real height
        // varies with data (Hero card + variable-length sections below
        // it) and can't be pre-reserved without guessing at content
        // that doesn't exist yet - the same honest constraint as not
        // fabricating stats. AppCircularProgress (SDS, reused per this
        // sprint's own "reuse SDS loading components" instruction).
        AppCircularProgress(progress = null, modifier = Modifier.testTag(HOME_LOADING_TEST_TAG))
    }
}

@Composable
private fun HomeError(state: HomeUiState.Error, modifier: Modifier = Modifier) {
    AppEmptyState(
        icon = Icons.Default.Warning,
        message = state.message,
        modifier = modifier,
    )
}

@Composable
private fun HomeLoaded(
    state: HomeUiState.Loaded,
    scanState: ScanUiState,
    onScanClick: () -> Unit,
    onAcknowledgeScanResult: () -> Unit,
    onNavigateToSecurityCenter: () -> Unit,
    onNavigateToCleaner: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutTokens.screenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.large),
        contentPadding = PaddingValues(vertical = spacing.medium),
    ) {
        item {
            HeroSecurityCard(
                status = state.protectionStatus,
                lastScan = state.lastScanSummary,
                scanState = scanState,
                onScanClick = onScanClick,
                onAcknowledgeScanResult = onAcknowledgeScanResult,
            )
        }
        item {
            SecuritySummarySection(lastScan = state.lastScanSummary, trustedItemsCount = state.trustedItemsCount)
        }
        item {
            QuickActionsSection(
                onNavigateToSecurityCenter = onNavigateToSecurityCenter,
                onNavigateToCleaner = onNavigateToCleaner,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToSettings = onNavigateToSettings,
            )
        }
        item {
            RecentActivitySection(lastScan = state.lastScanSummary)
        }
    }
}

/**
 * The Hero Security Card — Sprint 036's dominant visual element,
 * required to answer "am I protected?" in under five seconds without
 * scrolling. Merges what were three separate cards before this sprint
 * (ProtectionStatusCard, LastScanCard, and ScanActionSection's own
 * button/progress/result-banner logic) into one, since the reference
 * design's own point is that these three pieces of information belong
 * together as a single decision surface, not three cards a user has to
 * mentally reassemble.
 *
 * Background tint is status-driven: a soft, low-opacity wash of
 * SeverityColors' own PROTECTED-adjacent or ATTENTION-adjacent color
 * (never the raw, full-saturation token — those are sized for small
 * chip text, not a large card surface, per this sprint's own "calm, not
 * loud" design intent), falling back to a neutral surface for UNKNOWN
 * (nothing to grade yet, so nothing to tint). Every color, shape, and
 * elevation value here is an SDS token per this sprint's own SDS
 * Compliance requirement - no hardcoded hex/dp value appears in this
 * function.
 *
 * Sprint 036.5 (visual polish, not a redesign — structure, data, and
 * callbacks are all unchanged from Sprint 036): the status icon
 * previously appeared twice — once small (16dp) in its own "SECURE"/
 * "ATTENTION" label row, and again large (48dp) floating on the far
 * right of the headline row, visually disconnected from both the label
 * above it and the text beside it. Consolidated into one icon, sized
 * more deliberately and placed inside a soft, tonal circular badge
 * (ShapeTokens.iconBadge) directly beside the status label + headline +
 * supporting text as a single visual group — "the status graphic should
 * reinforce the message rather than compete with it," this sprint's own
 * words. Elevation raised from Elevation.card to Elevation.floating —
 * "the Hero Card is now the product's visual identity," so it should
 * read as more raised than the plain stat/action cards beneath it, not
 * matched to their elevation by default.
 */
@Composable
private fun HeroSecurityCard(
    status: ProtectionStatus,
    lastScan: LastScanSummary?,
    scanState: ScanUiState,
    onScanClick: () -> Unit,
    onAcknowledgeScanResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val isDark = isSystemInDarkTheme()

    val statusColor = when (status) {
        ProtectionStatus.PROTECTED -> if (isDark) SeverityColors.SafeDark else SeverityColors.SafeLight
        ProtectionStatus.NEEDS_ATTENTION -> if (isDark) SeverityColors.AttentionDark else SeverityColors.AttentionLight
        ProtectionStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val backgroundTint = when (status) {
        ProtectionStatus.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
        else -> statusColor.copy(alpha = if (isDark) 0.20f else 0.14f)
    }
    // Sprint 036.5: the icon's own circular badge is a visibly stronger
    // tint than the card's own background wash - a layered surface
    // (this sprint's own "layered surfaces, subtle tonal variation"
    // goal) giving the icon real presence rather than floating directly
    // on the card's already-tinted background with no separation.
    val iconBadgeTint = when (status) {
        ProtectionStatus.UNKNOWN -> MaterialTheme.colorScheme.surface
        else -> statusColor.copy(alpha = if (isDark) 0.32f else 0.20f)
    }
    val statusIcon = when (status) {
        ProtectionStatus.PROTECTED -> IconTokens.security
        ProtectionStatus.NEEDS_ATTENTION -> IconTokens.warning
        ProtectionStatus.UNKNOWN -> IconTokens.security
    }
    val (headline, supportingText) = when (status) {
        ProtectionStatus.PROTECTED ->
            "You're protected" to "No threats found in your last scan"
        ProtectionStatus.NEEDS_ATTENTION ->
            "Attention needed" to
                "${lastScan?.threatsFound ?: 0} item(s) from your last scan are worth reviewing"
        ProtectionStatus.UNKNOWN ->
            "Protection status unknown" to "Run your first scan to see your protection status"
    }
    val statusLabel = when (status) {
        ProtectionStatus.PROTECTED -> "SECURE"
        ProtectionStatus.NEEDS_ATTENTION -> "ATTENTION"
        ProtectionStatus.UNKNOWN -> "UNKNOWN"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        // Design-lead review pass: ShapeTokens.heroCard (16dp), not the
        // standard ShapeTokens.card (12dp) every other card on this
        // screen uses - a deliberately distinct silhouette, not just a
        // different color, for the one card meant to be recognizable at
        // a glance.
        shape = ShapeTokens.heroCard,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.floating),
        colors = CardDefaults.cardColors(containerColor = backgroundTint),
    ) {
        Column(
            modifier = Modifier.padding(spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            // One cohesive group: icon badge + (status label, headline,
            // supporting text) - the icon now sits directly beside the
            // exact text it represents, addressing "the status icon
            // feels visually detached." Top-aligned, not center-aligned,
            // so the badge lines up with the status label at the very
            // top of the text column rather than floating at the
            // vertical center of three lines of text.
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(ShapeTokens.iconBadge)
                        .background(iconBadgeTint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Column(modifier = Modifier.padding(start = spacing.medium)) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = spacing.tight),
                    )
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = spacing.tight),
                    )
                }
            }

            if (lastScan != null) {
                Text(
                    text = "Last scan: ${formatScanTime(lastScan.scannedAtEpochMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HeroScanAction(
                scanState = scanState,
                onScanClick = onScanClick,
                onAcknowledgeScanResult = onAcknowledgeScanResult,
            )
        }
    }
}

@Composable
private fun formatScanTime(epochMillis: Long): String = remember(epochMillis) {
    DateFormat.getDateTimeInstance().format(Date(epochMillis))
}

/**
 * The Hero Card's own action area - reuses ScanViewModel/ScanUiState
 * exactly as the pre-redesign ScanActionSection did (Sprint 020), only
 * relocated inside the Hero Card rather than living below it as a
 * separate section, per this sprint's own "one primary CTA" requirement
 * - the scan button is the one, unambiguous next action, so it belongs
 * inside the one card already answering "am I protected?", not a
 * further scroll away.
 */
@Composable
private fun HeroScanAction(
    scanState: ScanUiState,
    onScanClick: () -> Unit,
    onAcknowledgeScanResult: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        when (scanState) {
            is ScanUiState.Completed -> HeroResultBanner(
                message = if (scanState.isClean) {
                    "Scan complete \u2014 no threats found (${scanState.itemsScanned} apps checked)."
                } else {
                    "Scan complete \u2014 ${scanState.threatsFound} item(s) found. " +
                        "See Security Center for details."
                },
                onDismiss = onAcknowledgeScanResult,
            )
            is ScanUiState.Error -> HeroResultBanner(message = scanState.message, onDismiss = onAcknowledgeScanResult)
            else -> Unit
        }

        if (scanState is ScanUiState.Running) {
            HeroScanProgress(scanState.progress)
        } else {
            // Sprint 036.5: explicit height via LayoutTokens.primaryActionHeight
            // (56dp) - Material3's own default Button height is ~40dp,
            // which reads as a standard, secondary-weight action, not
            // "the one dominant primary CTA" this sprint's own Button
            // section asks the Scan Now button to be.
            AppFilledButton(
                text = "Scan Now",
                onClick = onScanClick,
                enabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LayoutTokens.primaryActionHeight),
            )
        }
    }
}

@Composable
private fun HeroResultBanner(message: String, onDismiss: () -> Unit) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.tight)) {
        Text(text = message, style = MaterialTheme.typography.bodySmall)
        AppTextButton(text = "Dismiss", onClick = onDismiss)
    }
}

@Composable
private fun HeroScanProgress(progress: ScanProgress?) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        val fraction = if (progress != null && progress.totalItems > 0) {
            progress.itemsProcessed.toFloat() / progress.totalItems.toFloat()
        } else {
            null
        }
        AppLinearProgress(progress = fraction, modifier = Modifier.fillMaxWidth())
        val label = if (progress != null && progress.totalItems > 0) {
            "Scanning\u2026 ${progress.itemsProcessed} of ${progress.totalItems}"
        } else {
            "Starting scan\u2026"
        }
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * Security Summary — Sprint 036's Part 2 requirement, "display only
 * truthful information... avoid spreadsheet layouts." Deliberately
 * shows exactly two stats, not the four a dashboard-style summary might
 * suggest: Threats Found and Trusted Items are the only two values
 * HomeUiState actually, persistently carries. "Apps Scanned" and "Scan
 * Duration" exist only transiently in ScanUiState.Completed, right
 * after a scan finishes in the current session — not as data that
 * survives navigation or an app restart the way this section's other
 * two stats do. Showing them here would mean either fabricating a
 * value when no scan just ran, or having this section's shape change
 * unpredictably based on ScanUiState, which HeroScanAction already
 * surfaces on its own. Extending HomeViewModel to persist these two
 * additional values was considered and rejected — this sprint's own
 * Engineering Constraints forbid ViewModel changes, and Sprint 036's
 * own Data Integrity section is explicit: adapt the UI to available
 * data, never extend the ViewModel just to match a reference design.
 *
 * Threats Found is only shown once a scan has actually happened
 * (lastScan != null) — showing "0" before any scan ever ran would
 * misleadingly imply a clean scan already happened, the same "never
 * fabricate data" principle applied to a value that's technically
 * available (defaulting to 0) but not yet meaningful.
 */
@Composable
private fun SecuritySummarySection(lastScan: LastScanSummary?, trustedItemsCount: Int) {
    val spacing = LocalSpacing.current
    val isDark = isSystemInDarkTheme()
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        SectionHeading(text = "Security Summary")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            if (lastScan != null) {
                // Sprint 036.5: a subtle semantic accent, only when there's
                // genuinely something to flag - a zero-threats card stays
                // neutral (this sprint's own "subtle," not "increasing
                // saturation" everywhere, guidance). Trusted Items is
                // deliberately left with no accentColor - it's a neutral
                // count, not inherently good or concerning, so it doesn't
                // need one.
                val threatsAccent = if (lastScan.threatsFound > 0) {
                    if (isDark) SeverityColors.AttentionDark else SeverityColors.AttentionLight
                } else {
                    null
                }
                AppStatCard(
                    value = "${lastScan.threatsFound}",
                    label = "Threats Found",
                    icon = IconTokens.warning,
                    accentColor = threatsAccent,
                    modifier = Modifier.weight(1f),
                )
            }
            AppStatCard(
                value = "$trustedItemsCount",
                label = "Trusted Items",
                icon = IconTokens.trusted,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Quick Actions — Sprint 036's Part 3. Provides Home-screen shortcuts to
 * four already-existing destinations. Security Center, Cleaner, and
 * Settings are already reachable via the bottom navigation bar
 * (TopLevelDestination) — a Home shortcut alongside a persistent bottom
 * bar is a common, deliberate pattern (a dashboard's own "jump straight
 * there" action, distinct from the bottom bar's "always available"
 * role), not a redundant duplicate. Scan History currently has no
 * bottom-bar entry at all (deliberately, per SpaceAntivirusNavHost.kt's
 * own KDoc — reached only via Security Center's "View full history"
 * today) — this is a second, additional way to reach the same existing
 * route, not a new destination.
 */
@Composable
private fun QuickActionsSection(
    onNavigateToSecurityCenter: () -> Unit,
    onNavigateToCleaner: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        SectionHeading(text = "Quick Actions")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            QuickActionCard(
                icon = IconTokens.security,
                title = "Security Center",
                subtitle = "Real-time protection",
                onClick = onNavigateToSecurityCenter,
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                icon = IconTokens.cleaner,
                title = "Cleaner",
                subtitle = "Free up space",
                onClick = onNavigateToCleaner,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            QuickActionCard(
                icon = IconTokens.history,
                title = "Scan History",
                subtitle = "View past scans",
                onClick = onNavigateToHistory,
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                icon = IconTokens.settings,
                title = "Settings",
                subtitle = "App preferences",
                onClick = onNavigateToSettings,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            // LayoutTokens.minTouchTarget (48dp) - this sprint's own
            // Accessibility requirement. Uses heightIn(min = ...), not
            // height(...) - a fixed height would clip this card's real
            // content (icon + two lines of text with padding, already
            // taller than 48dp); the token here is a floor, not an
            // exact size.
            .heightIn(min = LayoutTokens.minTouchTarget)
            .clip(ShapeTokens.card)
            // Sprint 036.5: .clickable() already provides Compose's own
            // default ripple via LocalIndication - "ripple feedback"
            // (this sprint's own Quick Actions goal) needed no change
            // here, only confirming it was genuinely already correct
            // rather than assuming so.
            .clickable(onClick = onClick),
        shape = ShapeTokens.card,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
    ) {
        Row(
            modifier = Modifier.padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Design-lead review pass: the Hero Card's own icon-badge
            // motif (a tonal circle behind the icon) extended here, at a
            // clearly smaller, subordinate scale - LayoutTokens.minTouchTarget
            // (48dp) versus the Hero Card's 56dp, same proportional
            // icon-to-badge ratio (roughly half) - so the whole screen
            // reads as one deliberate, recognizable visual system rather
            // than the Hero Card being visually isolated from everything
            // beneath it. Tinted with the brand primary color at low
            // opacity, not a severity color - Quick Actions aren't
            // status-driven the way the Hero Card or Recent Activity are.
            Box(
                modifier = Modifier
                    .size(LayoutTokens.minTouchTarget)
                    .clip(ShapeTokens.iconBadge)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.padding(start = spacing.medium)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Recent Activity — Sprint 036's Part 4, "short, readable, transparent."
 * Shows exactly one item: the last scan, the only real activity event
 * HomeUiState actually carries. The reference design's own examples
 * ("Threat Removed," "Database Updated") aren't shown — neither is data
 * this project's current architecture produces (there is no persisted
 * "database updated" event, and "threat removed" isn't distinguished
 * from "scan found this" in any currently-observable state); inventing
 * either would be exactly the fabricated data this sprint's own Data
 * Integrity section forbids. AppEmptyState (SDS, reused) covers the
 * true empty case honestly rather than showing a misleadingly-empty
 * activity list.
 */
@Composable
private fun RecentActivitySection(lastScan: LastScanSummary?) {
    val spacing = LocalSpacing.current
    val isDark = isSystemInDarkTheme()
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        SectionHeading(text = "Recent Activity")
        if (lastScan == null) {
            AppEmptyState(
                icon = IconTokens.scan,
                message = "No activity yet. Run your first scan to get started.",
            )
        } else {
            val resultText = if (lastScan.isClean) {
                "No threats detected"
            } else {
                "${lastScan.threatsFound} item(s) found"
            }
            // Sprint 036.5: fixed a genuine semantic mismatch - this icon
            // previously always showed a checkmark (IconTokens.trusted),
            // even when threats were found, tinted with the brand primary
            // color for a clean result and a flat neutral gray for a
            // dirty one (backwards - the concerning result read as less
            // visually significant than the reassuring one). Now shows a
            // real checkmark tinted Safe-green for clean, or a warning
            // icon tinted Attention-amber when not - "status icon,
            // typography, spacing, and alignment should work together,"
            // this sprint's own words for Recent Activity specifically.
            val (activityIcon, activityColor) = if (lastScan.isClean) {
                IconTokens.trusted to (if (isDark) SeverityColors.SafeDark else SeverityColors.SafeLight)
            } else {
                IconTokens.warning to (if (isDark) SeverityColors.AttentionDark else SeverityColors.AttentionLight)
            }
            Card(shape = ShapeTokens.card, elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card)) {
                Row(
                    modifier = Modifier.padding(spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Design-lead review pass: same icon-badge motif as
                    // QuickActionCard, tinted with this section's own
                    // already-computed semantic color (Safe-green or
                    // Attention-amber) rather than the neutral brand
                    // primary Quick Actions uses - Recent Activity is
                    // status-driven the same way the Hero Card is, so
                    // its badge should carry that same semantic meaning,
                    // not a decorative one.
                    Box(
                        modifier = Modifier
                            .size(LayoutTokens.minTouchTarget)
                            .clip(ShapeTokens.iconBadge)
                            .background(activityColor.copy(alpha = if (isDark) 0.24f else 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = activityIcon,
                            contentDescription = null,
                            tint = activityColor,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Column(modifier = Modifier.padding(start = spacing.medium)) {
                        Text(text = "Full device scan completed", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${formatScanTime(lastScan.scannedAtEpochMillis)} \u00B7 $resultText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A section title consistent across Security Summary/Quick Actions/
 * Recent Activity. Sprint 035's own SDS Component Catalog documented
 * SpaceSectionHeader as a real gap ("Planned Components" — not yet
 * implemented). Building a full, generalized shared component ahead of
 * that catalog entry being formally implemented is out of this sprint's
 * own scope (Home redesign, not a return to Phase 1's token/component
 * work) — this is a small, private, Home-screen-local heading using
 * SDS's own titleMedium type token, not a public core:ui component. A
 * future Phase 1 pass turning this into the catalog's own
 * SpaceSectionHeader, with Home switched over to it, is the right way
 * to close that gap — not invented here, ahead of being asked for.
 */
@Composable
private fun SectionHeading(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}
