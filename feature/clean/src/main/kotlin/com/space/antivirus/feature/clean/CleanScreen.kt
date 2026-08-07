package com.space.antivirus.feature.clean

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.space.antivirus.core.designsystem.brand.heroBackdrop
import com.space.antivirus.core.designsystem.theme.Elevation
import com.space.antivirus.core.designsystem.theme.IconTokens
import com.space.antivirus.core.designsystem.theme.LayoutTokens
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.designsystem.theme.ShapeTokens
import com.space.antivirus.core.model.CleanableCategory
import com.space.antivirus.core.model.CleanableItem
import com.space.antivirus.core.model.CleanupRecord
import com.space.antivirus.core.model.JunkScanProgress
import com.space.antivirus.core.model.StorageStatistics
import com.space.antivirus.core.ui.component.AppCircularProgress
import com.space.antivirus.core.ui.component.AppEmptyState
import com.space.antivirus.core.ui.component.AppFilledButton
import com.space.antivirus.core.ui.component.AppLinearProgress
import com.space.antivirus.core.ui.component.AppOutlinedButton
import com.space.antivirus.core.ui.component.AppSectionHeader
import com.space.antivirus.core.ui.format.formatBytes

/** Exposed so CleanScreenTest can find the scanning indicator — the
 *  Scanning state still has no determinate value to assert on, by
 *  design (see `JunkScanProgress`). */
const val CLEAN_SCANNING_TEST_TAG = "clean_scanning_indicator"

/** The cleaning state's indicator, which unlike scanning IS
 *  determinate — the candidate count is known before deletion starts. */
const val CLEAN_CLEANING_TEST_TAG = "clean_cleaning_indicator"

/**
 * The Junk Cleaner screen.
 *
 * **Sprint 038** built these layouts against a domain layer that could
 * only scan. Its central design constraint — that nothing in this
 * project deleted a file — was true then and is documented at length in
 * ADR 0053. **Sprint 039 made it false**, deliberately and in that
 * order: the UI was built honest first, and the engine was built to
 * match it rather than the UI being built to flatter an engine that did
 * not exist.
 *
 * What changed here as a result is narrow, and only where a real
 * capability replaced a documented omission:
 * - Scanning shows real inspected/found counts and a real Cancel, both
 *   streamed from `ScanForJunkFilesUseCase`.
 * - Results has a real Clean action, because `CleanJunkFilesUseCase` now
 *   genuinely deletes.
 * - Cleaning and Completed exist at all, on real measured progress.
 * - Idle shows a real "Last cleanup" and real storage totals.
 *
 * The layouts, spacing, typography and colour of the four Sprint 038
 * states are untouched. Cleaning and Completed are assembled from the
 * same local building blocks (`FeatureHeader`, `InfoCard`, `IconBadge`)
 * rather than introducing any new visual language.
 *
 * Everything on screen is derived from `CleanUiState` and nothing else.
 * The scanning state still shows no percentage: a filesystem walk does
 * not know its own total (see `JunkScanProgress`). The cleaning state
 * does show one, because there the total is genuinely known.
 */
@Composable
fun CleanRoute(
    viewModel: CleanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CleanScreen(
        uiState = uiState,
        onScanClick = viewModel::scanForJunk,
        onCancelScanClick = viewModel::cancelScan,
        onCleanClick = viewModel::cleanJunk,
        onCancelCleanClick = viewModel::cancelClean,
        onDoneClick = viewModel::dismissCompletion,
    )
}

@Composable
fun CleanScreen(
    uiState: CleanUiState,
    onScanClick: () -> Unit,
    onCancelScanClick: () -> Unit = {},
    onCleanClick: () -> Unit = {},
    onCancelCleanClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is CleanUiState.Idle -> CleanIdle(uiState, onScanClick, modifier)
        is CleanUiState.Scanning -> CleanScanning(uiState, onCancelScanClick, modifier)
        is CleanUiState.Loaded ->
            if (uiState.items.isEmpty()) {
                CleanNothingFound(onScanClick, modifier)
            } else {
                CleanResults(uiState, onScanClick, onCleanClick, modifier)
            }
        is CleanUiState.Cleaning -> CleanCleaning(uiState, onCancelCleanClick, modifier)
        is CleanUiState.Completed -> CleanCompleted(uiState, onScanClick, onDoneClick, modifier)
        is CleanUiState.Error -> CleanError(uiState, onScanClick, modifier)
    }
}

// ---------------------------------------------------------------------
// State 1 — Idle
// ---------------------------------------------------------------------

/**
 * The pre-scan screen.
 *
 * Sprint 038 omitted the reference design's storage line and "Last
 * cleanup" row because neither had any data behind it. Sprint 039 built
 * both capabilities, so both now appear — with real values, and only
 * when those values actually loaded. A null stays absent rather than
 * becoming a zero or a placeholder.
 *
 * The four capability rows are not decorative copy — each one names a
 * rule `JunkFileClassifier` (`domain/cleaning`) genuinely implements,
 * and the set matches `CleanableCategory`'s four real values exactly.
 * "Empty Folders", shown in the reference, is not among them: the
 * classifier only ever classifies files, never directories, so a row
 * promising it would describe behavior that does not exist.
 */
@Composable
private fun CleanIdle(state: CleanUiState.Idle, onScanClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutTokens.screenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        contentPadding = PaddingValues(vertical = spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            FeatureHeader(
                icon = IconTokens.cleaner,
                title = "Junk Cleaner",
                description = "Find cache, temporary and log files your device no longer needs, " +
                    "and see how much space they're using.",
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeTokens.card,
                elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
            ) {
                Column(modifier = Modifier.padding(vertical = spacing.small)) {
                    CleanableCategory.entries.forEachIndexed { index, category ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.medium))
                        }
                        CapabilityRow(
                            icon = category.icon(),
                            title = category.displayLabel(),
                            description = category.capabilityDescription(),
                        )
                    }
                }
            }
        }
        item { SafeCleanupCard() }
        item {
            AppFilledButton(
                text = "Scan for Junk Files",
                onClick = onScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LayoutTokens.primaryActionHeight),
            )
        }
        state.storage?.let { storage -> item { StorageLine(storage) } }
        state.lastCleanup?.let { record -> item { LastCleanupLine(record) } }
    }
}

// ---------------------------------------------------------------------
// State 2 — Scanning
// ---------------------------------------------------------------------

/**
 * The in-progress scan, still built on an **indeterminate** indicator —
 * and that is not a leftover from Sprint 038's constraints.
 *
 * Sprint 039 gave this screen genuinely real progress: `filesInspected`,
 * `junkFound` and `bytesFound` are live counters streamed out of
 * `ScanForJunkFilesUseCase` as the walk visits each file. What it did
 * NOT give it is a percentage, because a filesystem walk does not know
 * how many files it will visit until it has visited them. Producing one
 * would need either a full counting pre-pass (doubling the I/O purely to
 * animate a bar) or an invented denominator. See `JunkScanProgress`.
 *
 * `Cancel Scan` is real now. `enumerateFilesAsFlow` checks for
 * cancellation between files precisely so a blocking tree walk has
 * somewhere to actually stop; Sprint 038 showed no such button because
 * nothing underneath could have honoured it.
 *
 * The current path is shown as the file's name only, never its full
 * path — the reference design's `com.whatsapp/cache/thumbs` is another
 * app's private directory, which this app cannot read on any modern
 * Android, and a real path here would mostly expose this app's own
 * internals to no benefit.
 */
@Composable
private fun CleanScanning(
    state: CleanUiState.Scanning,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutTokens.screenHorizontalPadding)
            .padding(vertical = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(SCANNING_RING_SIZE),
            contentAlignment = Alignment.Center,
        ) {
            AppCircularProgress(
                progress = null,
                modifier = Modifier
                    .size(SCANNING_RING_SIZE)
                    .testTag(CLEAN_SCANNING_TEST_TAG),
            )
            Icon(
                imageVector = IconTokens.cleaner,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(HERO_ICON_SIZE),
            )
        }
        Text(
            text = "Scanning for junk files\u2026",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = state.progress.currentItemLabel(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        // Real counters, streamed per file. No total, so no percentage.
        ScanCounters(state.progress)
        InfoCard(
            icon = IconTokens.scan,
            title = "What we're checking",
            body = "Your app files and cache: files with common temporary extensions, log files, " +
                "and app installers left behind.",
        )
        InfoCard(
            icon = IconTokens.security,
            title = "Read-only",
            body = "Scanning only reads file information. Nothing is opened, changed or deleted.",
        )
        AppOutlinedButton(
            text = "Cancel Scan",
            onClick = onCancelClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(LayoutTokens.primaryActionHeight),
        )
    }
}

/**
 * The three live scan counters. Uses the same three-across row the
 * Sprint 038 design language already established, and shows only values
 * `JunkScanProgress` genuinely carries.
 */
@Composable
private fun ScanCounters(progress: JunkScanProgress) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        CounterCell(label = "Checked", value = progress.filesInspected.toString(), modifier = Modifier.weight(1f))
        CounterCell(label = "Junk found", value = progress.junkFound.toString(), modifier = Modifier.weight(1f))
        CounterCell(
            label = "Reclaimable",
            value = formatSize(progress.bytesFound),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CounterCell(label: String, value: String, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    Card(
        modifier = modifier,
        shape = ShapeTokens.card,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------
// State 3 — Results
// ---------------------------------------------------------------------

/**
 * The results screen.
 *
 * **The Clean button is real as of Sprint 039.** Sprint 038 shipped this
 * screen with no clean action at all and a hero that said plainly that
 * nothing had been deleted, because nothing could be. `CleanJunkFilesUseCase`
 * now genuinely deletes, so the action exists and the disclaimer is
 * replaced by an accurate description of what pressing it will do. The
 * button names the amount it will free, from the real measured total.
 *
 * One deliberate departure from the reference image remains, documented
 * in ADR 0053 and unchanged:
 *
 * 1. **No alarm-red hero.** The reference tints "Junk found" in the same
 *    red this app reserves for `ACTION_NEEDED` security findings.
 *    `CleanableCategory`'s own KDoc is explicit that a cache file is not
 *    a security concern and that conflating reclaimable storage with a
 *    threat misrepresents what the finding means. The hero uses the
 *    brand's own primary tonal wash instead — the same treatment Home's
 *    Hero Card uses for a neutral, non-alarming state.
 *
 * Per-category totals, percentages and counts are all derived from the
 * real item list. Only categories that actually appear in the results
 * are rendered; the four-row fixed breakdown in the reference would
 * otherwise show invented zero rows.
 */
@Composable
private fun CleanResults(
    state: CleanUiState.Loaded,
    onScanClick: () -> Unit,
    onCleanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val groups = remember(state.items) { state.items.groupIntoCategories() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutTokens.screenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        contentPadding = PaddingValues(vertical = spacing.medium),
    ) {
        item {
            ResultsHeroCard(
                totalSizeBytes = state.totalSizeBytes,
                itemCount = state.items.size,
            )
        }
        // Sprint 046.2: the section header sits on the same 16dp
        // rhythm as every card gap, so the hero and the breakdown read
        // as one continuous stack. A larger gap above the header is what
        // makes them two sections.
        item {
            AppSectionHeader(
                title = "Junk breakdown",
                modifier = Modifier.padding(top = spacing.small),
            )
        }
        items(groups.size) { index ->
            val group = groups[index]
            JunkCategoryCard(group = group, totalSizeBytes = state.totalSizeBytes)
        }
        item {
            AppFilledButton(
                text = "Clean ${formatSize(state.totalSizeBytes)}",
                onClick = onCleanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LayoutTokens.primaryActionHeight),
            )
        }
        item {
            AppOutlinedButton(
                text = "Scan Again",
                onClick = onScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LayoutTokens.primaryActionHeight),
            )
        }
        state.storage?.let { storage -> item { StorageLine(storage) } }
    }
}

@Composable
private fun ResultsHeroCard(totalSizeBytes: Long, itemCount: Int) {
    val spacing = LocalSpacing.current
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeTokens.heroCard,
        // Sprint 046.2: Elevation.floating down to Elevation.card. At
        // floating, in light theme, the shadow rendered as a thick grey
        // ring around the card — a frame drawn around a frame, which is
        // what made this read as a text container rather than a
        // dashboard panel. The tint below now does the separating.
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
        // Sprint 050: plain surface, with the tint moved into
        // heroBackdrop() as a gradient — the same treatment as Home's
        // hero, so the two read as one surface across screens.
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .heroBackdrop()
                .padding(CLEAN_HERO_PADDING),
            // Sprint 046.2: no uniform arrangement any more. Even spacing
            // between four elements is exactly what made them feel
            // equally important. Each gap below is now set individually
            // to say how tightly that element belongs to the one above.
            verticalArrangement = Arrangement.Top,
        ) {
            // 1. Badge — a contained pill, matching Home's hero, rather
            //    than a loose icon and caption floating above the number.
            Row(
                modifier = Modifier
                    .clip(ShapeTokens.chip)
                    .background(
                        MaterialTheme.colorScheme.primary
                            .copy(alpha = if (isDark) 0.22f else 0.16f),
                    )
                    .padding(horizontal = spacing.small, vertical = spacing.tight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = IconTokens.cleaner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(STATUS_ICON_SIZE),
                )
                Text(
                    text = "SCAN COMPLETE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = spacing.tight),
                )
            }

            // 2. The result — the one thing a user opens this screen to
            //    find out. displayLarge, not displayMedium.
            //
            //    Type.kt reserves displayLarge for two named hero
            //    moments and Sprint 038 read that as excluding this one.
            //    On a real device that reading was wrong: the reclaimed
            //    size IS this screen's hero moment, in the same sense
            //    Home's status headline is Home's, and at displayMedium
            //    it carried no more weight than the paragraph beneath
            //    it. Home gave displayLarge up in Sprint 046 because a
            //    two-line wrapping headline does not need it; a short
            //    number does.
            Text(
                text = formatSize(totalSizeBytes),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = spacing.medium),
            )

            // 3. Subtitle — bound tightly to the number it qualifies,
            //    so the two read as one statement.
            Text(
                text = "across $itemCount file(s) that look reclaimable",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.tight),
            )

            // 4. Reassurance — demoted to labelSmall and pushed away by
            //    the largest gap in the card. It is the least urgent
            //    thing here and was previously set at nearly the same
            //    weight as the subtitle, which is most of why everything
            //    felt equally important.
            Text(
                text = "These files are safe to remove. Your photos, documents and downloads " +
                    "are never touched.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.medium),
            )
        }
    }
}

@Composable
private fun JunkCategoryCard(group: JunkCategoryGroup, totalSizeBytes: Long) {
    val spacing = LocalSpacing.current
    var expanded by remember { mutableStateOf(false) }
    val percent = percentOf(group.totalSizeBytes, totalSizeBytes)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeTokens.card,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
    ) {
        Column {
            JunkCategoryHeaderRow(
                group = group,
                percent = percent,
                expanded = expanded,
                onToggle = { expanded = !expanded },
            )
            AppLinearProgress(
                progress = if (totalSizeBytes > 0) {
                    group.totalSizeBytes.toFloat() / totalSizeBytes.toFloat()
                } else {
                    0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium),
            )
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = spacing.medium,
                        end = spacing.medium,
                        top = spacing.small,
                        bottom = spacing.medium,
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    group.items.forEach { item -> JunkFileRow(item) }
                }
            }
            if (!expanded) {
                Box(modifier = Modifier.height(spacing.medium))
            }
        }
    }
}

/**
 * The always-visible summary row of a category card, and the whole
 * card's toggle target: the entire row is clickable, not just the
 * chevron, so the touch area matches the visual affordance.
 */
@Composable
private fun JunkCategoryHeaderRow(
    group: JunkCategoryGroup,
    percent: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LayoutTokens.minTouchTarget)
            .clickable(onClick = onToggle)
            .padding(spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon = group.category.icon())
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = spacing.small),
        ) {
            Text(
                text = group.category.displayLabel(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${group.items.size} file(s) \u00B7 $percent% of what was found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatSize(group.totalSizeBytes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Icon(
            imageVector = if (expanded) IconTokens.collapse else IconTokens.expand,
            // A real, announced label rather than a decorative null: this
            // is the control that reveals the file list, so TalkBack must
            // be able to describe what it does and which section it acts on.
            contentDescription = if (expanded) {
                "Hide files in ${group.category.displayLabel()}"
            } else {
                "Show files in ${group.category.displayLabel()}"
            },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = spacing.tight),
        )
    }
}

/**
 * A single real file within a category. `reason` is shown rather than
 * hidden — the same evidence-first principle the detection side follows
 * (a user should never be told "this is junk" without being told why),
 * and it is a real field on `CleanableItem`, written by the classifier.
 */
@Composable
private fun JunkFileRow(item: CleanableItem) {
    val spacing = LocalSpacing.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatSize(item.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = spacing.small),
            )
        }
        Text(
            text = item.reason,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------
// State 4 — Nothing found
// ---------------------------------------------------------------------

/**
 * The genuinely-clean result. The reference's storage overview and
 * "Next recommended scan: Tomorrow" row are both omitted — there is no
 * storage statistics provider and no scan scheduler to derive either
 * from. `Done` is omitted too: Clean is a top-level bottom-navigation
 * destination, so "leave this screen" is already one tap away on a
 * persistent bar, and a Done button would need navigation wiring to
 * duplicate it.
 */
@Composable
private fun CleanNothingFound(onScanClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutTokens.screenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        contentPadding = PaddingValues(vertical = spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            FeatureHeader(
                icon = IconTokens.trusted,
                title = "Your storage is clean",
                description = "We didn't find any junk files. Your device is already in good shape.",
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                AppSectionHeader(title = "What we checked")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ShapeTokens.card,
                    elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
                ) {
                    Column(modifier = Modifier.padding(vertical = spacing.small)) {
                        CleanableCategory.entries.forEach { category ->
                            CheckedRow(label = category.displayLabel())
                        }
                    }
                }
            }
        }
        item {
            AppOutlinedButton(
                text = "Scan Again",
                onClick = onScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LayoutTokens.primaryActionHeight),
            )
        }
    }
}

// ---------------------------------------------------------------------
// State 5 — Cleaning (Sprint 039)
// ---------------------------------------------------------------------

/**
 * The cleaning screen. Unlike scanning, this one **can** honestly show a
 * percentage: the candidate list is known before deletion begins, so
 * `CleaningProgress.fraction` is a real fraction of real work, not an
 * animation.
 *
 * `bytesFreed` accumulates the size each file actually was at the moment
 * it was deleted, reported back by `FileDeletionRepository` — never the
 * size recorded at scan time, and never including a file whose deletion
 * failed. "Freed 240 MB" therefore means 240 MB that is genuinely no
 * longer on disk.
 *
 * There is no "time remaining" anywhere on this screen. The reference
 * design's countdown would require predicting how long the remaining
 * deletions will take, which is an estimate dressed as a measurement.
 * The real percentage and the real counts say everything a countdown
 * would, without the invention.
 *
 * `Stop Cleaning` cancels the Job driving the deletion loop, which
 * checks for cancellation between files. Files already deleted stay
 * deleted — the completion screen reports exactly that, and says it was
 * stopped early.
 */
@Composable
private fun CleanCleaning(
    state: CleanUiState.Cleaning,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val progress = state.progress
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutTokens.screenHorizontalPadding)
            .padding(vertical = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(SCANNING_RING_SIZE),
            contentAlignment = Alignment.Center,
        ) {
            AppCircularProgress(
                progress = progress.fraction,
                modifier = Modifier
                    .size(SCANNING_RING_SIZE)
                    .testTag(CLEAN_CLEANING_TEST_TAG),
            )
            Icon(
                imageVector = IconTokens.cleaner,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(HERO_ICON_SIZE),
            )
        }
        Text(
            text = "Cleaning\u2026",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = progress.currentItemName?.let { "Removing $it" }
                ?: "Removing junk files from your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            CounterCell(
                label = "Removed",
                value = "${progress.itemsDeleted} of ${progress.totalItems}",
                modifier = Modifier.weight(1f),
            )
            CounterCell(
                label = "Space freed",
                value = formatSize(progress.bytesFreed),
                modifier = Modifier.weight(1f),
            )
        }
        SafeCleanupCard()
        AppOutlinedButton(
            text = "Stop Cleaning",
            onClick = onCancelClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(LayoutTokens.primaryActionHeight),
        )
    }
}

// ---------------------------------------------------------------------
// State 6 — Completed (Sprint 039)
// ---------------------------------------------------------------------

/**
 * The completion screen, reporting a real `CleaningSummary`.
 *
 * Every figure is measured: bytes actually freed, files actually
 * removed, and the real wall-clock duration of the run. A cancelled
 * cleanup lands here too and says so plainly rather than being dressed
 * up as a finished one — the files deleted before Stop genuinely were
 * deleted, and both that and the ones skipped are stated.
 *
 * Failures are surfaced rather than hidden. A file can vanish or be held
 * open between being scanned and being deleted; when that happens the
 * count is shown, because silently reporting a smaller success total
 * than the user was promised is how a cleaner starts lying by omission.
 */
@Composable
private fun CleanCompleted(
    state: CleanUiState.Completed,
    onScanClick: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val summary = state.summary
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutTokens.screenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        contentPadding = PaddingValues(vertical = spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            FeatureHeader(
                icon = IconTokens.trusted,
                title = if (summary.wasCancelled) "Cleaning stopped" else "Storage cleaned",
                description = if (summary.wasCancelled) {
                    "You stopped the cleanup. Everything removed before that is gone for good."
                } else {
                    "Your device now has more free space."
                },
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                CounterCell(
                    label = "Space freed",
                    value = formatSize(summary.bytesFreed),
                    modifier = Modifier.weight(1f),
                )
                CounterCell(
                    label = "Files removed",
                    value = summary.itemsDeleted.toString(),
                    modifier = Modifier.weight(1f),
                )
                CounterCell(
                    label = "Time taken",
                    value = formatDuration(summary.durationMillis),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (summary.itemsFailed > 0) {
            item {
                InfoCard(
                    icon = IconTokens.warning,
                    title = "${summary.itemsFailed} file(s) couldn't be removed",
                    body = "They may have been in use or already deleted. Running another scan " +
                        "will show whether they are still there.",
                )
            }
        }
        if (summary.itemsSkipped > 0) {
            item {
                InfoCard(
                    icon = IconTokens.recommendation,
                    title = "${summary.itemsSkipped} file(s) not reached",
                    body = "Cleaning stopped before these were processed. They are untouched.",
                )
            }
        }
        state.storage?.let { storage -> item { StorageLine(storage) } }
        item {
            AppFilledButton(
                text = "Done",
                onClick = onDoneClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LayoutTokens.primaryActionHeight),
            )
        }
        item {
            AppOutlinedButton(
                text = "Scan Again",
                onClick = onScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LayoutTokens.primaryActionHeight),
            )
        }
    }
}

// ---------------------------------------------------------------------
// Error
// ---------------------------------------------------------------------

@Composable
private fun CleanError(state: CleanUiState.Error, onScanClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutTokens.screenHorizontalPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppEmptyState(icon = IconTokens.warning, message = state.message)
        AppFilledButton(
            text = "Try Again",
            onClick = onScanClick,
            modifier = Modifier.padding(top = spacing.medium),
        )
    }
}

// ---------------------------------------------------------------------
// Shared, Cleaner-local building blocks
// ---------------------------------------------------------------------

/**
 * The large icon + title + description block both the Idle and
 * Nothing-Found states open with. Kept private and local: two states of
 * one screen is not two screens, so this does not meet this project's
 * bar for a `core:ui` component (the bar Sprint 037 round 2 enforced by
 * deleting `AppStatGroup`).
 *
 * The reference images use bespoke 3D illustrations. None exist as
 * assets in this repo, and inventing a substitute is not a presentation
 * sprint's job — the SDS's own tonal icon-badge motif, already
 * established on Home, is used at hero scale instead.
 */
@Composable
private fun FeatureHeader(icon: ImageVector, title: String, description: String) {
    val spacing = LocalSpacing.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Box(
            modifier = Modifier
                .size(HERO_BADGE_SIZE)
                .clip(ShapeTokens.iconBadge)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = HERO_TINT_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(HERO_ICON_SIZE),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.small),
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CapabilityRow(icon: ImageVector, title: String, description: String) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LayoutTokens.minTouchTarget)
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon = icon)
        Column(modifier = Modifier.padding(start = spacing.small)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CheckedRow(label: String) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LayoutTokens.minTouchTarget)
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = IconTokens.trusted,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(STATUS_ICON_SIZE),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = spacing.small),
        )
    }
}

@Composable
private fun SafeCleanupCard() {
    InfoCard(
        icon = IconTokens.security,
        title = "Safe by design",
        // Sprint 038's copy here said nothing is ever deleted. Sprint 039
        // made that false, so the copy states the real, enforced boundary
        // instead: AppPrivateStorageRoots refuses any path outside this
        // app's own storage, checked inside the deletion repository below
        // every use case. That is a property of the code, not a promise.
        body = "Space Antivirus only removes files from its own app storage. Your photos, " +
            "documents and downloads are never touched.",
    )
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, body: String) {
    val spacing = LocalSpacing.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeTokens.card,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.flat),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(spacing.medium),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(STATUS_ICON_SIZE),
            )
            Column(modifier = Modifier.padding(start = spacing.small)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.tight),
                )
            }
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(CATEGORY_BADGE_SIZE)
            .clip(ShapeTokens.iconBadge)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = BADGE_TINT_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(CATEGORY_ICON_SIZE),
        )
    }
}

// ---------------------------------------------------------------------
// Presentation-layer derivation (no business logic)
// ---------------------------------------------------------------------

/**
 * A category and the real items in it. Purely a grouping of data the
 * ViewModel already provided — no new information is introduced.
 */
private data class JunkCategoryGroup(
    val category: CleanableCategory,
    val items: List<CleanableItem>,
    val totalSizeBytes: Long,
)

private fun List<CleanableItem>.groupIntoCategories(): List<JunkCategoryGroup> = groupBy { it.category }
    .map { (category, items) ->
        JunkCategoryGroup(
            category = category,
            items = items.sortedByDescending { it.sizeBytes },
            totalSizeBytes = items.sumOf { it.sizeBytes },
        )
    }
    .sortedByDescending { it.totalSizeBytes }

private fun percentOf(part: Long, total: Long): Int =
    if (total <= 0L) 0 else ((part.toDouble() / total.toDouble()) * 100).toInt()

private fun CleanableCategory.displayLabel(): String = when (this) {
    CleanableCategory.CACHE_FILE -> "Cache files"
    CleanableCategory.TEMPORARY_FILE -> "Temporary files"
    CleanableCategory.LOG_FILE -> "Log files"
    CleanableCategory.LEFTOVER_INSTALLER -> "Leftover installers"
}

/** Each line describes a rule `JunkFileClassifier` actually implements. */
private fun CleanableCategory.capabilityDescription(): String = when (this) {
    CleanableCategory.CACHE_FILE -> "Files inside an app's cache directory"
    CleanableCategory.TEMPORARY_FILE -> "Files with a common temporary extension"
    CleanableCategory.LOG_FILE -> "Files ending in .log"
    CleanableCategory.LEFTOVER_INSTALLER -> "Old .apk installers left in Downloads"
}

private fun CleanableCategory.icon(): ImageVector = when (this) {
    CleanableCategory.CACHE_FILE -> IconTokens.cacheFile
    CleanableCategory.TEMPORARY_FILE -> IconTokens.temporaryFile
    CleanableCategory.LOG_FILE -> IconTokens.logFile
    CleanableCategory.LEFTOVER_INSTALLER -> IconTokens.leftoverInstaller
}

/** Presentation-layer formatting only (same precedent as HomeScreen's
 *  date formatting, HistoryScreen's duration formatting) — not business
 *  logic, so it stays in the Screen file, not the ViewModel. Unchanged
 *  from Sprint 022. */
/**
 * Real device storage totals, shown only when they loaded. Sprint 038
 * omitted this entirely because no provider existed; `StatFs` needs no
 * permission, so Sprint 039 could close that gap honestly.
 */
@Composable
private fun StorageLine(storage: StorageStatistics) {
    val spacing = LocalSpacing.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeTokens.card,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.flat),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Device storage", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${formatSize(storage.freeBytes)} free",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            storage.usedFraction?.let { fraction ->
                AppLinearProgress(
                    progress = fraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.small),
                )
            }
            Text(
                text = "${formatSize(storage.usedBytes)} used of ${formatSize(storage.totalBytes)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.tight),
            )
        }
    }
}

/** The reference design's "Last cleanup" row, real at last — backed by
 *  the cleanup_records table added in Sprint 039. Absent entirely if the
 *  user has never run one, rather than showing "Never" against a value
 *  that was never stored. */
@Composable
private fun LastCleanupLine(record: CleanupRecord) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LayoutTokens.minTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = IconTokens.history,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(STATUS_ICON_SIZE),
        )
        Text(
            text = "Last cleanup freed ${formatSize(record.bytesFreed)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = spacing.small),
        )
    }
}

/** The scan's supporting line. Shows the current file's NAME only — the
 *  full path would expose this app's internal directory layout to no
 *  benefit. Falls back to a neutral line before the first file arrives. */
private fun JunkScanProgress.currentItemLabel(): String =
    currentPath?.substringAfterLast('/')?.takeIf { it.isNotBlank() }?.let { "Checking $it" }
        ?: "Looking through your app files\u2026"

/** Presentation-layer formatting, same placement rationale as
 *  formatSize. Whole seconds: a cleanup measured to the millisecond
 *  would imply a precision the number does not have. */
private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1_000
    return if (totalSeconds < 60) {
        "$totalSeconds sec"
    } else {
        "${totalSeconds / 60}m ${totalSeconds % 60}s"
    }
}

/** Sprint 040: the implementation moved to `core:ui`'s `formatBytes`
 *  now that Home needs identical formatting. Kept as a local alias so
 *  the ~15 call sites in this file stay unchanged rather than churning
 *  for a rename. */
private fun formatSize(sizeBytes: Long): String = formatBytes(sizeBytes)

// Local dimension constants. Every spacing/shape/color value on this
// screen is an SDS token; these four are icon/illustration sizes, for
// which the SDS has no token layer (Home's own 36dp/18dp/24dp icon sizes
// are written the same way).
private val HERO_BADGE_SIZE = 96.dp
private val HERO_ICON_SIZE = 48.dp
private val SCANNING_RING_SIZE = 96.dp
private val CATEGORY_BADGE_SIZE = 40.dp
private val CATEGORY_ICON_SIZE = 22.dp
private val STATUS_ICON_SIZE = 18.dp
private const val HERO_TINT_ALPHA = 0.12f

private val CLEAN_HERO_PADDING = 20.dp
private const val BADGE_TINT_ALPHA = 0.12f
