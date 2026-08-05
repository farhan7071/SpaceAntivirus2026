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
        onNavigateToSecurityCenter: () -> Unit = {},
        onNavigateToCleaner: () -> Unit = {},
        onNavigateToHistory: () -> Unit = {},
        onNavigateToSettings: () -> Unit = {},
    ) {
        setContent {
            SpaceAntivirusTheme {
                HomeScreen(
                    uiState = uiState,
                    scanState = scanState,
                    onScanClick = onScanClick,
                    onAcknowledgeScanResult = onAcknowledgeScanResult,
                    onNavigateToSecurityCenter = onNavigateToSecurityCenter,
                    onNavigateToCleaner = onNavigateToCleaner,
                    onNavigateToHistory = onNavigateToHistory,
                    onNavigateToSettings = onNavigateToSettings,
                )
            }
        }
    }

    private val unknownStatusState = HomeUiState.Loaded(
        protectionStatus = ProtectionStatus.UNKNOWN,
        lastScanSummary = null,
        trustedItemsCount = 0,
    )

    /** Sprint 046. The hero's metadata row only renders once a scan has
     *  actually run, so outcome assertions need a state that has one. */
    private fun scannedState(
        status: ProtectionStatus,
        threatsFound: Int,
    ) = HomeUiState.Loaded(
        protectionStatus = status,
        lastScanSummary = LastScanSummary(
            scannedAtEpochMillis = 1_700_000_000_000L,
            threatsFound = threatsFound,
            isClean = threatsFound == 0,
        ),
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

        // Sprint 037 (Design Review #5, "Unknown State needs
        // personality"): copy warmed from "Protection status unknown" /
        // "Run your first scan to see your protection status" / "No
        // activity yet. Run your first scan to get started." to more
        // welcoming, onboarding-style text.
        composeTestRule.onNodeWithText("Let's get you protected").assertExists()
        composeTestRule.onNodeWithText("Run your first scan to see how your device is doing").assertExists()
        composeTestRule.onNodeWithText(
            "Nothing to show yet \u2014 run your first scan and we'll keep you posted here.",
        ).assertExists()
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

        // Sprint 036: AppStatCard (core:ui) shows value and label as two
        // separate text nodes ("5" / "Trusted Items"), not one combined
        // "5 items trusted" string the way the pre-redesign TrustedItemsCard
        // did.
        composeTestRule.onNodeWithText("5").assertExists()
        composeTestRule.onNodeWithText("Trusted Items").assertExists()
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

        // Sprint 037 (Design Review #6): replaced the old flat "Scanning…
        // N of M" with an honest, milestone-based message - 3/10 is 30%,
        // below the 50% "Almost done" threshold, so this is the
        // "Scanning your apps" branch.
        composeTestRule.onNodeWithText("Scanning your apps \u2014 3 of 10 checked").assertExists()
    }

    @Test
    fun scanRunning_pastHalfway_showsTheAlmostDoneMessage() {
        composeTestRule.setHomeScreen(
            uiState = unknownStatusState,
            scanState = ScanUiState.Running(
                progress = ScanProgress(sessionId = "s1", itemsProcessed = 8, totalItems = 10, threatsFoundSoFar = 0),
            ),
        )

        // 8/10 is 80%, at or past the 50% threshold.
        composeTestRule.onNodeWithText("Almost done \u2014 8 of 10 apps checked").assertExists()
    }

    @Test
    fun scanCompleted_clean_reportsTheOutcomeOnce() {
        composeTestRule.setHomeScreen(
            uiState = scannedState(ProtectionStatus.PROTECTED, threatsFound = 0),
            scanState = ScanUiState.Completed(isClean = true, threatsFound = 0, itemsScanned = 10),
        )

        // Sprint 046: the outcome moved from a full-width banner into the
        // hero's metadata row. It is reported once now — the banner
        // restated what the headline and supporting line already said,
        // which is what made the card feel heavy.
        composeTestRule.onNodeWithText("No threats found", substring = true).assertExists()
        composeTestRule
            .onNodeWithText("Scan complete — no threats found (10 apps checked).")
            .assertDoesNotExist()
        // The button remains available so the user can scan again.
        composeTestRule.onNodeWithText("Scan Now").assertIsEnabled()
    }

    /**
     * Sprint 046. The banner is gone, but the count the user needs is
     * not: it stays in the hero's supporting line, which is where it was
     * always the actual message rather than a restatement.
     */
    @Test
    fun scanCompleted_withThreats_stillReportsTheCountWithoutRestatingIt() {
        composeTestRule.setHomeScreen(
            uiState = scannedState(ProtectionStatus.NEEDS_ATTENTION, threatsFound = 2),
            scanState = ScanUiState.Completed(isClean = false, threatsFound = 2, itemsScanned = 10),
        )

        composeTestRule.onNodeWithText("Attention needed").assertExists()
        composeTestRule
            .onNodeWithText("Scan complete — 2 item(s) found. See Security Center for details.")
            .assertDoesNotExist()
    }

    /** An error is genuinely new information the rest of the card does
     *  not carry, so it keeps its banner. */
    @Test
    fun scanError_stillShowsItsBanner() {
        composeTestRule.setHomeScreen(
            uiState = unknownStatusState,
            scanState = ScanUiState.Error("A scan is already running."),
        )

        composeTestRule.onNodeWithText("A scan is already running.").assertExists()
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

    // --- Security Summary / Recent Activity, with a real last scan (Sprint 036) ---

    @Test
    fun aLoadedStateWithARealLastScan_showsThreatsFoundAndRecentActivity() {
        composeTestRule.setHomeScreen(
            uiState = HomeUiState.Loaded(
                protectionStatus = ProtectionStatus.NEEDS_ATTENTION,
                lastScanSummary = LastScanSummary(isClean = false, threatsFound = 3, scannedAtEpochMillis = 0L),
                trustedItemsCount = 2,
            ),
        )

        // Security Summary - Threats Found only appears once a real scan
        // exists (Sprint 036's own "never fabricate data" reasoning).
        composeTestRule.onNodeWithText("3").assertExists()
        composeTestRule.onNodeWithText("Threats Found").assertExists()
        // Recent Activity - the one real event this project's current
        // architecture actually produces.
        composeTestRule.onNodeWithText("Full device scan completed").assertExists()
        composeTestRule.onNodeWithText("3 item(s) found", substring = true).assertExists()
    }

    @Test
    fun aLoadedStateWithACleanLastScan_recentActivityShowsNoThreatsDetected() {
        composeTestRule.setHomeScreen(
            uiState = HomeUiState.Loaded(
                protectionStatus = ProtectionStatus.PROTECTED,
                lastScanSummary = LastScanSummary(isClean = true, threatsFound = 0, scannedAtEpochMillis = 0L),
                trustedItemsCount = 0,
            ),
        )

        composeTestRule.onNodeWithText("No threats detected", substring = true).assertExists()
    }

    // --- Quick Actions navigation (Sprint 036) ---

    @Test
    fun tappingSecurityCenterQuickAction_invokesOnNavigateToSecurityCenter() {
        var navigated = false
        composeTestRule.setHomeScreen(uiState = unknownStatusState, onNavigateToSecurityCenter = { navigated = true })

        composeTestRule.onNodeWithText("Security Center").performClick()

        assertThat(navigated).isTrue()
    }

    @Test
    fun tappingCleanerQuickAction_invokesOnNavigateToCleaner() {
        var navigated = false
        composeTestRule.setHomeScreen(uiState = unknownStatusState, onNavigateToCleaner = { navigated = true })

        composeTestRule.onNodeWithText("Cleaner").performClick()

        assertThat(navigated).isTrue()
    }

    @Test
    fun tappingScanHistoryQuickAction_invokesOnNavigateToHistory() {
        var navigated = false
        composeTestRule.setHomeScreen(uiState = unknownStatusState, onNavigateToHistory = { navigated = true })

        composeTestRule.onNodeWithText("Scan History").performClick()

        assertThat(navigated).isTrue()
    }

    @Test
    fun tappingSettingsQuickAction_invokesOnNavigateToSettings() {
        var navigated = false
        composeTestRule.setHomeScreen(uiState = unknownStatusState, onNavigateToSettings = { navigated = true })

        composeTestRule.onNodeWithText("Settings").performClick()

        assertThat(navigated).isTrue()
    }

    // -- Last cleanup in Recent Activity (Sprint 040) -----------------

    private fun cleanupSummary(
        bytesFreed: Long = 482_000_000L,
        itemsDeleted: Int = 152,
        wasCancelled: Boolean = false,
    ) = LastCleanupSummary(
        bytesFreed = bytesFreed,
        itemsDeleted = itemsDeleted,
        cleanedAtEpochMillis = 1_700_000_000_000L,
        wasCancelled = wasCancelled,
    )

    @Test
    fun recentActivity_showsTheRealBytesFreedAndFilesRemoved() {
        composeTestRule.setHomeScreen(
            uiState = unknownStatusState.copy(lastCleanupSummary = cleanupSummary()),
        )

        composeTestRule.onNodeWithText("Junk cleanup completed").assertExists()
        composeTestRule.onNodeWithText("482.0 MB freed \u00B7 152 file(s) removed").assertExists()
    }

    /** Absent entirely until a cleanup has actually run — never a
     *  placeholder "0 B" or "Never". */
    @Test
    fun recentActivity_showsNoCleanupRowBeforeAnyCleanupHasRun() {
        composeTestRule.setHomeScreen(uiState = unknownStatusState)

        composeTestRule.onNodeWithText("Junk cleanup completed").assertDoesNotExist()
        composeTestRule.onNodeWithText("0 B freed \u00B7 0 file(s) removed").assertDoesNotExist()
    }

    @Test
    fun recentActivity_saysPlainlyWhenTheCleanupWasStoppedEarly() {
        composeTestRule.setHomeScreen(
            uiState = unknownStatusState.copy(lastCleanupSummary = cleanupSummary(wasCancelled = true)),
        )

        composeTestRule.onNodeWithText("Cleanup stopped early").assertExists()
        composeTestRule.onNodeWithText("Junk cleanup completed").assertDoesNotExist()
    }

    /** With a cleanup recorded but no scan yet, the section must show the
     *  cleanup rather than the "nothing to show yet" empty state. */
    @Test
    fun recentActivity_withACleanupButNoScan_showsTheCleanupNotTheEmptyState() {
        composeTestRule.setHomeScreen(
            uiState = unknownStatusState.copy(lastCleanupSummary = cleanupSummary()),
        )

        composeTestRule.onNodeWithText("Junk cleanup completed").assertExists()
        composeTestRule
            .onNodeWithText("Nothing to show yet \u2014 run your first scan and we'll keep you posted here.")
            .assertDoesNotExist()
    }
}
