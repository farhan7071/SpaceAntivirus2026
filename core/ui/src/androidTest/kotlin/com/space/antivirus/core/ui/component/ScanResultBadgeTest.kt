package com.space.antivirus.core.ui.component

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import org.junit.Rule
import org.junit.Test

/**
 * Sprint 034 (Part 7) — a scan session's own result badge, distinct
 * from StatusChip for the reasons this component's own KDoc explains in
 * full: fixes a real, pre-existing bug (StatusChip(Severity.INFO) being
 * pressed into service for clean sessions, a concept it was never meant
 * to cover).
 */
class ScanResultBadgeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setBadge(isClean: Boolean, highestSeverity: Severity) {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                ScanResultBadge(isClean = isClean, highestSeverity = highestSeverity)
            }
        }
    }

    @Test
    fun aCleanSession_showsSafe_notAnySeverityLabel() {
        setBadge(isClean = true, highestSeverity = Severity.INFO)

        composeTestRule.onNodeWithText("Safe").assertExists()
        // The real, pre-existing bug this component fixes: "Informational"
        // (Severity.INFO's own label) must never appear for a clean
        // session, regardless of which highestSeverity value happens to
        // be passed in - isClean alone determines the badge shown.
        composeTestRule.onNodeWithText("Informational").assertDoesNotExist()
    }

    @Test
    fun aSessionWithFindings_delegatesToStatusChip_showingTheHighestSeverity() {
        setBadge(isClean = false, highestSeverity = Severity.ACTION_NEEDED)

        composeTestRule.onNodeWithText("High Risk").assertExists()
        composeTestRule.onNodeWithText("Safe").assertDoesNotExist()
    }

    @Test
    fun aSessionWithOnlyInformationalFindings_showsInformational_notSafe() {
        // isClean = false but the highest severity present is still just
        // INFO - this must show Severity's own "Informational" label,
        // not the Safe badge, since findings genuinely exist.
        setBadge(isClean = false, highestSeverity = Severity.INFO)

        composeTestRule.onNodeWithText("Informational").assertExists()
        composeTestRule.onNodeWithText("Safe").assertDoesNotExist()
    }
}
