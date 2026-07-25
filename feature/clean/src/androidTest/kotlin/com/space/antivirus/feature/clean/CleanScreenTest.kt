package com.space.antivirus.feature.clean

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
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

    @Test
    fun idleState_showsTheScanButton() {
        setScreen(CleanUiState.Idle)

        composeTestRule.onNodeWithText("Scan for Junk Files").assertExists()
    }

    @Test
    fun tappingScanButton_invokesOnScanClick() {
        var clicked = false
        setScreen(CleanUiState.Idle, onScanClick = { clicked = true })

        composeTestRule.onNodeWithText("Scan for Junk Files").performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun loadingState_doesNotShowTheScanButton() {
        setScreen(CleanUiState.Loading)

        composeTestRule.onNodeWithText("Scan for Junk Files").assertDoesNotExist()
    }

    @Test
    fun loadedState_withNoItems_showsThePositiveEmptyMessage() {
        setScreen(CleanUiState.Loaded(items = emptyList(), totalSizeBytes = 0L))

        composeTestRule.onNodeWithText("No junk files found. Your storage looks clean.").assertExists()
    }

    @Test
    fun loadedState_withItems_showsTheTotalSizeAndEveryItem() {
        setScreen(
            CleanUiState.Loaded(
                items = listOf(
                    CleanableItem(
                        path = "/storage/emulated/0/Documents/notes.bak",
                        name = "notes.bak",
                        sizeBytes = 500L,
                        category = CleanableCategory.TEMPORARY_FILE,
                        reason = "File extension \".bak\" is commonly used for temporary files.",
                    ),
                    CleanableItem(
                        path = "/data/data/com.example/cache/thumb.jpg",
                        name = "thumb.jpg",
                        sizeBytes = 1_500L,
                        category = CleanableCategory.CACHE_FILE,
                        reason = "Located in a cache directory — cache contents are safe to clear by " +
                            "Android convention.",
                    ),
                ),
                totalSizeBytes = 2_000L,
            ),
        )

        composeTestRule.onNodeWithText("2.0 KB reclaimable").assertExists()
        composeTestRule.onNodeWithText("2 item(s) found").assertExists()
        composeTestRule.onNodeWithText("notes.bak").assertExists()
        composeTestRule.onNodeWithText("thumb.jpg").assertExists()
    }

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
