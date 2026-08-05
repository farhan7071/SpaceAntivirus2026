package com.space.antivirus.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.space.antivirus.core.model.ProtectionState
import com.space.antivirus.core.model.ScanProgress
import com.space.antivirus.core.ui.component.AppCircularProgress
import com.space.antivirus.core.ui.component.AppEmptyState
import com.space.antivirus.core.ui.component.AppFilledButton
import com.space.antivirus.core.ui.component.AppLinearProgress
import com.space.antivirus.core.ui.component.AppSectionHeader
import com.space.antivirus.core.ui.component.AppTextButton
import com.space.antivirus.core.ui.format.formatBytes
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
/** Sprint 042. Exposed so HomeScreenTest can find the quick toggle —
 *  a Switch has no text of its own to match on. */
const val HOME_PROTECTION_SWITCH_TEST_TAG = "home_protection_switch"

@Composable
fun HomeRoute(
    homeViewModel: HomeViewModel = hiltViewModel(),
    scanViewModel: ScanViewModel = hiltViewModel(),
    onNavigateToSecurityCenter: () -> Unit = {},
    onNavigateToCleaner: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    /**
     * Sprint 044. Fires after the scan result has been dismissed, never
     * before — the app's only interstitial moment. Defaulted to a no-op
     * so this screen has no ads dependency of its own.
     */
    onScanResultAcknowledged: () -> Unit = {},
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val scanState by scanViewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        scanState = scanState,
        onScanClick = scanViewModel::startScan,
        onAcknowledgeScanResult = {
            // Order matters: the result is cleared first, so the ad
            // opens over a screen the user has finished with rather than
            // over the finding they just asked to see.
            scanViewModel.acknowledgeResult()
            onScanResultAcknowledged()
        },
        onProtectionToggled = homeViewModel::onProtectionToggled,
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
    onProtectionToggled: (Boolean) -> Unit = {},
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
            onProtectionToggled = onProtectionToggled,
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
    onProtectionToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutTokens.screenHorizontalPadding),
        // Sprint 037 (Design Review #4, "too much vertical spacing...
        // reduce gaps between sections... 8-12dp smaller"): spacing.large
        // (24dp) -> spacing.medium (16dp), an 8dp reduction across every
        // gap between Hero Card/Security Summary/Quick Actions/Recent
        // Activity, matching the requested range exactly.
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
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
            state.protection?.let { protection ->
                ProtectionSection(protection = protection, onProtectionToggled = onProtectionToggled)
            }

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
            RecentActivitySection(
                lastScan = state.lastScanSummary,
                lastCleanup = state.lastCleanupSummary,
            )
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
 * Sprint 037 (design review round 2, first-principles rebuild): the
 * headline now uses Type.kt's own displayLarge style — reserved,
 * per that file's own KDoc, for exactly this "status headline" moment,
 * but never actually applied here until this round. Two smaller
 * icon-badge treatments were tried and removed across two earlier
 * rounds of polish (Sprint 036.5's 56dp badge beside the headline,
 * then a shrunk 44dp version) — at displayLarge's scale, a competing
 * icon element beside it read as clutter rather than reinforcement, so
 * this round removed it, keeping only the small, accessible icon in
 * the status-label row above the headline. Elevation stays
 * Elevation.floating — more raised than the plain stat/action cards
 * beneath it, "the Hero Card is now the product's visual identity."
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
    // Sprint 037 (Design Review #5, "Unknown State needs personality"):
    // "Protection status unknown" read as clinical/error-like rather than
    // a first-run welcome - the same underlying UNKNOWN state, but copy
    // that reads as onboarding ("let's get started") rather than "we
    // don't know what's going on."
    val (headline, supportingText) = when (status) {
        ProtectionStatus.PROTECTED ->
            "You're protected" to "No threats found in your last scan"
        ProtectionStatus.NEEDS_ATTENTION ->
            "Attention needed" to
                "${lastScan?.threatsFound ?: 0} item(s) from your last scan are worth reviewing"
        ProtectionStatus.UNKNOWN ->
            "Let's get you protected" to "Run your first scan to see how your device is doing"
    }
    val statusLabel = when (status) {
        ProtectionStatus.PROTECTED -> "SECURE"
        ProtectionStatus.NEEDS_ATTENTION -> "ATTENTION"
        ProtectionStatus.UNKNOWN -> "GET STARTED"
    }
    // Sprint 037 (Design Review #1, "move Dismiss to top-right as text or
    // icon, not a large element"): Dismiss only has real meaning while
    // there's an actual just-completed/failed scan result to acknowledge
    // - onAcknowledgeScanResult, unchanged, still only does anything
    // meaningful in those two states. Moving it to the top row is a pure
    // repositioning of existing, real behavior, not new functionality.
    val showDismiss = scanState is ScanUiState.Completed || scanState is ScanUiState.Error

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeTokens.heroCard,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.floating),
        colors = CardDefaults.cardColors(containerColor = backgroundTint),
    ) {
        Column(
            // Sprint 037 (Design Review #1, "reduce height by ~20-25%"):
            // outer padding tightened from spacing.large (24dp) to
            // spacing.medium (16dp), and the internal gap between groups
            // from spacing.medium to spacing.small - a real, measured
            // reduction, not a cosmetic tweak, on top of removing the
            // large 56dp badge-beside-headline layout below.
            // Sprint 046: 16dp -> 20dp, and the internal rhythm from
            // 8dp to 12dp. Sprint 037 tightened both to cut the card's
            // height by a measured 20-25%, which was the right call
            // against a card carrying three redundant text lines and a
            // 56dp badge. With that redundancy gone the constraint goes
            // with it, and the space is better spent on breathing room.
            modifier = Modifier.padding(HERO_PADDING),
            verticalArrangement = Arrangement.spacedBy(spacing.standard),
        ) {
            // Small, inline icon + status label row, matching both
            // reference images' actual layout (neither shows a large
            // icon badge beside the headline - that was this project's
            // own Sprint 036.5 addition, reconsidered here against a
            // clearer reference). Dismiss sits at the far right of this
            // same row - "top-right, as text or icon, not a large
            // element" - only shown while there's a real result to
            // dismiss.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Sprint 046: the same icon + label, now on a filled
                // pill. As loose glyph-plus-text it read as a caption
                // floating above the headline; contained, it reads as a
                // status the card is reporting. Uses the status colour
                // it already had at low alpha, so nothing new enters the
                // palette.
                Row(
                    modifier = Modifier
                        .clip(ShapeTokens.chip)
                        .background(statusColor.copy(alpha = if (isDark) 0.22f else 0.16f))
                        .padding(horizontal = spacing.small, vertical = spacing.tight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(HERO_STATUS_ICON_SIZE),
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = spacing.tight),
                    )
                }
                if (showDismiss) {
                    AppTextButton(text = "Dismiss", onClick = onAcknowledgeScanResult)
                }
            }

            // Sprint 037 (design review round 2, "redesign the Hero Card
            // from first principles... avoid making it feel like:
            // Material Card / Icon / Button. Instead it should feel
            // like a product identity"): a real, significant finding
            // made while re-reviewing this card against that
            // instruction - Type.kt's own KDoc (Sprint 035) already
            // says displayLarge is "reserved for exactly the two hero
            // moments (status headline, scan-complete moment)." This
            // exact headline is that status headline, and it had never
            // actually used displayLarge - headlineSmall (24sp) was
            // used instead throughout every prior round of polish. The
            // design system's own typography scale already had the
            // right answer for "unmistakably the primary focus...
            // premium typography" before this sprint asked the
            // question; it just hadn't been applied here yet.
            //
            // The separate restrained icon badge (44dp, prior round) is
            // removed here, not shrunk further - at this new headline
            // scale, a second icon element beside it read as competing
            // clutter rather than reinforcement. The small icon in the
            // status-label row above already carries the icon role,
            // accessibly, without an additional decorative element.
            // Sprint 046: displayLarge (45sp) down to headlineLarge
            // (32sp). Sprint 037 applied displayLarge because Type.kt
            // reserves it for this exact headline, and on paper that was
            // right. On a real device it wraps "Attention needed" onto
            // two lines and takes roughly a third of the card, which
            // reads as shouting rather than as hierarchy. The card is
            // still unmistakably the hero — it is the largest type on
            // the screen by a wide margin — it just no longer has to
            // wrap to prove it.
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = spacing.tight),
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Sprint 046: one metadata row replaces two stacked lines.
            // The card previously said the same thing three times — the
            // supporting line, a "Last scan: ..." line, and the result
            // banner below all reported the same completed scan and the
            // same count. Repetition is the main reason the card felt
            // heavy. Timestamp and outcome now sit together on one quiet
            // row; the count stays in the supporting line above, where
            // it is the actual message.
            HeroMetadataRow(lastScan = lastScan, scanState = scanState)

            HeroScanAction(
                scanState = scanState,
                onScanClick = onScanClick,
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
/**
 * Sprint 046 — one quiet line carrying when the last scan ran and how it
 * ended.
 *
 * Replaces two stacked lines that between them repeated what the
 * headline and supporting text had already said. Renders nothing at all
 * before a first scan rather than showing a placeholder, and the outcome
 * half is omitted while a scan is running, since there is no outcome yet.
 */
@Composable
private fun HeroMetadataRow(lastScan: LastScanSummary?, scanState: ScanUiState) {
    val spacing = LocalSpacing.current
    if (lastScan == null) return

    val outcome = when {
        scanState is ScanUiState.Running -> null
        scanState is ScanUiState.Completed && scanState.isClean -> "No threats found"
        scanState is ScanUiState.Completed -> "Scan complete"
        else -> null
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = IconTokens.history,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(HERO_METADATA_ICON_SIZE),
        )
        Text(
            text = listOfNotNull(formatScanTime(lastScan.scannedAtEpochMillis), outcome)
                .joinToString("  \u00B7  "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = spacing.small),
        )
    }
}

@Composable
private fun HeroScanAction(
    scanState: ScanUiState,
    onScanClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        // Sprint 046: the Completed banner is gone. It restated the
        // headline and the supporting line — three renderings of one
        // fact stacked vertically — and was the single biggest source of
        // the card's bulk. The outcome now appears once, in the metadata
        // row. Errors keep their banner: an error is genuinely new
        // information the rest of the card does not carry.
        if (scanState is ScanUiState.Error) {
            HeroResultBanner(message = scanState.message)
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

/**
 * Sprint 037 (Design Review #1): no longer owns its own Dismiss button -
 * that action moved to the Hero Card's top-right row, so this is now
 * pure message text, letting the result read as one paragraph rather
 * than a message + a second, separate action floating beneath it.
 */
@Composable
private fun HeroResultBanner(message: String) {
    Text(text = message, style = MaterialTheme.typography.bodySmall)
}

/**
 * Sprint 037 (Design Review #6, "replace the generic progress indicator
 * with meaningful scan stages"). ScanProgress (core:model) carries only
 * itemsProcessed/totalItems/threatsFoundSoFar - a linear count, not a
 * phase or stage field of any kind (verified directly before writing
 * this, not assumed). Literally implementing named technical phases
 * ("Checking installed apps\u2026 Analyzing permissions\u2026 Scanning
 * app signatures\u2026") would mean claiming the scan engine is doing a
 * specific thing at a specific moment that this presentation-layer code
 * has no way to actually verify - exactly the kind of fabricated detail
 * this project has consistently avoided (e.g. Sprint 036 declining to
 * invent "Apps Scanned"/"Scan Duration" as persistent Home stats).
 * Modifying the scan engine to genuinely track and report phases would
 * be a business-logic change, explicitly out of this sprint's own
 * scope.
 *
 * Instead: three honest, real milestones derived directly from the
 * fraction of items actually processed - "Starting", "Scanning" (with
 * the real, current N-of-M count), and "Almost done" past the halfway
 * point. Genuinely varies as the scan progresses, reads as considered
 * rather than a bare technical counter, and every word is something
 * this code can actually verify from the data it has.
 * threatsFoundSoFar is deliberately never surfaced mid-scan - showing a
 * partial, still-accumulating threat count before CumulativeRiskScorer
 * has evaluated the complete picture risks reading as alarming ahead of
 * time, the same "never exaggerate risk" discipline the detection
 * engine itself already follows (ADR 0015).
 */
private fun scanStageMessage(progress: ScanProgress?): String {
    if (progress == null || progress.totalItems == 0) return "Starting scan\u2026"
    val fraction = progress.itemsProcessed.toFloat() / progress.totalItems.toFloat()
    return when {
        fraction >= 1f -> "Finishing up\u2026"
        fraction >= 0.5f -> "Almost done \u2014 ${progress.itemsProcessed} of ${progress.totalItems} apps checked"
        else -> "Scanning your apps \u2014 ${progress.itemsProcessed} of ${progress.totalItems} checked"
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
        Text(text = scanStageMessage(progress), style = MaterialTheme.typography.labelMedium)
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
 *
 * Sprint 037 (Design Review #2, "feels like two unrelated cards... make
 * it feel like one dashboard"): one shared Card with a vertical divider
 * between the two stats, rather than two cards that happen to sit next
 * to each other.
 *
 * Sprint 037 (design review round 2, "do not create reusable components
 * unless two or more screens genuinely need them today"): this was
 * briefly extracted to a public core:ui component (AppStatGroup) in the
 * prior round, justified by "a future sprint touching Security Center
 * might reasonably adopt this too" — that's hypothetical reuse, not a
 * genuine need today (verified directly: nothing else in this codebase
 * calls it). Folded back into this file as simple, local presentation
 * code per this sprint's own explicit correction — a private SecurityStat
 * composable below, not a shared component or a generic StatGroupItem
 * data class, since there's exactly one caller and inventing that
 * indirection for it isn't warranted.
 */
/**
 * Sprint 042 — live background-protection status with a quick toggle.
 *
 * **The copy here is deliberately not the brief's.** The sprint brief
 * suggested "Real-time protection active". This app has no real-time
 * protection: it runs scheduled scans, and live file scanning, APK
 * interception, accessibility monitoring and install interception are
 * all explicitly out of scope and absent from the project. Claiming
 * real-time protection on the home screen of a security app would be the
 * most consequential false claim this project could make, and ADR 0015's
 * "never exaggerate" rule does not stop applying because the sentence is
 * reassuring rather than alarming. It says what is true instead.
 *
 * The next-scan line is worded as approximate for the same reason:
 * WorkManager decides when periodic work actually fires and defers it
 * for this project's battery and storage constraints, so an exact time
 * would state a guarantee the platform does not make.
 *
 * Built from the same Card + tonal icon badge + Switch pieces Settings
 * and the rest of Home already use — no new visual language.
 */
@Composable
private fun ProtectionSection(protection: ProtectionState, onProtectionToggled: (Boolean) -> Unit) {
    val spacing = LocalSpacing.current
    val isDark = isSystemInDarkTheme()
    val accent = if (protection.isEnabled) {
        if (isDark) SeverityColors.SafeDark else SeverityColors.SafeLight
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        AppSectionHeader(title = "Background Protection")
        Card(shape = ShapeTokens.card, elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Sprint 046: 16dp -> 20dp vertical. The row was
                    // cramped chiefly because a 48dp badge and two lines
                    // of text sat inside 16dp of padding, leaving the
                    // text touching the card edges.
                    .padding(horizontal = spacing.medium, vertical = HERO_PADDING),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(LayoutTokens.minTouchTarget)
                        .clip(ShapeTokens.iconBadge)
                        .background(accent.copy(alpha = if (isDark) 0.24f else 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = IconTokens.security,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = spacing.medium),
                ) {
                    Text(
                        text = if (protection.isEnabled) "Protection enabled" else "Protection disabled",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    protectionSupportingLines(protection).forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = protection.isEnabled,
                    onCheckedChange = onProtectionToggled,
                    modifier = Modifier.testTag(HOME_PROTECTION_SWITCH_TEST_TAG),
                )
            }
        }
    }
}

/**
 * "Around", not "at": the next-scan time is an estimate derived from
 * when the periodic work was enqueued, and WorkManager may defer it.
 * Falls back to saying less when there is no timestamp to derive from,
 * rather than inventing one.
 */
/**
 * Sprint 046: two lines instead of one.
 *
 * As a single dot-joined string this wrapped wherever the row happened
 * to run out of width — on a real device, mid-phrase, after "next scan"
 * — which is the kind of ragged break that makes an otherwise fine card
 * look unfinished. Split at the natural boundary, each half sits on its
 * own line and the card gains a predictable height.
 */
private fun protectionSupportingLines(protection: ProtectionState): List<String> = when {
    !protection.isEnabled -> listOf("Tap to enable automatic scans")
    protection.earliestNextScanEpochMillis != null -> listOf(
        "Automatic scans on",
        "Next scan around " +
            DateFormat.getTimeInstance(DateFormat.SHORT)
                .format(Date(protection.earliestNextScanEpochMillis!!)),
    )
    else -> listOf("Automatic scans on")
}

@Composable
private fun SecuritySummarySection(lastScan: LastScanSummary?, trustedItemsCount: Int) {
    val spacing = LocalSpacing.current
    val isDark = isSystemInDarkTheme()
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        AppSectionHeader(title = "Security Summary")
        Card(shape = ShapeTokens.card, elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card)) {
            // Sprint 046: the divider between these two metrics has been
            // in the code since Sprint 037 and has never been visible on
            // screen. VerticalDivider uses fillMaxHeight(), and inside a
            // Row with no height constraint that resolves to zero — the
            // divider was rendering at 0dp tall. IntrinsicSize.Min makes
            // the Row adopt its tallest child's height, which is what
            // fillMaxHeight() needs to measure against. Vertical padding
            // also goes 16dp -> 20dp so the two columns get the
            // breathing room the divider is meant to organise.
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .padding(vertical = HERO_PADDING),
            ) {
                if (lastScan != null) {
                    // Sprint 036.5: a subtle semantic accent, only when
                    // there's genuinely something to flag - a
                    // zero-threats card stays neutral. Trusted Items is
                    // deliberately left with no accentColor - it's a
                    // neutral count, not inherently good or concerning.
                    val threatsAccent = if (lastScan.threatsFound > 0) {
                        if (isDark) SeverityColors.AttentionDark else SeverityColors.AttentionLight
                    } else {
                        null
                    }
                    SecurityStat(
                        value = "${lastScan.threatsFound}",
                        label = "Threats Found",
                        icon = IconTokens.warning,
                        accentColor = threatsAccent,
                        modifier = Modifier.weight(1f),
                    )
                    VerticalDivider(modifier = Modifier.fillMaxHeight())
                }
                SecurityStat(
                    value = "$trustedItemsCount",
                    label = "Trusted Items",
                    icon = IconTokens.trusted,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SecurityStat(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
) {
    val spacing = LocalSpacing.current
    val valueColor = accentColor ?: MaterialTheme.colorScheme.onSurface
    val iconColor = accentColor ?: MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier.padding(horizontal = spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(18.dp)
                .padding(bottom = spacing.tight),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = valueColor,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        AppSectionHeader(title = "Quick Actions")
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
            // Accessibility requirement, unchanged even as the visible
            // content inside shrinks (Design Review #3) - the touch
            // target and the icon badge's own visual size are two
            // different things; the card's own heightIn floor keeps the
            // former accessible regardless of the latter getting more
            // compact.
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
            // Sprint 046: 8dp -> 14dp. Sprint 037 tightened this to 8dp
            // to increase information density, which was the right
            // trade against a screen whose hero was consuming a third of
            // the viewport. With the hero reduced, these four cards are
            // the most-tapped controls on the screen and were the
            // tightest thing on it — 8dp put text almost against the
            // card edge. The 48dp touch-target floor below is unchanged;
            // this is about how the card reads, not how it is hit.
            modifier = Modifier.padding(QUICK_ACTION_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Sprint 037 (Design Review #3, "reduce icon badge size"):
            // shrunk from LayoutTokens.minTouchTarget (48dp) to 36dp -
            // still the same tonal-circle motif tying this card to the
            // Hero Card's own visual language, just sized to read as a
            // dashboard shortcut rather than a large feature tile.
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(ShapeTokens.iconBadge)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
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
 *
 * Sprint 037 (prose "Recent Activity" section, "make it look like a
 * timeline or activity feed, not just another card"): a literal
 * timeline connector line was considered and rejected here specifically
 * - this section shows exactly one real event (the same data
 * constraint as above), and a connector line joining a single item to
 * nothing would be decorative rather than meaningful, the same
 * "restraint" discipline this project has applied consistently.
 * Instead, stronger internal hierarchy: the event title is now bolder,
 * and the timestamp has its own distinct, smaller/quieter caption
 * styling (labelSmall) separated from the result text (bodySmall,
 * semantically colored) rather than both run together in one
 * undifferentiated line - "better timestamp styling" without inventing
 * data this section doesn't have. Empty-state copy also warmed (Design
 * Review #5's reasoning applies equally here, not only to the Hero
 * Card's own UNKNOWN branch).
 */
@Composable
private fun RecentActivitySection(lastScan: LastScanSummary?, lastCleanup: LastCleanupSummary?) {
    val spacing = LocalSpacing.current
    val isDark = isSystemInDarkTheme()
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        AppSectionHeader(title = "Recent Activity")
        if (lastScan == null && lastCleanup == null) {
            AppEmptyState(
                icon = IconTokens.scan,
                message = "Nothing to show yet \u2014 run your first scan and we'll keep you posted here.",
            )
        }
        if (lastScan != null) {
            val resultText = if (lastScan.isClean) {
                "No threats detected"
            } else {
                "${lastScan.threatsFound} item(s) found"
            }
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
                    // Sprint 046: three stacked lines become two. The
                    // result and the timestamp are one fact about one
                    // event, and stacking them made a single activity
                    // entry as tall as the two-metric summary card above
                    // it. On one row with a separator the entry reads as
                    // a feed item rather than a paragraph. The result
                    // keeps its accent colour; only the timestamp is
                    // demoted, which is the correct hierarchy — the
                    // count is the news, the time is context.
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = spacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Full device scan completed",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = spacing.tight),
                            ) {
                                Text(
                                    text = resultText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = activityColor,
                                )
                                Text(
                                    text = "  \u00B7  " +
                                        formatScanTime(lastScan.scannedAtEpochMillis),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (lastCleanup != null) {
            LastCleanupCard(lastCleanup)
        }
    }
}

/**
 * Sprint 040. The Cleaner has persisted real cleanup records since
 * Sprint 039, but nothing read them back — Recent Activity showed only
 * scans, so a user who had just freed 480 MB saw no trace of it here.
 *
 * Built as a second card in the existing Recent Activity section, using
 * the same Card/icon-badge/three-line anatomy as the scan card directly
 * above it. Home's visual design has been locked since Sprint 037; this
 * adds real data to an existing section rather than introducing anything
 * new to look at.
 *
 * Every figure comes from a persisted `CleanupRecord`. The row is absent
 * entirely until the user has actually run a cleanup — never a
 * placeholder "0 B" or "Never".
 */
@Composable
private fun LastCleanupCard(lastCleanup: LastCleanupSummary) {
    val spacing = LocalSpacing.current
    val isDark = isSystemInDarkTheme()
    val accent = if (isDark) SeverityColors.SafeDark else SeverityColors.SafeLight
    Card(shape = ShapeTokens.card, elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card)) {
        Row(
            modifier = Modifier.padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(LayoutTokens.minTouchTarget)
                    .clip(ShapeTokens.iconBadge)
                    .background(accent.copy(alpha = if (isDark) 0.24f else 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = IconTokens.cleaner,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.padding(start = spacing.medium)) {
                Text(
                    // A cancelled cleanup really did free the bytes it
                    // reports, so it belongs here — but it says it was
                    // stopped rather than implying it ran to completion.
                    text = if (lastCleanup.wasCancelled) "Cleanup stopped early" else "Junk cleanup completed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${formatBytes(lastCleanup.bytesFreed)} freed \u00B7 " +
                        "${lastCleanup.itemsDeleted} file(s) removed",
                    style = MaterialTheme.typography.bodySmall,
                    color = accent,
                    modifier = Modifier.padding(top = spacing.tight),
                )
                Text(
                    text = formatScanTime(lastCleanup.cleanedAtEpochMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Sprint 046 hero dimensions. Local constants, same convention as the
// screen's other icon sizes — the SDS has no token layer for these.
private val HERO_PADDING = 20.dp
private val HERO_STATUS_ICON_SIZE = 16.dp
private val HERO_METADATA_ICON_SIZE = 14.dp
private val QUICK_ACTION_PADDING = 14.dp
