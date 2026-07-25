package com.space.antivirus.feature.security

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import com.space.antivirus.core.model.RiskLevel
import org.junit.Rule
import org.junit.Test

/**
 * Tests the stateless SecurityCenterScreen directly with hand-built
 * SecurityCenterUiState, same pattern HomeScreenTest/OnboardingScreenTest
 * established (ADR 0030) — no Hilt test infrastructure needed.
 */
class SecurityCenterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(uiState: SecurityCenterUiState) {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                SecurityCenterScreen(uiState = uiState)
            }
        }
    }

    @Test
    fun unknownStatus_showsTheNoScanYetMessage() {
        setScreen(
            SecurityCenterUiState.Loaded(
                protectionStatus = ProtectionStatus.UNKNOWN,
                lastScanCompletedAtEpochMillis = null,
                threats = emptyList(),
            ),
        )

        composeTestRule.onNodeWithText(
            "No scan results yet. Run a scan from Home to see your security status here.",
        ).assertExists()
    }

    @Test
    fun protectedStatus_withNoThreats_showsThePositiveEmptyMessage_notTheNoScanYetMessage() {
        setScreen(
            SecurityCenterUiState.Loaded(
                protectionStatus = ProtectionStatus.PROTECTED,
                lastScanCompletedAtEpochMillis = 2_000L,
                threats = emptyList(),
            ),
        )

        composeTestRule.onNodeWithText(
            "No threats found. Your last scan didn't detect anything to review.",
        ).assertExists()
        composeTestRule.onNodeWithText(
            "No scan results yet. Run a scan from Home to see your security status here.",
        ).assertDoesNotExist()
    }

    @Test
    fun needsAttentionStatus_rendersEveryThreatsTitleAndDescription() {
        setScreen(
            SecurityCenterUiState.Loaded(
                protectionStatus = ProtectionStatus.NEEDS_ATTENTION,
                lastScanCompletedAtEpochMillis = 2_000L,
                threats = listOf(
                    ThreatSummary(
                        title = "Unusual permission combination",
                        description = "Requests SMS access together with INTERNET access",
                        riskLevel = RiskLevel.ATTENTION,
                    ),
                    ThreatSummary(
                        title = "Possible app impersonation",
                        description = "Package identity does not match the real app",
                        riskLevel = RiskLevel.ATTENTION,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("Unusual permission combination").assertExists()
        composeTestRule.onNodeWithText("Requests SMS access together with INTERNET access").assertExists()
        composeTestRule.onNodeWithText("Possible app impersonation").assertExists()
        composeTestRule.onNodeWithText("Package identity does not match the real app").assertExists()
    }

    @Test
    fun loadingState_doesNotShowAnyLoadedContent() {
        setScreen(SecurityCenterUiState.Loading)

        composeTestRule.onNodeWithText(
            "No scan results yet. Run a scan from Home to see your security status here.",
        ).assertDoesNotExist()
    }

    @Test
    fun errorState_showsTheErrorMessage() {
        setScreen(SecurityCenterUiState.Error("Something went wrong"))

        composeTestRule.onNodeWithText("Something went wrong").assertExists()
    }
}
