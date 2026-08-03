package com.space.antivirus.feature.clean

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.space.antivirus.core.designsystem.theme.Elevation
import com.space.antivirus.core.designsystem.theme.IconTokens
import com.space.antivirus.core.designsystem.theme.LayoutTokens
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.designsystem.theme.ShapeTokens
import com.space.antivirus.core.model.CleanableCategory
import com.space.antivirus.core.model.CleanableItem
import com.space.antivirus.core.ui.component.AppCircularProgress
import com.space.antivirus.core.ui.component.AppEmptyState
import com.space.antivirus.core.ui.component.AppFilledButton
import com.space.antivirus.core.ui.component.AppLinearProgress
import com.space.antivirus.core.ui.component.AppOutlinedButton
import com.space.antivirus.core.ui.component.AppSectionHeader

/** Exposed so CleanScreenTest can find the scanning indicator — the
 *  Scanning state has no determinate value to assert on by design. */
const val CLEAN_SCANNING_TEST_TAG = "clean_scanning_indicator"

/**
 * Sprint 038 — a full presentation-layer overhaul of the Junk Scanner,
 * replacing the plain Sprint 022 list view with the approved Cleaner
 * design language.
 *
 * **Scope, and why it is what it is.** The sprint brief originally asked
 * for six screens, including Cleaning Progress and Cleaning Complete.
 * A verification pass against the actual codebase found those two cannot
 * be built honestly: nothing in this project deletes a file. That is not
 * an oversight to work around — `CleanableItem`, `FindCleanableItemsUseCase`
 * and `CleanViewModel` each state it explicitly in their own KDoc, and
 * ADR 0035 scoped the domain layer to candidates only. There is likewise
 * no progress Flow (`FindCleanableItemsUseCase` is a one-shot `suspend`
 * returning a finished `List`), no cancellation, no storage statistics
 * anywhere in the tree, and no cleanup history. Building the reference
 * design's percentage counters, per-file paths, countdown timers,
 * "space freed" totals or a `Stop Cleaning` button would mean shipping a
 * user-visible claim this code cannot verify — the exact fabrication
 * ADR 0015's "never exaggerate" discipline and every prior sprint's data
 * rules forbid. Sprint 038 was rescoped by the project owner to the four
 * states real data supports; Sprint 039 builds the cleaning domain layer,
 * and Sprint 040 builds those two screens on top of it.
 *
 * Everything on screen here is derived from `CleanUiState` and nothing
 * else. Category totals, percentages and file counts are all computed
 * from the real `List<CleanableItem>` the classifier returned.
 */
@Composable
fun CleanRoute(
    viewModel: CleanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CleanScreen(uiState = uiState, onScanClick = viewModel::scanForJunk)
}

@Composable
fun CleanScreen(uiState: CleanUiState, onScanClick: () -> Unit, modifier: Modifier = Modifier) {
    when (uiState) {
        is CleanUiState.Idle -> CleanIdle(onScanClick, modifier)
        is CleanUiState.Loading -> CleanScanning(modifier)
        is CleanUiState.Loaded ->
            if (uiState.items.isEmpty()) {
                CleanNothingFound(onScanClick, modifier)
            } else {
                CleanResults(uiState, onScanClick, modifier)
            }
        is CleanUiState.Error -> CleanError(uiState, onScanClick, modifier)
    }
}

// ---------------------------------------------------------------------
// State 1 — Idle
// ---------------------------------------------------------------------

/**
 * The pre-scan screen. The reference design's storage ring ("128 GB")
 * and "Last cleanup" row are both absent here, deliberately: no storage
 * statistics provider and no cleanup history exist in this project, and
 * the sprint's own data rule is to omit rather than invent.
 *
 * The four capability rows are not decorative copy — each one names a
 * rule `JunkFileClassifier` (`domain/cleaning`) genuinely implements,
 * and the set matches `CleanableCategory`'s four real values exactly.
 * "Empty Folders", shown in the reference, is not among them: the
 * classifier only ever classifies files, never directories, so a row
 * promising it would describe behavior that does not exist.
 */
@Composable
private fun CleanIdle(onScanClick: () -> Unit, modifier: Modifier = Modifier) {
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
        item { ReadOnlyReassuranceCard() }
        item {
            AppFilledButton(
                text = "Scan for Junk Files",
                onClick = onScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LayoutTokens.primaryActionHeight),
            )
        }
    }
}

// ---------------------------------------------------------------------
// State 2 — Scanning
// ---------------------------------------------------------------------

/**
 * The in-progress screen, built on an **indeterminate** indicator.
 *
 * The reference design shows 62%, a live file path, a processed/total
 * file count and a countdown. None of those exist:
 * `FindCleanableItemsUseCase` suspends once and returns a completed
 * list, emitting nothing along the way, and `CleanUiState.Loading`
 * carries no fields at all. A determinate ring here would be an
 * animation invented in the UI layer and presented as measurement —
 * precisely the failure Sprint 037 avoided when it declined to show
 * named scan phases `ScanProgress` had no field for.
 *
 * This is not a departure from the design system, either.
 * `AppProgressIndicator.kt`'s own KDoc states the SDS rule as
 * "determinate-first... wherever the underlying process reports one."
 * This one reports nothing, so indeterminate is what that rule actually
 * prescribes here — checked before assuming, rather than after.
 *
 * `Cancel Scan` is likewise absent rather than decorative:
 * `CleanViewModel` exposes no cancellation entry point, so a visible
 * Cancel button would either do nothing or require business-logic
 * changes this presentation-only sprint may not make.
 *
 * What *is* shown is fully verifiable: the four things the classifier
 * actually looks for, and the fact that scanning reads files without
 * modifying them — which is not reassurance copy but a literal
 * description of what this code path does.
 */
@Composable
private fun CleanScanning(modifier: Modifier = Modifier) {
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
            text = "This can take a moment on a device with a lot of files.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        InfoCard(
            icon = IconTokens.scan,
            title = "What we're checking",
            body = "Cache directories, files with common temporary extensions, log files, and app " +
                "installers left in Downloads.",
        )
        InfoCard(
            icon = IconTokens.security,
            title = "Read-only",
            body = "Scanning only reads file information. Nothing is opened, changed or deleted.",
        )
    }
}

// ---------------------------------------------------------------------
// State 3 — Results
// ---------------------------------------------------------------------

/**
 * The results screen.
 *
 * Two deliberate departures from the reference image, both documented in
 * ADR 0053:
 *
 * 1. **No `Clean 482 MB` button.** Nothing in this project can delete a
 *    file. A primary action that looked like it cleaned but didn't would
 *    be the single most misleading thing this screen could ship, and a
 *    disabled one promising a future capability is no better. The screen
 *    is therefore honest about what it is — a report — and says so in
 *    plain words rather than implying otherwise through a button.
 * 2. **No alarm-red hero.** The reference tints "Junk found" in the same
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
        item { AppSectionHeader(title = "Junk breakdown") }
        items(groups.size) { index ->
            val group = groups[index]
            JunkCategoryCard(group = group, totalSizeBytes = state.totalSizeBytes)
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

@Composable
private fun ResultsHeroCard(totalSizeBytes: Long, itemCount: Int) {
    val spacing = LocalSpacing.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeTokens.heroCard,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.floating),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = HERO_TINT_ALPHA),
        ),
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
            // displayMedium, not displayLarge: Type.kt's own KDoc reserves
            // displayLarge for exactly two hero moments (Home's status
            // headline and the scan-complete moment) and says it "is not
            // used generically". A junk total is neither of those, so it
            // takes the next step down rather than quietly widening that
            // reservation to a third case.
            Text(
                text = formatSize(totalSizeBytes),
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = "across $itemCount file(s) that look reclaimable",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Nothing has been deleted \u2014 this is a report of what the scan found.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun ReadOnlyReassuranceCard() {
    InfoCard(
        icon = IconTokens.security,
        title = "Safe by design",
        // Literally true of this code path, not marketing copy: the scan
        // enumerates and classifies files, and no delete-capable use case
        // exists anywhere in this project for it to call.
        body = "This scan is read-only. Space Antivirus lists what it finds \u2014 your photos, " +
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
private fun formatSize(sizeBytes: Long): String = when {
    sizeBytes >= 1_000_000 -> "%.1f MB".format(sizeBytes / 1_000_000.0)
    sizeBytes >= 1_000 -> "%.1f KB".format(sizeBytes / 1_000.0)
    else -> "$sizeBytes B"
}

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
private const val BADGE_TINT_ALPHA = 0.12f
