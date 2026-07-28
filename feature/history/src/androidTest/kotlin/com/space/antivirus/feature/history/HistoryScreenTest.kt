package com.space.antivirus.feature.history

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import com.space.antivirus.core.model.RiskLevel
import org.junit.Rule
import org.junit.Test

/**
 * Tests the stateless HistoryScreen directly with hand-built
 * HistoryUiState, same pattern established since Sprint 017 (ADR 0030).
 * No Hilt test infrastructure needed.
 *
 * Sprint 030 (ADR 0044): rewritten for ThreatSummaryCard's collapsed/
 * expanded structure — same reasoning as SecurityCenterScreenTest's
 * identical rewrite. This file focuses on this screen's own wiring (the
 * per-session summary line, and that onIgnoreClick reaches the right
 * package name); ThreatSummaryCardTest (core:ui) already covers the
 * shared component's own expand/collapse and menu behavior directly.
 */
class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(uiState: HistoryUiState, onIgnoreClick: (String) -> Unit = {}) {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                HistoryScreen(uiState = uiState, onIgnoreClick = onIgnoreClick)
            }
        }
    }

    private fun threatSummary(
        appLabel: String = "Example App",
        packageName: String = "com.example.app",
        riskLevel: RiskLevel = RiskLevel.ATTENTION,
        shortSummary: String = "A short summary.",
        technicalDetail: String = "The full technical explanation.",
        evidenceBullets: List<String> = listOf("Some reason"),
        recommendation: String = "Review if unexpected.",
    ) = ThreatSummary(
        appLabel = appLabel,
        packageName = packageName,
        riskLevel = riskLevel,
        shortSummary = shortSummary,
        technicalDetail = technicalDetail,
        evidenceBullets = evidenceBullets,
        recommendation = recommendation,
    )

    @Test
    fun loadingState_doesNotShowAnyEntryContent() {
        setScreen(HistoryUiState.Loading)

        composeTestRule.onNodeWithText("No scans yet. Run a scan from Home to see your history here.")
            .assertDoesNotExist()
    }

    @Test
    fun emptyHistory_showsTheNoScansYetMessage() {
        setScreen(HistoryUiState.Loaded(entries = emptyList()))

        composeTestRule.onNodeWithText("No scans yet. Run a scan from Home to see your history here.").assertExists()
    }

    @Test
    fun aCleanScanEntry_showsNoThreatsFoundAndTheScanMetadata() {
        setScreen(
            HistoryUiState.Loaded(
                entries = listOf(
                    ScanHistoryEntry(
                        sessionId = "s1",
                        completedAtEpochMillis = 0L,
                        durationMillis = 1_500,
                        itemsScanned = 20,
                        isClean = true,
                        threats = emptyList(),
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("20 apps scanned in 1.5s \u00B7 No threats found").assertExists()
    }

    @Test
    fun aFlaggedScanEntry_showsScanMetadataAndAppIdentityAndShortSummary_withoutExpanding() {
        setScreen(
            HistoryUiState.Loaded(
                entries = listOf(
                    ScanHistoryEntry(
                        sessionId = "s1",
                        completedAtEpochMillis = 0L,
                        durationMillis = 800,
                        itemsScanned = 10,
                        isClean = false,
                        threats = listOf(
                            threatSummary(
                                appLabel = "Suspicious App",
                                packageName = "com.example.suspicious",
                                shortSummary = "Can access SMS and internet.",
                            ),
                        ),
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("10 apps scanned in 0.8s \u00B7 1 item(s) found").assertExists()
        composeTestRule.onNodeWithText("Suspicious App").assertExists()
        composeTestRule.onNodeWithText("com.example.suspicious").assertExists()
        composeTestRule.onNodeWithText("Can access SMS and internet.").assertExists()
    }

    @Test
    fun aFlaggedScanEntry_evidenceAndRecommendation_onlyAppearAfterExpanding() {
        setScreen(
            HistoryUiState.Loaded(
                entries = listOf(
                    ScanHistoryEntry(
                        sessionId = "s1",
                        completedAtEpochMillis = 0L,
                        durationMillis = 800,
                        itemsScanned = 10,
                        isClean = false,
                        threats = listOf(
                            threatSummary(
                                evidenceBullets = listOf("SMS access with INTERNET access"),
                                recommendation = "Review if unexpected.",
                            ),
                        ),
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("\u2022 SMS access with INTERNET access").assertDoesNotExist()

        composeTestRule.onNodeWithText("View details").performClick()

        composeTestRule.onNodeWithText("\u2022 SMS access with INTERNET access").assertExists()
        composeTestRule.onNodeWithText("Review if unexpected.").assertExists()
    }

    @Test
    fun multipleEntries_areAllRendered() {
        setScreen(
            HistoryUiState.Loaded(
                entries = listOf(
                    ScanHistoryEntry(
                        sessionId = "s1",
                        completedAtEpochMillis = 0L,
                        durationMillis = 500,
                        itemsScanned = 5,
                        isClean = true,
                        threats = emptyList(),
                    ),
                    ScanHistoryEntry(
                        sessionId = "s2",
                        completedAtEpochMillis = 1_000L,
                        durationMillis = 900,
                        itemsScanned = 12,
                        isClean = true,
                        threats = emptyList(),
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("5 apps scanned in 0.5s \u00B7 No threats found").assertExists()
        composeTestRule.onNodeWithText("12 apps scanned in 0.9s \u00B7 No threats found").assertExists()
    }

    @Test
    fun errorState_showsTheErrorMessage() {
        setScreen(HistoryUiState.Error("Something went wrong"))

        composeTestRule.onNodeWithText("Something went wrong").assertExists()
    }

    @Test
    fun ignoringACard_invokesOnIgnoreClickWithThatCardsPackageName() {
        var ignoredPackageName: String? = null
        setScreen(
            uiState = HistoryUiState.Loaded(
                entries = listOf(
                    ScanHistoryEntry(
                        sessionId = "s1",
                        completedAtEpochMillis = 0L,
                        durationMillis = 800,
                        itemsScanned = 10,
                        isClean = false,
                        threats = listOf(threatSummary(packageName = "com.example.suspicious")),
                    ),
                ),
            ),
            onIgnoreClick = { ignoredPackageName = it },
        )

        composeTestRule.onNode(hasContentDescription("More actions")).performClick()
        composeTestRule.onNodeWithText("Ignore").performClick()

        assertThat(ignoredPackageName).isEqualTo("com.example.suspicious")
    }
}
