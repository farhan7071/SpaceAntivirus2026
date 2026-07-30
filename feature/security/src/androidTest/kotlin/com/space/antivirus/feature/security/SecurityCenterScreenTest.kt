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
        threatCategory: String = "Permission Usage",
        shortSummary: String = "A short summary.",
        technicalDetail: String = "The full technical explanation.",
        evidenceBullets: List<String> = listOf("Some reason"),
        recommendation: String = "Review if unexpected.",
        confidenceLabel: String = "Medium",
    ) = ThreatSummary(
        appLabel = appLabel,
        packageName = packageName,
        riskLevel = riskLevel,
        threatCategory = threatCategory,
        shortSummary = shortSummary,
        technicalDetail = technicalDetail,
        evidenceBullets = evidenceBullets,
        recommendation = recommendation,
        confidenceLabel = confidenceLabel,
    )

    @Test
    fun unknownStatus_showsTheNoScanYetMessage() {
        setScreen(
            SecurityCenterUiState.Loaded(
                protectionStatus = ProtectionStatus.UNKNOWN,
                lastScanCompletedAtEpochMillis = null,
                threats = emptyList(),
                scanSummary = null,
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
                scanSummary = null,
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
                scanSummary = null,
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
                scanSummary = null,
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
                scanSummary = null,
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
                scanSummary = null,
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
                scanSummary = null,
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
                scanSummary = null,
            ),
            onIgnoreClick = { ignoredPackageName = it },
        )

        composeTestRule.onNode(hasContentDescription("More actions")).performClick()
        composeTestRule.onNodeWithText("Ignore").performClick()

        assertThat(ignoredPackageName).isEqualTo("com.example.suspicious")
    }

    // --- Sprint 034, Part 1: scan summary dashboard display ---

    @Test
    fun scanSummary_showsTheDashboard_whenPresent() {
        setScreen(
            SecurityCenterUiState.Loaded(
                protectionStatus = ProtectionStatus.NEEDS_ATTENTION,
                lastScanCompletedAtEpochMillis = 2_000L,
                threats = listOf(threatSummary()),
                scanSummary = ScanSummary(
                    appsScanned = 42,
                    threatsDetected = 1,
                    trustedApps = 3,
                    ignoredThreats = 2,
                    scanDurationMillis = 1_500L,
                    highestThreatLabel = "Attention",
                    averageConfidenceLabel = "Medium",
                ),
            ),
        )

        // Redesigned as a dashboard (Sprint 034, ScanSummaryCard,
        // core:ui) — value and label are now separate text nodes in a
        // stat grid, not one combined "Label: value" string.
        composeTestRule.onNodeWithText("Needs your attention").assertExists()
        composeTestRule.onNodeWithText("42").assertExists()
        composeTestRule.onNodeWithText("Apps scanned").assertExists()
        composeTestRule.onNodeWithText("3").assertExists()
        composeTestRule.onNodeWithText("Trusted").assertExists()
        // "Attention" itself is deliberately not asserted here - the
        // threat card below the summary also renders a StatusChip with
        // that same text (Severity.ATTENTION's label), and
        // onNodeWithText expects exactly one match by default; "2" for
        // Ignored and the other unambiguous labels below are sufficient
        // to confirm the breakdown row rendered correctly.
        composeTestRule.onNodeWithText("2").assertExists()
        composeTestRule.onNodeWithText("Ignored").assertExists()
        composeTestRule.onNodeWithText("1.5s").assertExists()
        composeTestRule.onNodeWithText("Highest severity").assertExists()
        composeTestRule.onNodeWithText("Medium").assertExists()
        composeTestRule.onNodeWithText("Avg. confidence").assertExists()
    }

    @Test
    fun scanSummary_isNotShown_whenNull() {
        setScreen(
            SecurityCenterUiState.Loaded(
                protectionStatus = ProtectionStatus.NEEDS_ATTENTION,
                lastScanCompletedAtEpochMillis = 2_000L,
                threats = listOf(threatSummary()),
                scanSummary = null,
            ),
        )

        composeTestRule.onNodeWithText("Needs your attention").assertDoesNotExist()
        composeTestRule.onNodeWithText("Apps scanned").assertDoesNotExist()
    }

    @Test
    fun scanSummary_isNotShown_inTheEmptyOrUnknownStates_evenIfSomehowNonNull() {
        // Defensive coverage: SecurityCenterLoaded only ever renders the
        // scan summary inside the threat-list branch of its own when{}
        // (protectionStatus == UNKNOWN and threats.isEmpty() both take
        // the empty-state branches instead) - this confirms that holds
        // even if a caller passed a non-null scanSummary alongside an
        // empty threats list, which the real ViewModel would never do
        // (scanSummary is only computed from a real lastScan, same as
        // threats), but this test doesn't rely on that ViewModel
        // invariant to pass.
        setScreen(
            SecurityCenterUiState.Loaded(
                protectionStatus = ProtectionStatus.PROTECTED,
                lastScanCompletedAtEpochMillis = 2_000L,
                threats = emptyList(),
                scanSummary = ScanSummary(
                    appsScanned = 10,
                    threatsDetected = 0,
                    trustedApps = 0,
                    ignoredThreats = 0,
                    scanDurationMillis = 500L,
                    highestThreatLabel = "None",
                    averageConfidenceLabel = "None",
                ),
            ),
        )

        composeTestRule.onNodeWithText("All good!").assertDoesNotExist()
        composeTestRule.onNodeWithText("Apps scanned").assertDoesNotExist()
    }
}
