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
        setScreen(CleanUiState.Idle)

        composeTestRule.onNodeWithText("Scan for Junk Files").assertExists()
    }

    @Test
    fun idleState_listsEveryRealCleanableCategory() {
        setScreen(CleanUiState.Idle)

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
        setScreen(CleanUiState.Idle)

        composeTestRule.onNodeWithText("Empty Folders").assertDoesNotExist()
    }

    @Test
    fun tappingScanButton_invokesOnScanClick() {
        var clicked = false
        setScreen(CleanUiState.Idle, onScanClick = { clicked = true })

        composeTestRule.onNodeWithText("Scan for Junk Files").performClick()

        assertThat(clicked).isTrue()
    }

    // -- Scanning -----------------------------------------------------

    @Test
    fun scanningState_showsTheIndeterminateIndicatorAndHidesTheScanButton() {
        setScreen(CleanUiState.Loading)

        composeTestRule.onNodeWithTag(CLEAN_SCANNING_TEST_TAG).assertExists()
        composeTestRule.onNodeWithText("Scan for Junk Files").assertDoesNotExist()
    }

    /**
     * CleanViewModel exposes no cancellation entry point, so a Cancel
     * button would either do nothing or require business-logic changes.
     * Delete this test when Sprint 039 makes cancellation real.
     */
    @Test
    fun scanningState_doesNotOfferCancel() {
        setScreen(CleanUiState.Loading)

        composeTestRule.onNodeWithText("Cancel Scan").assertDoesNotExist()
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
     * Nothing in this project deletes a file (ADR 0035, ADR 0053). A
     * primary action implying otherwise is the single most misleading
     * thing this screen could ship. Delete this test when Sprint 039
     * lands a real delete use case.
     */
    @Test
    fun resultsState_doesNotOfferACleanAction() {
        setScreen(loadedWithCacheItems())

        composeTestRule.onNodeWithText("Clean 2.0 KB").assertDoesNotExist()
        composeTestRule.onNodeWithText("Clean Now").assertDoesNotExist()
    }

    @Test
    fun resultsState_statesPlainlyThatNothingWasDeleted() {
        setScreen(loadedWithCacheItems())

        composeTestRule
            .onNodeWithText("Nothing has been deleted \u2014 this is a report of what the scan found.")
            .assertExists()
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
}
