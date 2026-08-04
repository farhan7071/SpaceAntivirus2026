package com.space.antivirus.core.ui.component

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import org.junit.Rule
import org.junit.Test

/**
 * Sprint 034 (Part 1) — the dashboard scan summary. Same pattern
 * ThreatSummaryCardTest already established for this module: hand-built
 * parameters, no Hilt infrastructure needed.
 */
class ScanSummaryCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setCard(
        isProtected: Boolean = true,
        lastScanText: String = "Today, 10:30 AM",
        appsScanned: Int = 467,
        findingsCount: Int = 0,
        trustedCount: Int = 421,
        infoCount: Int = 0,
        attentionCount: Int = 0,
        highRiskCount: Int = 0,
        ignoredCount: Int = 0,
        scanDurationLabel: String = "2.3s",
        highestSeverity: Severity? = null,
        averageConfidenceLabel: String = "None",
    ) {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                ScanSummaryCard(
                    isProtected = isProtected,
                    lastScanText = lastScanText,
                    appsScanned = appsScanned,
                    findingsCount = findingsCount,
                    trustedCount = trustedCount,
                    infoCount = infoCount,
                    attentionCount = attentionCount,
                    highRiskCount = highRiskCount,
                    ignoredCount = ignoredCount,
                    scanDurationLabel = scanDurationLabel,
                    highestSeverity = highestSeverity,
                    averageConfidenceLabel = averageConfidenceLabel,
                )
            }
        }
    }

    @Test
    fun whenProtected_showsTheAllGoodMessage() {
        setCard(isProtected = true)

        composeTestRule.onNodeWithText("All good!").assertExists()
    }

    @Test
    fun whenNotProtected_showsTheNeedsAttentionMessage() {
        setCard(isProtected = false)

        composeTestRule.onNodeWithText("Needs your attention").assertExists()
    }

    @Test
    fun showsTheLastScanText() {
        setCard(lastScanText = "Yesterday, 9:15 PM")

        composeTestRule.onNodeWithText("Last scan: Yesterday, 9:15 PM").assertExists()
    }

    /**
     * Sprint 041 rebuilt this card for hierarchy. The point of this test
     * is unchanged and, if anything, more important now: every figure
     * that was visible before must still be visible. Three of them moved
     * from large stat columns to quiet secondary lines — moved, not
     * dropped.
     */
    @Test
    fun showsEveryStatValueAndItsLabel() {
        setCard(
            appsScanned = 467,
            findingsCount = 42,
            trustedCount = 421,
            infoCount = 3,
            attentionCount = 4,
            highRiskCount = 5,
            ignoredCount = 6,
            scanDurationLabel = "2.3s",
            highestSeverity = Severity.ACTION_NEEDED,
            averageConfidenceLabel = "Low",
        )

        // Primary row.
        composeTestRule.onNodeWithText("467").assertExists()
        composeTestRule.onNodeWithText("Apps scanned").assertExists()
        composeTestRule.onNodeWithText("42").assertExists()
        composeTestRule.onNodeWithText("Findings").assertExists()
        composeTestRule.onNodeWithText("2.3s").assertExists()
        composeTestRule.onNodeWithText("Duration").assertExists()

        // Highest severity, now a real badge carrying Severity's label.
        composeTestRule.onNodeWithText("Highest severity").assertExists()
        composeTestRule.onNodeWithText("High Risk").assertExists()

        // Demoted but still present, and still accurate.
        composeTestRule.onNodeWithText("3 informational \u00B7 4 attention \u00B7 5 high risk").assertExists()
        composeTestRule.onNodeWithText("421 trusted \u00B7 6 ignored \u00B7 Low confidence").assertExists()
    }

    /** "Highest severity: None" is noise on a clean result. */
    @Test
    fun omitsTheHighestSeverityRow_whenThereIsNothingToRank() {
        setCard(isProtected = true, findingsCount = 0, highestSeverity = null)

        composeTestRule.onNodeWithText("Highest severity").assertDoesNotExist()
    }

    /** The severity breakdown line is only meaningful once something was
     *  found; three zeroes on a clean result is noise. */
    @Test
    fun omitsTheSeverityBreakdown_whenThereAreNoFindings() {
        setCard(isProtected = true, findingsCount = 0)

        composeTestRule.onNodeWithText("0 informational \u00B7 0 attention \u00B7 0 high risk")
            .assertDoesNotExist()
    }
}
