package com.space.antivirus.feature.history

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import com.space.antivirus.core.model.RiskLevel
import org.junit.Rule
import org.junit.Test

/**
 * Tests the stateless HistoryScreen directly with hand-built
 * HistoryUiState, same pattern established since Sprint 017 (ADR 0030).
 * No Hilt test infrastructure needed.
 */
class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(uiState: HistoryUiState) {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                HistoryScreen(uiState = uiState)
            }
        }
    }

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
    fun aFlaggedScanEntry_showsEveryThreatsTitleAndDescription() {
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
                            ThreatSummary(
                                title = "Unusual permission combination",
                                description = "Requests SMS access together with INTERNET access",
                                riskLevel = RiskLevel.ATTENTION,
                            ),
                        ),
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("10 apps scanned in 0.8s \u00B7 1 item(s) found").assertExists()
        composeTestRule.onNodeWithText("Unusual permission combination").assertExists()
        composeTestRule.onNodeWithText("Requests SMS access together with INTERNET access").assertExists()
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
}
