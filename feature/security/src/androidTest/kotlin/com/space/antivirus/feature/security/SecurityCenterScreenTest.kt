package com.space.antivirus.feature.security

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
 * Tests the stateless SecurityCenterScreen directly with hand-built
 * SecurityCenterUiState, same pattern HomeScreenTest/OnboardingScreenTest
 * established (ADR 0030) — no Hilt test infrastructure needed.
 *
 * Sprint 030 (ADR 0044): rewritten for ThreatSummaryCard's collapsed/
 * expanded structure — evidenceBullets and recommendation now only
 * render after "View details" is tapped, matching the real component's
 * own behavior (ThreatSummaryCardTest, core:ui, covers that expand
 * mechanism directly; this file focuses on this screen's own wiring:
 * which UiState renders what, and that onIgnoreClick/history navigation
 * reach the right callback with the right argument).
 */
class SecurityCenterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(
        uiState: SecurityCenterUiState,
        onViewHistoryClick: () -> Unit = {},
        onIgnoreClick: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                SecurityCenterScreen(
                    uiState = uiState,
                    onViewHistoryClick = onViewHistoryClick,
                    onIgnoreClick = onIgnoreClick,
                )
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
                    threatSummary(appLabel = "Chrome", packageName = "com.android.chrome"),
                    threatSummary(appLabel = "WhatsApp", packageName = "com.definitely.not.whatsapp"),
                ),
            ),
        )

        composeTestRule.onNodeWithText("Chrome").assertExists()
        composeTestRule.onNodeWithText("com.android.chrome").assertExists()
        composeTestRule.onNodeWithText("WhatsApp").assertExists()
        composeTestRule.onNodeWithText("com.definitely.not.whatsapp").assertExists()
    }

    @Test
    fun shortSummaryIsAlwaysVisible_withoutExpanding() {
        setScreen(
            SecurityCenterUiState.Loaded(
                protectionStatus = ProtectionStatus.NEEDS_ATTENTION,
                lastScanCompletedAtEpochMillis = 2_000L,
                threats = listOf(threatSummary(shortSummary = "Can record and transmit media.")),
            ),
        )

        composeTestRule.onNodeWithText("Can record and transmit media.").assertExists()
    }

    @Test
    fun evidenceBulletsAndRecommendation_onlyAppearAfterExpanding() {
        setScreen(
            SecurityCenterUiState.Loaded(
                protectionStatus = ProtectionStatus.NEEDS_ATTENTION,
                lastScanCompletedAtEpochMillis = 2_000L,
                threats = listOf(
                    threatSummary(
                        evidenceBullets = listOf("Overlay reason", "Surveillance reason"),
                        recommendation = "Review these findings.",
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithText("\u2022 Overlay reason").assertDoesNotExist()
        composeTestRule.onNodeWithText("Review these findings.").assertDoesNotExist()

        composeTestRule.onNodeWithText("View details").performClick()

        composeTestRule.onNodeWithText("\u2022 Overlay reason").assertExists()
        composeTestRule.onNodeWithText("\u2022 Surveillance reason").assertExists()
        composeTestRule.onNodeWithText("Review these findings.").assertExists()
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

    @Test
    fun ignoringACard_invokesOnIgnoreClickWithThatCardsPackageName() {
        var ignoredPackageName: String? = null
        setScreen(
            uiState = SecurityCenterUiState.Loaded(
                protectionStatus = ProtectionStatus.NEEDS_ATTENTION,
                lastScanCompletedAtEpochMillis = 2_000L,
                threats = listOf(threatSummary(packageName = "com.example.suspicious")),
            ),
            onIgnoreClick = { ignoredPackageName = it },
        )

        composeTestRule.onNode(hasContentDescription("More actions")).performClick()
        composeTestRule.onNodeWithText("Ignore").performClick()

        assertThat(ignoredPackageName).isEqualTo("com.example.suspicious")
    }
}
