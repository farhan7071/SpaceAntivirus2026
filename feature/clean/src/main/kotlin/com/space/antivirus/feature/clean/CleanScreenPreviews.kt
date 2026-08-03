package com.space.antivirus.feature.clean

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import com.space.antivirus.core.model.CleanableCategory
import com.space.antivirus.core.model.CleanableItem
import com.space.antivirus.core.model.CleaningProgress
import com.space.antivirus.core.model.CleaningSummary
import com.space.antivirus.core.model.JunkScanProgress
import com.space.antivirus.core.model.StorageStatistics

/**
 * Sprint 038 — the first Compose previews in this project.
 *
 * Every prior sprint verified UI through instrumented Compose tests
 * only; `compose-ui-tooling-preview` has been on every feature module's
 * classpath since Sprint 003 (via `AndroidLibraryComposeConventionPlugin`)
 * without a single `@Preview` ever being written. For a screen whose
 * acceptance criterion is "matches an approved visual reference across
 * four distinct states, in both light and dark," being able to see all
 * four side by side in Android Studio is worth more here than anywhere
 * this project has built UI so far.
 *
 * Kept in their own file rather than appended to `CleanScreen.kt`: these
 * are development tooling, not part of the screen's own composition, and
 * the sample data below exists only to feed them.
 *
 * Previews call the stateless `CleanScreen` with hand-built
 * `CleanUiState`, exactly as `CleanScreenTest` does — no ViewModel, no
 * Hilt, no fake repository.
 */
private const val PREVIEW_NAME_IDLE = "Cleaner \u2014 Idle"
private const val PREVIEW_NAME_SCANNING = "Cleaner \u2014 Scanning"
private const val PREVIEW_NAME_RESULTS = "Cleaner \u2014 Results"
private const val PREVIEW_NAME_CLEAN = "Cleaner \u2014 Nothing found"
private const val PREVIEW_NAME_CLEANING = "Cleaner \u2014 Cleaning"
private const val PREVIEW_NAME_DONE = "Cleaner \u2014 Completed"

private fun previewItems(): List<CleanableItem> = listOf(
    CleanableItem(
        path = "/data/data/com.example.chat/cache/thumbs/img_0041.jpg",
        name = "img_0041.jpg",
        sizeBytes = 3_400_000L,
        category = CleanableCategory.CACHE_FILE,
        reason = "Located in a cache directory \u2014 cache contents are safe to clear by Android convention.",
    ),
    CleanableItem(
        path = "/data/data/com.example.chat/cache/thumbs/img_0042.jpg",
        name = "img_0042.jpg",
        sizeBytes = 2_100_000L,
        category = CleanableCategory.CACHE_FILE,
        reason = "Located in a cache directory \u2014 cache contents are safe to clear by Android convention.",
    ),
    CleanableItem(
        path = "/storage/emulated/0/Documents/report.tmp",
        name = "report.tmp",
        sizeBytes = 820_000L,
        category = CleanableCategory.TEMPORARY_FILE,
        reason = "File extension \".tmp\" is commonly used for temporary files.",
    ),
    CleanableItem(
        path = "/storage/emulated/0/Android/data/com.example/files/debug.log",
        name = "debug.log",
        sizeBytes = 240_000L,
        category = CleanableCategory.LOG_FILE,
        reason = "Log file (.log extension).",
    ),
    CleanableItem(
        path = "/storage/emulated/0/Download/app-release.apk",
        name = "app-release.apk",
        sizeBytes = 18_600_000L,
        category = CleanableCategory.LEFTOVER_INSTALLER,
        reason = "Downloaded app installer (.apk) in Downloads, unmodified for over 24 hours.",
    ),
)

private fun previewLoadedState(): CleanUiState.Loaded {
    val items = previewItems()
    return CleanUiState.Loaded(
        items = items,
        totalSizeBytes = items.sumOf { it.sizeBytes },
        storage = previewStorage(),
    )
}

private fun previewStorage(): StorageStatistics =
    StorageStatistics(totalBytes = 64_000_000_000L, freeBytes = 18_400_000_000L)

@Preview(name = PREVIEW_NAME_IDLE, showBackground = true)
@Preview(name = PREVIEW_NAME_IDLE, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CleanIdlePreview() {
    SpaceAntivirusTheme {
        CleanScreen(uiState = CleanUiState.Idle(storage = previewStorage()), onScanClick = {})
    }
}

@Preview(name = PREVIEW_NAME_SCANNING, showBackground = true)
@Preview(name = PREVIEW_NAME_SCANNING, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CleanScanningPreview() {
    SpaceAntivirusTheme {
        CleanScreen(
            uiState = CleanUiState.Scanning(
                JunkScanProgress(
                    filesInspected = 1_284,
                    junkFound = 37,
                    bytesFound = 24_800_000L,
                    currentPath = "/data/user/0/com.space.antivirus/cache/image_cache/9f21.tmp",
                ),
            ),
            onScanClick = {},
        )
    }
}

@Preview(name = PREVIEW_NAME_RESULTS, showBackground = true, heightDp = 900)
@Preview(
    name = PREVIEW_NAME_RESULTS,
    showBackground = true,
    heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CleanResultsPreview() {
    SpaceAntivirusTheme {
        CleanScreen(uiState = previewLoadedState(), onScanClick = {})
    }
}

@Preview(name = PREVIEW_NAME_CLEAN, showBackground = true)
@Preview(name = PREVIEW_NAME_CLEAN, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CleanNothingFoundPreview() {
    SpaceAntivirusTheme {
        CleanScreen(
            uiState = CleanUiState.Loaded(items = emptyList(), totalSizeBytes = 0L),
            onScanClick = {},
        )
    }
}

@Preview(name = PREVIEW_NAME_CLEANING, showBackground = true)
@Preview(name = PREVIEW_NAME_CLEANING, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CleanCleaningPreview() {
    SpaceAntivirusTheme {
        CleanScreen(
            uiState = CleanUiState.Cleaning(
                CleaningProgress(
                    itemsProcessed = 32,
                    totalItems = 50,
                    itemsDeleted = 31,
                    itemsFailed = 1,
                    bytesFreed = 18_400_000L,
                    currentItemName = "image_cache_9f21.tmp",
                ),
            ),
            onScanClick = {},
        )
    }
}

@Preview(name = PREVIEW_NAME_DONE, showBackground = true, heightDp = 900)
@Preview(
    name = PREVIEW_NAME_DONE,
    showBackground = true,
    heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CleanCompletedPreview() {
    SpaceAntivirusTheme {
        CleanScreen(
            uiState = CleanUiState.Completed(
                summary = CleaningSummary(
                    itemsRequested = 50,
                    itemsDeleted = 48,
                    itemsFailed = 2,
                    bytesFreed = 26_200_000L,
                    durationMillis = 4_300L,
                    completedAtEpochMillis = 0L,
                    wasCancelled = false,
                ),
                storage = previewStorage(),
            ),
            onScanClick = {},
        )
    }
}
