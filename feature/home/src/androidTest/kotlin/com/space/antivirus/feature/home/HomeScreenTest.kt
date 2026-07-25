package com.space.antivirus.feature.home

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import com.space.antivirus.core.model.ScanProgress
import org.junit.Rule
import org.junit.Test

/**
 * Tests the stateless HomeScreen directly with hand-built HomeUiState +
 * ScanUiState, same pattern established since Sprint 017 (ADR 0030). No
 * Hilt test infrastructure needed. Updated in Sprint 020 for the real
 * scan action — HomeScreen's signature gained scanState/onScanClick/
 * onAcknowledgeScanResult, so every existing call site needed updating,
 * not just the new scan-specific tests.
 */
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun ComposeContentTestRule.setHomeScreen(
        uiState: HomeUiState,
        scanState: ScanUiState = ScanUiState.Idle,
        onScanClick: () -> Unit = {},
        onAcknowledgeScanResult: () -> Unit = {},
    ) {
        setContent {
            SpaceAntivirusTheme {
                HomeScreen(
                    uiState = uiState,
                    scanState = scanState,
                    onScanClick = onScanClick,
                    onAcknowledgeScanResult = onAcknowledgeScanResult,
                )
            }
        }
    }

    private val unknownStatusState = HomeUiState.Loaded(
        protectionStatus = ProtectionStatus.UNKNOWN,
        lastScanSummary = null,
        trustedItemsCount = 0,
    )

    @Test
    fun loadingState_showsTheLoadingIndicator() {
        composeTestRule.setHomeScreen(uiState = HomeUiState.Loading)

        composeTestRule.onNodeWithTag(HOME_LOADING_TEST_TAG).assertExists()
    }

    @Test
    fun loadedState_withNoScanHistory_showsUnknownStatusAndNoScansYet() {
        composeTestRule.setHomeScreen(uiState = unknownStatusState)

        composeTestRule.onNodeWithText("Protection status unknown").assertExists()
        composeTestRule.onNodeWithText("No scans yet").assertExists()
        composeTestRule.onNodeWithText("Trusted Items").assertExists()
    }

    @Test
    fun loadedState_protected_showsThePositiveStatusMessage() {
        composeTestRule.setHomeScreen(
            uiState = HomeUiState.Loaded(
                protectionStatus = ProtectionStatus.PROTECTED,
                lastScanSummary = LastScanSummary(isClean = true, threatsFound = 0, scannedAtEpochMillis = 0L),
                trustedItemsCount = 0,
            ),
        )

        composeTestRule.onNodeWithText("You're protected").assertExists()
    }

    @Test
    fun loadedState_needsAttention_showsTheAttentionMessage() {
        composeTestRule.setHomeScreen(
            uiState = HomeUiState.Loaded(
                protectionStatus = ProtectionStatus.NEEDS_ATTENTION,
                lastScanSummary = LastScanSummary(isClean = false, threatsFound = 2, scannedAtEpochMillis = 0L),
                trustedItemsCount = 0,
            ),
        )

        composeTestRule.onNodeWithText("Attention needed").assertExists()
    }

    @Test
    fun loadedState_trustedItemsCount_isReflectedInTheCardText() {
        composeTestRule.setHomeScreen(
            uiState = HomeUiState.Loaded(
                protectionStatus = ProtectionStatus.UNKNOWN,
                lastScanSummary = null,
                trustedItemsCount = 5,
            ),
        )

        composeTestRule.onNodeWithText("5 items trusted").assertExists()
    }

    @Test
    fun errorState_showsTheErrorMessage() {
        composeTestRule.setHomeScreen(uiState = HomeUiState.Error("Something went wrong"))

        composeTestRule.onNodeWithText("Something went wrong").assertExists()
    }

    // --- scan action states (Sprint 020) ---

    @Test
    fun scanIdle_showsAnEnabledScanNowButton() {
        composeTestRule.setHomeScreen(uiState = unknownStatusState, scanState = ScanUiState.Idle)

        composeTestRule.onNodeWithText("Scan Now").assertIsEnabled()
    }

    @Test
    fun tappingScanNow_invokesOnScanClick() {
        var clicked = false
        composeTestRule.setHomeScreen(
            uiState = unknownStatusState,
            scanState = ScanUiState.Idle,
            onScanClick = { clicked = true },
        )

        composeTestRule.onNodeWithText("Scan Now").performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun scanRunning_withNoProgressYet_showsStartingScanMessage_noButton() {
        composeTestRule.setHomeScreen(
            uiState = unknownStatusState,
            scanState = ScanUiState.Running(progress = null),
        )

        composeTestRule.onNodeWithText("Starting scan\u2026").assertExists()
        composeTestRule.onNodeWithText("Scan Now").assertDoesNotExist()
    }

    @Test
    fun scanRunning_withProgress_showsTheItemCounts() {
        composeTestRule.setHomeScreen(
            uiState = unknownStatusState,
            scanState = ScanUiState.Running(
                progress = ScanProgress(sessionId = "s1", itemsProcessed = 3, totalItems = 10, threatsFoundSoFar = 0),
            ),
        )

        composeTestRule.onNodeWithText("Scanning\u2026 3 of 10").assertExists()
    }

    @Test
    fun scanCompleted_clean_showsThePositiveResultBanner() {
        composeTestRule.setHomeScreen(
            uiState = unknownStatusState,
            scanState = ScanUiState.Completed(isClean = true, threatsFound = 0, itemsScanned = 10),
        )

        composeTestRule.onNodeWithText("Scan complete — no threats found (10 apps checked).").assertExists()
        // The button remains available so the user can scan again.
        composeTestRule.onNodeWithText("Scan Now").assertIsEnabled()
    }

    @Test
    fun scanCompleted_withThreats_showsTheThreatCountAndPointsToSecurityCenter() {
        composeTestRule.setHomeScreen(
            uiState = unknownStatusState,
            scanState = ScanUiState.Completed(isClean = false, threatsFound = 2, itemsScanned = 10),
        )

        composeTestRule.onNodeWithText(
            "Scan complete — 2 item(s) found. See Security Center for details.",
        ).assertExists()
    }

    @Test
    fun tappingDismissOnTheResultBanner_invokesOnAcknowledgeScanResult() {
        var acknowledged = false
        composeTestRule.setHomeScreen(
            uiState = unknownStatusState,
            scanState = ScanUiState.Completed(isClean = true, threatsFound = 0, itemsScanned = 10),
            onAcknowledgeScanResult = { acknowledged = true },
        )

        composeTestRule.onNodeWithText("Dismiss").performClick()

        assertThat(acknowledged).isTrue()
    }

    @Test
    fun scanError_showsTheErrorMessageAsAResultBanner() {
        composeTestRule.setHomeScreen(
            uiState = unknownStatusState,
            scanState = ScanUiState.Error("A scan is already running."),
        )

        composeTestRule.onNodeWithText("A scan is already running.").assertExists()
    }
}
