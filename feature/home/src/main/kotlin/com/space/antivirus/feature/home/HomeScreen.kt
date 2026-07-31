package com.space.antivirus.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
        shape = ShapeTokens.card,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
        colors = CardDefaults.cardColors(containerColor = backgroundTint),
    ) {
        Column(
            modifier = Modifier.padding(spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            // Small status-label row ("SECURE"/"ATTENTION"/"UNKNOWN") -
            // deliberately plain text + icon, not StatusChip: StatusChip
            // is scoped to Severity's three per-finding tiers (core:ui),
            // and ProtectionStatus is a different, three-value enum
            // measuring the same underlying signal at the whole-device
            // level - reusing StatusChip here would mean either an
            // incorrect Severity mapping or bypassing its own type
            // safety with a workaround, neither of which is genuine
            // reuse.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = spacing.tight),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = headline, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(LayoutTokens.minTouchTarget),
                )
            }

            if (lastScan != null) {
                Text(
                    text = "Last scan: ${formatScanTime(lastScan.scannedAtEpochMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(spacing.tight))

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
            AppFilledButton(
                text = "Scan Now",
                onClick = onScanClick,
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
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
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        SectionHeading(text = "Security Summary")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            if (lastScan != null) {
                AppStatCard(
                    value = "${lastScan.threatsFound}",
                    label = "Threats Found",
                    icon = IconTokens.warning,
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
            .clickable(onClick = onClick),
        shape = ShapeTokens.card,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
    ) {
        Row(
            modifier = Modifier.padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.padding(start = spacing.small)) {
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
            Card(shape = ShapeTokens.card, elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card)) {
                Row(
                    modifier = Modifier.padding(spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = IconTokens.trusted,
                        contentDescription = null,
                        tint = if (lastScan.isClean) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp),
                    )
                    Column(modifier = Modifier.padding(start = spacing.small)) {
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
