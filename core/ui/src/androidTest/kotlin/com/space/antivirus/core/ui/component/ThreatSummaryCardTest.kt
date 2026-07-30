package com.space.antivirus.core.ui.component

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import org.junit.Rule
import org.junit.Test

/**
 * The first real Compose UI test in core:ui — ThreatSummaryCard (Sprint
 * 030) is the first component here substantial enough to warrant one.
 * Same pattern every feature screen test has used since Sprint 017
 * (ADR 0030): hand-built parameters, no Hilt infrastructure needed.
 */
class ThreatSummaryCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setCard(
        appLabel: String = "Example App",
        packageName: String = "com.example.app",
        severity: Severity = Severity.ATTENTION,
        threatCategory: String = "Permission Usage",
        evidenceIcons: Set<EvidenceIcon> = setOf(EvidenceIcon.CAMERA),
        shortSummary: String = "Can record and transmit media.",
        technicalDetail: String = "Full technical explanation goes here.",
        evidenceBullets: List<String> = listOf("Camera access", "Microphone access"),
        recommendation: String = "Review if unexpected.",
        confidenceLabel: String = "Medium",
        onIgnoreClick: () -> Unit = {},
        onOpenAppInfoClick: () -> Unit = {},
        onUninstallClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                ThreatSummaryCard(
                    appLabel = appLabel,
                    packageName = packageName,
                    severity = severity,
                    threatCategory = threatCategory,
                    evidenceIcons = evidenceIcons,
                    shortSummary = shortSummary,
                    technicalDetail = technicalDetail,
                    evidenceBullets = evidenceBullets,
                    recommendation = recommendation,
                    confidenceLabel = confidenceLabel,
                    onIgnoreClick = onIgnoreClick,
                    onOpenAppInfoClick = onOpenAppInfoClick,
                    onUninstallClick = onUninstallClick,
                )
            }
        }
    }

    @Test
    fun identityIsShownFirst_appNameAndPackageAlwaysVisible() {
        setCard(appLabel = "Chrome", packageName = "com.android.chrome")

        composeTestRule.onNodeWithText("Chrome").assertExists()
        composeTestRule.onNodeWithText("com.android.chrome").assertExists()
    }

    @Test
    fun collapsedByDefault_technicalDetailAndEvidenceBulletsNotShown() {
        setCard(technicalDetail = "The full technical explanation", evidenceBullets = listOf("Camera access"))

        composeTestRule.onNodeWithText("The full technical explanation").assertDoesNotExist()
        composeTestRule.onNodeWithText("Camera access").assertDoesNotExist()
    }

    @Test
    fun shortSummaryIsAlwaysVisible_evenWhenCollapsed() {
        setCard(shortSummary = "Can record and transmit media.")

        composeTestRule.onNodeWithText("Can record and transmit media.").assertExists()
    }

    @Test
    fun tappingViewDetails_expandsToShowTechnicalDetailEvidenceAndRecommendation() {
        setCard(
            threatCategory = "Permission Usage",
            technicalDetail = "The full technical explanation",
            evidenceBullets = listOf("Camera access", "Microphone access"),
            recommendation = "Review if unexpected.",
        )

        composeTestRule.onNodeWithText("View details").performClick()

        composeTestRule.onNodeWithText("Threat Category: Permission Usage").assertExists()
        composeTestRule.onNodeWithText("The full technical explanation").assertExists()
        // Sprint 034 (Part 4): each evidence bullet is now a row with a
        // short icon title above its own full text, not a "• text"
        // bullet line — both the inferred title and the bullet's own
        // unmodified text are checked.
        composeTestRule.onNodeWithText("Camera").assertExists()
        composeTestRule.onNodeWithText("Camera access").assertExists()
        composeTestRule.onNodeWithText("Microphone").assertExists()
        composeTestRule.onNodeWithText("Microphone access").assertExists()
        composeTestRule.onNodeWithText("Review if unexpected.").assertExists()
    }

    @Test
    fun theThreatCategory_onlyAppearsAfterExpanding_sameAsTheRestOfTheExpandedDetail() {
        setCard(threatCategory = "Malware")

        composeTestRule.onNodeWithText("Threat Category: Malware").assertDoesNotExist()

        composeTestRule.onNodeWithText("View details").performClick()

        composeTestRule.onNodeWithText("Threat Category: Malware").assertExists()
    }

    @Test
    fun theConfidenceLabel_onlyAppearsAfterExpanding_sameAsTheRestOfTheExpandedDetail() {
        setCard(confidenceLabel = "Low")

        composeTestRule.onNodeWithText("Confidence: Low").assertDoesNotExist()

        composeTestRule.onNodeWithText("View details").performClick()

        composeTestRule.onNodeWithText("Confidence: Low").assertExists()
    }

    @Test
    fun tappingViewDetailsTwice_collapsesAgain() {
        setCard(technicalDetail = "The full technical explanation")

        composeTestRule.onNodeWithText("View details").performClick()
        composeTestRule.onNodeWithText("Hide details").performClick()

        composeTestRule.onNodeWithText("The full technical explanation").assertDoesNotExist()
        composeTestRule.onNodeWithText("View details").assertExists()
    }

    @Test
    fun tappingMoreActions_thenIgnore_invokesOnIgnoreClick() {
        var ignored = false
        setCard(onIgnoreClick = { ignored = true })

        composeTestRule.onNode(hasContentDescription("More actions")).performClick()
        composeTestRule.onNodeWithText("Ignore").performClick()

        assertThat(ignored).isTrue()
    }

    @Test
    fun tappingMoreActions_thenOpenAppInfo_invokesOnOpenAppInfoClick() {
        var opened = false
        setCard(onOpenAppInfoClick = { opened = true })

        composeTestRule.onNode(hasContentDescription("More actions")).performClick()
        composeTestRule.onNodeWithText("Open app info").performClick()

        assertThat(opened).isTrue()
    }

    @Test
    fun tappingMoreActions_thenUninstall_invokesOnUninstallClick() {
        var uninstalled = false
        setCard(onUninstallClick = { uninstalled = true })

        composeTestRule.onNode(hasContentDescription("More actions")).performClick()
        composeTestRule.onNodeWithText("Uninstall").performClick()

        assertThat(uninstalled).isTrue()
    }

    @Test
    fun noEvidenceIcons_rendersWithoutError_iconRowSimplyOmitted() {
        // Defensive coverage — a Detection this project's analyzers
        // produce should always yield at least one icon (falling back to
        // EvidenceIcon.OTHER), but the card itself must not assume a
        // non-empty set.
        setCard(evidenceIcons = emptySet())

        composeTestRule.onNodeWithText("Example App").assertExists()
    }

    @Test
    fun theRecommendationSection_stillShowsItsOwnTitle_inTheRedesignedCard() {
        setCard(recommendation = "Verify this is expected.")

        composeTestRule.onNodeWithText("View details").performClick()

        composeTestRule.onNodeWithText("Recommendation").assertExists()
        composeTestRule.onNodeWithText("Verify this is expected.").assertExists()
    }

    @Test
    fun anEvidenceBulletMatchingNoKnownKeyword_fallsBackToThePermissionTitle() {
        setCard(evidenceBullets = listOf("Requests an unusual configuration."))

        composeTestRule.onNodeWithText("View details").performClick()

        // EvidenceIcon.OTHER's title (Sprint 034) — the same fallback
        // EvidenceIcon.inferFrom already used for its icon before this
        // sprint added titles to the enum.
        composeTestRule.onNodeWithText("Permission").assertExists()
        composeTestRule.onNodeWithText("Requests an unusual configuration.").assertExists()
    }
}
