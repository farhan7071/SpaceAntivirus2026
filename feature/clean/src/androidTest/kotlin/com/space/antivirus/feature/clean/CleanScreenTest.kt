package com.space.antivirus.feature.clean

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import com.space.antivirus.core.model.CleanableCategory
import com.space.antivirus.core.model.CleanableItem
import com.space.antivirus.core.model.CleaningProgress
import com.space.antivirus.core.model.CleaningSummary
import com.space.antivirus.core.model.JunkScanProgress
import org.junit.Rule
import org.junit.Test

/**
 * Tests the stateless CleanScreen directly with hand-built CleanUiState,
 * same pattern established since Sprint 017 (ADR 0030). No Hilt test
 * infrastructure needed.
 *
 * Sprint 038 reworked these for the redesigned four-state Cleaner. Three
 * of the assertions below exist specifically to lock in the *absence* of
 * fabricated UI — no Clean button, no Cancel button, no determinate
 * progress — so a future sprint can't reintroduce any of them without a
 * red test asking why (see ADR 0053). Those three become obsolete only
 * when Sprints 039/040 build the real capabilities behind them.
 *
 * Sample data is kept to a single category with two files so every
 * assertion targets content that fits on screen without scrolling the
 * results LazyColumn.
 */
class CleanScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(uiState: CleanUiState, onScanClick: () -> Unit = {}) {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                CleanScreen(uiState = uiState, onScanClick = onScanClick)
            }
        }
    }

    private fun cacheItems() = listOf(
        CleanableItem(
            path = "/data/data/com.example/cache/thumb.jpg",
            name = "thumb.jpg",
            sizeBytes = 1_500L,
            category = CleanableCategory.CACHE_FILE,
            reason = "Located in a cache directory.",
        ),
        CleanableItem(
            path = "/data/data/com.example/cache/avatar.png",
            name = "avatar.png",
            sizeBytes = 500L,
            category = CleanableCategory.CACHE_FILE,
            reason = "Located in a cache directory.",
        ),
    )

    private fun loadedWithCacheItems() = CleanUiState.Loaded(items = cacheItems(), totalSizeBytes = 2_000L)

    // -- Idle ---------------------------------------------------------

    @Test
    fun idleState_showsTheScanButton() {
        setScreen(CleanUiState.Idle())

        composeTestRule.onNodeWithText("Scan for Junk Files").assertExists()
    }

    @Test
    fun idleState_listsEveryRealCleanableCategory() {
        setScreen(CleanUiState.Idle())

        composeTestRule.onNodeWithText("Cache files").assertExists()
        composeTestRule.onNodeWithText("Temporary files").assertExists()
        composeTestRule.onNodeWithText("Log files").assertExists()
        composeTestRule.onNodeWithText("Leftover installers").assertExists()
    }

    /**
     * The reference design lists "Empty Folders" as a fifth capability.
     * JunkFileClassifier only ever classifies files, never directories,
     * so advertising it would describe behavior that does not exist.
     */
    @Test
    fun idleState_doesNotAdvertiseCapabilitiesTheClassifierLacks() {
        setScreen(CleanUiState.Idle())

        composeTestRule.onNodeWithText("Empty Folders").assertDoesNotExist()
    }

    @Test
    fun tappingScanButton_invokesOnScanClick() {
        var clicked = false
        setScreen(CleanUiState.Idle(), onScanClick = { clicked = true })

        composeTestRule.onNodeWithText("Scan for Junk Files").performClick()

        assertThat(clicked).isTrue()
    }

    // -- Scanning -----------------------------------------------------

    @Test
    fun scanningState_showsTheIndeterminateIndicatorAndHidesTheScanButton() {
        setScreen(CleanUiState.Scanning(JunkScanProgress.STARTING))

        composeTestRule.onNodeWithTag(CLEAN_SCANNING_TEST_TAG).assertExists()
        composeTestRule.onNodeWithText("Scan for Junk Files").assertDoesNotExist()
    }

    /**
     * Sprint 038 asserted the opposite of this, and said to delete that
     * test when Sprint 039 made cancellation real. It has: the scan is a
     * cancellable Job and `enumerateFilesAsFlow` checks for cancellation
     * between files, so the button now does what it says.
     */
    @Test
    fun scanningState_offersARealCancel() {
        var cancelled = false
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                CleanScreen(
                    uiState = CleanUiState.Scanning(JunkScanProgress.STARTING),
                    onScanClick = {},
                    onCancelScanClick = { cancelled = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel Scan").performClick()

        assertThat(cancelled).isTrue()
    }

    @Test
    fun scanningState_showsRealCountersAndStillNoPercentage() {
        setScreen(
            CleanUiState.Scanning(
                JunkScanProgress(
                    filesInspected = 1_284,
                    junkFound = 37,
                    bytesFound = 24_800_000L,
                    currentPath = "/data/user/0/com.space.antivirus/cache/9f21.tmp",
                ),
            ),
        )

        composeTestRule.onNodeWithText("1284").assertExists()
        composeTestRule.onNodeWithText("37").assertExists()
        composeTestRule.onNodeWithText("24.8 MB").assertExists()
        // A walk cannot know its own total, so no percentage is claimed.
        composeTestRule.onNodeWithText("62%").assertDoesNotExist()
    }

    /** The full path is deliberately never rendered. */
    @Test
    fun scanningState_showsTheFileNameNotItsPath() {
        setScreen(
            CleanUiState.Scanning(
                JunkScanProgress(
                    filesInspected = 5,
                    junkFound = 1,
                    bytesFound = 10L,
                    currentPath = "/data/user/0/com.space.antivirus/cache/9f21.tmp",
                ),
            ),
        )

        composeTestRule.onNodeWithText("Checking 9f21.tmp").assertExists()
    }

    // -- Results ------------------------------------------------------

    @Test
    fun resultsState_showsTheRealTotalAndCategoryBreakdown() {
        setScreen(loadedWithCacheItems())

        // Appears twice, legitimately: once as the hero total, once as
        // this single category's own total — they are the same number
        // because every item in this fixture is a cache file.
        composeTestRule.onAllNodesWithText("2.0 KB").assertCountEquals(2)
        composeTestRule.onNodeWithText("across 2 file(s) that look reclaimable").assertExists()
        composeTestRule.onNodeWithText("Junk breakdown").assertExists()
        composeTestRule.onNodeWithText("Cache files").assertExists()
        composeTestRule.onNodeWithText("2 file(s) \u00B7 100% of what was found").assertExists()
    }

    @Test
    fun resultsState_startsCollapsedAndRevealsRealFilesWhenExpanded() {
        setScreen(loadedWithCacheItems())

        composeTestRule.onNodeWithText("thumb.jpg").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Show files in Cache files").performClick()

        composeTestRule.onNodeWithText("thumb.jpg").assertExists()
        composeTestRule.onNodeWithText("avatar.png").assertExists()
    }

    /**
     * Sprint 038 asserted that no clean action existed, and said to
     * delete that test once Sprint 039 landed a real delete use case.
     * It has. The button names the real measured total it will free.
     */
    @Test
    fun resultsState_offersARealCleanActionNamingTheRealTotal() {
        var cleaned = false
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                CleanScreen(
                    uiState = loadedWithCacheItems(),
                    onScanClick = {},
                    onCleanClick = { cleaned = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Clean 2.0 KB").performClick()

        assertThat(cleaned).isTrue()
    }

    @Test
    fun resultsState_offersAnotherScan() {
        var clicked = false
        setScreen(loadedWithCacheItems(), onScanClick = { clicked = true })

        composeTestRule.onNodeWithText("Scan Again").performClick()

        assertThat(clicked).isTrue()
    }

    // -- Nothing found ------------------------------------------------

    @Test
    fun nothingFoundState_showsThePositiveResultAndWhatWasChecked() {
        setScreen(CleanUiState.Loaded(items = emptyList(), totalSizeBytes = 0L))

        composeTestRule.onNodeWithText("Your storage is clean").assertExists()
        composeTestRule.onNodeWithText("What we checked").assertExists()
        composeTestRule.onNodeWithText("Cache files").assertExists()
    }

    @Test
    fun nothingFoundState_doesNotShowAJunkBreakdown() {
        setScreen(CleanUiState.Loaded(items = emptyList(), totalSizeBytes = 0L))

        composeTestRule.onNodeWithText("Junk breakdown").assertDoesNotExist()
    }

    @Test
    fun nothingFoundState_tappingScanAgain_invokesOnScanClick() {
        var clicked = false
        setScreen(CleanUiState.Loaded(items = emptyList(), totalSizeBytes = 0L), onScanClick = { clicked = true })

        composeTestRule.onNodeWithText("Scan Again").performClick()

        assertThat(clicked).isTrue()
    }

    // -- Error --------------------------------------------------------

    @Test
    fun errorState_showsTheErrorMessageAndARetryButton() {
        setScreen(CleanUiState.Error("Something went wrong scanning for junk files."))

        composeTestRule.onNodeWithText("Something went wrong scanning for junk files.").assertExists()
        composeTestRule.onNodeWithText("Try Again").assertExists()
    }

    @Test
    fun tappingTryAgain_invokesOnScanClick() {
        var clicked = false
        setScreen(CleanUiState.Error("Something went wrong."), onScanClick = { clicked = true })

        composeTestRule.onNodeWithText("Try Again").performClick()

        assertThat(clicked).isTrue()
    }

    // -- Cleaning (Sprint 039) ----------------------------------------

    @Test
    fun cleaningState_showsRealProgressAndBytesFreed() {
        setScreen(
            CleanUiState.Cleaning(
                CleaningProgress(
                    itemsProcessed = 32,
                    totalItems = 50,
                    itemsDeleted = 31,
                    itemsFailed = 1,
                    bytesFreed = 18_400_000L,
                    currentItemName = "cache_9f21.tmp",
                ),
            ),
        )

        composeTestRule.onNodeWithTag(CLEAN_CLEANING_TEST_TAG).assertExists()
        composeTestRule.onNodeWithText("31 of 50").assertExists()
        composeTestRule.onNodeWithText("18.4 MB").assertExists()
        composeTestRule.onNodeWithText("Removing cache_9f21.tmp").assertExists()
    }

    /**
     * A countdown would be a prediction presented as a measurement. The
     * real percentage and counts carry the same information honestly.
     */
    @Test
    fun cleaningState_doesNotShowTimeRemaining() {
        setScreen(CleanUiState.Cleaning(CleaningProgress.starting(10)))

        composeTestRule.onNodeWithText("Time left").assertDoesNotExist()
    }

    @Test
    fun cleaningState_offersARealStop() {
        var stopped = false
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                CleanScreen(
                    uiState = CleanUiState.Cleaning(CleaningProgress.starting(10)),
                    onScanClick = {},
                    onCancelCleanClick = { stopped = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Stop Cleaning").performClick()

        assertThat(stopped).isTrue()
    }

    // -- Completed (Sprint 039) ---------------------------------------

    private fun completedSummary(
        itemsFailed: Int = 0,
        itemsRequested: Int = 48,
        wasCancelled: Boolean = false,
    ) = CleaningSummary(
        itemsRequested = itemsRequested,
        itemsDeleted = 48 - itemsFailed,
        itemsFailed = itemsFailed,
        bytesFreed = 26_200_000L,
        durationMillis = 4_300L,
        completedAtEpochMillis = 0L,
        wasCancelled = wasCancelled,
    )

    @Test
    fun completedState_reportsRealFreedBytesRemovedFilesAndDuration() {
        setScreen(CleanUiState.Completed(summary = completedSummary()))

        composeTestRule.onNodeWithText("Storage cleaned").assertExists()
        composeTestRule.onNodeWithText("26.2 MB").assertExists()
        composeTestRule.onNodeWithText("48").assertExists()
        composeTestRule.onNodeWithText("4 sec").assertExists()
    }

    /** Failures are surfaced, never quietly folded into the success
     *  total — that is how a cleaner starts lying by omission. */
    @Test
    fun completedState_surfacesFilesThatCouldNotBeRemoved() {
        setScreen(CleanUiState.Completed(summary = completedSummary(itemsFailed = 2)))

        composeTestRule.onNodeWithText("2 file(s) couldn't be removed").assertExists()
    }

    @Test
    fun completedState_saysPlainlyWhenTheUserStoppedEarly() {
        setScreen(
            CleanUiState.Completed(
                summary = completedSummary(itemsRequested = 100, wasCancelled = true),
            ),
        )

        composeTestRule.onNodeWithText("Cleaning stopped").assertExists()
        composeTestRule.onNodeWithText("52 file(s) not reached").assertExists()
    }
}
