package com.space.antivirus.core.ui.component

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import org.junit.Rule
import org.junit.Test

/**
 * Sprint 034 (Part 3): confirms each of the three Severity tiers still
 * renders its own correct, relabeled text ("Informational"/"Attention"/
 * "High Risk" — this sprint's own relabeling from "Info"/"Attention"/
 * "Action needed"). The icon each tier now carries alongside its label
 * isn't independently asserted here — Compose UI tests match on text,
 * and an Icon composable has no text of its own to query; the icon's
 * presence is a visual, not a semantic, property this test framework
 * isn't the right tool to verify.
 */
class StatusChipTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setChip(severity: Severity) {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                StatusChip(severity)
            }
        }
    }

    @Test
    fun infoTier_showsInformational() {
        setChip(Severity.INFO)

        composeTestRule.onNodeWithText("Informational").assertExists()
    }

    @Test
    fun attentionTier_showsAttention() {
        setChip(Severity.ATTENTION)

        composeTestRule.onNodeWithText("Attention").assertExists()
    }

    @Test
    fun actionNeededTier_showsHighRisk() {
        setChip(Severity.ACTION_NEEDED)

        composeTestRule.onNodeWithText("High Risk").assertExists()
    }
}
