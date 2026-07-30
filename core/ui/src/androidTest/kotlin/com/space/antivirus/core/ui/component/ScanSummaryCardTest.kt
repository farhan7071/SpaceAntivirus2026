package com.space.antivirus.core.ui.component

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
        highestSeverityLabel: String = "None",
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
                    highestSeverityLabel = highestSeverityLabel,
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
            // Deliberately "Action Needed", not "Attention"/"High Risk" -
            // this comes from RiskLevel.toDisplayLabel() upstream
            // (SecurityCenterViewModel), a genuinely different label set
            // from Severity's own ("Informational"/"Attention"/"High
            // Risk", Part 3's relabeling) - using a real value here also
            // avoids colliding with the breakdown row's own "Attention"/
            // "High Risk" labels asserted below.
            highestSeverityLabel = "Action Needed",
            averageConfidenceLabel = "Low",
        )

        composeTestRule.onNodeWithText("467").assertExists()
        composeTestRule.onNodeWithText("Apps scanned").assertExists()
        composeTestRule.onNodeWithText("42").assertExists()
        composeTestRule.onNodeWithText("Findings").assertExists()
        composeTestRule.onNodeWithText("421").assertExists()
        composeTestRule.onNodeWithText("Trusted").assertExists()
        composeTestRule.onNodeWithText("3").assertExists()
        composeTestRule.onNodeWithText("Info").assertExists()
        composeTestRule.onNodeWithText("4").assertExists()
        composeTestRule.onNodeWithText("Attention").assertExists()
        composeTestRule.onNodeWithText("5").assertExists()
        composeTestRule.onNodeWithText("High Risk").assertExists()
        composeTestRule.onNodeWithText("6").assertExists()
        composeTestRule.onNodeWithText("Ignored").assertExists()
        composeTestRule.onNodeWithText("2.3s").assertExists()
        composeTestRule.onNodeWithText("Scan duration").assertExists()
        composeTestRule.onNodeWithText("Action Needed").assertExists()
        composeTestRule.onNodeWithText("Highest severity").assertExists()
        composeTestRule.onNodeWithText("Low").assertExists()
        composeTestRule.onNodeWithText("Avg. confidence").assertExists()
    }

    @Test
    fun allStatsCanBeZero_withoutError() {
        // Defensive coverage - a fresh install's first scan, or a scan
        // that genuinely found and ignored/trusted nothing.
        setCard(
            appsScanned = 0,
            findingsCount = 0,
            trustedCount = 0,
            infoCount = 0,
            attentionCount = 0,
            highRiskCount = 0,
            ignoredCount = 0,
        )

        composeTestRule.onNodeWithText("All good!").assertExists()
    }
}
