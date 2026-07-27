package com.space.antivirus.feature.security

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import com.space.antivirus.core.model.RiskLevel
import org.junit.Rule
import org.junit.Test

/**
 * Tests the stateless SecurityCenterScreen directly with hand-built
 * SecurityCenterUiState, same pattern HomeScreenTest/OnboardingScreenTest
 * established (ADR 0030) — no Hilt test infrastructure needed. Updated in
 * Sprint 021 for the new onViewHistoryClick callback (History's only
 * entry point in the real app).
 */
class SecurityCenterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(uiState: SecurityCenterUiState, onViewHistoryClick: () -> Unit = {}) {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                SecurityCenterScreen(uiState = uiState, onViewHistoryClick = onViewHistoryClick)
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
    fun needsAttentionStatus_showsAppIdentityFirst_forEachDifferentApp() {
        // Sprint 029 root-cause fix, at the rendering layer: two different
        // apps must be visually distinguishable by name, not just their
        // shared, generic threatType category.
        setScreen(
            SecurityCenterUiState.Loaded(
                protectionStatus = ProtectionStatus.NEEDS_ATTENTION,
                lastScanCompletedAtEpochMillis = 2_000L,
                threats = listOf(
                    ThreatSummary(
                        appLabel = "Chrome",
                        packageName = "com.android.chrome",
                        riskLevel = RiskLevel.ATTENTION,
                        reasons = listOf("SMS access with INTERNET access"),
                        recommendation = "Review if unexpected.",
                    ),
                    ThreatSummary(
                        appLabel = "WhatsApp",
                        packageName = "com.definitely.not.whatsapp",
                        riskLevel = RiskLevel.ATTENTION,
                        reasons = listOf("Package doesn't match the real app"),
                        recommendation = "Verify the official listing.",
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("Chrome").assertExists()
        composeTestRule.onNodeWithText("com.android.chrome").assertExists()
        composeTestRule.onNodeWithText("WhatsApp").assertExists()
        composeTestRule.onNodeWithText("com.definitely.not.whatsapp").assertExists()
    }

    @Test
    fun aThreatWithMultipleReasons_showsEveryReasonAsItsOwnBullet() {
        setScreen(
            SecurityCenterUiState.Loaded(
                protectionStatus = ProtectionStatus.NEEDS_ATTENTION,
                lastScanCompletedAtEpochMillis = 2_000L,
                threats = listOf(
                    ThreatSummary(
                        appLabel = "Example App",
                        packageName = "com.example.app",
                        riskLevel = RiskLevel.ACTION_NEEDED,
                        reasons = listOf("Overlay reason", "Surveillance reason", "Installer reason"),
                        recommendation = "Review these findings.",
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("\u2022 Overlay reason").assertExists()
        composeTestRule.onNodeWithText("\u2022 Surveillance reason").assertExists()
        composeTestRule.onNodeWithText("\u2022 Installer reason").assertExists()
    }

    @Test
    fun aThreatCard_showsARecommendation() {
        setScreen(
            SecurityCenterUiState.Loaded(
                protectionStatus = ProtectionStatus.NEEDS_ATTENTION,
                lastScanCompletedAtEpochMillis = 2_000L,
                threats = listOf(
                    ThreatSummary(
                        appLabel = "Example App",
                        packageName = "com.example.app",
                        riskLevel = RiskLevel.ATTENTION,
                        reasons = listOf("Some reason"),
                        recommendation = "Review if unexpected.",
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("Review if unexpected.").assertExists()
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

    @Test
    fun viewFullHistoryButton_existsRegardlessOfProtectionStatus() {
        setScreen(
            SecurityCenterUiState.Loaded(
                protectionStatus = ProtectionStatus.UNKNOWN,
                lastScanCompletedAtEpochMillis = null,
                threats = emptyList(),
            ),
        )

        composeTestRule.onNodeWithText("View full history").assertExists()
    }

    @Test
    fun tappingViewFullHistory_invokesTheCallback() {
        var clicked = false
        setScreen(
            uiState = SecurityCenterUiState.Loaded(
                protectionStatus = ProtectionStatus.PROTECTED,
                lastScanCompletedAtEpochMillis = 2_000L,
                threats = emptyList(),
            ),
            onViewHistoryClick = { clicked = true },
        )

        composeTestRule.onNodeWithText("View full history").performClick()

        assertThat(clicked).isTrue()
    }
}
