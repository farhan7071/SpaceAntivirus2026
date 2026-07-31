package com.space.antivirus.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import org.junit.Rule
import org.junit.Test

/**
 * Sprint 036 — AppStatCard, extracted from ScanSummaryCard's own
 * previously-private StatColumn pattern specifically to be genuinely
 * reusable (see this component's own KDoc). First real consumer is
 * HomeScreen's Security Summary section.
 */
class AppStatCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsTheValueAndLabel() {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                AppStatCard(value = "42", label = "Threats Found")
            }
        }

        composeTestRule.onNodeWithText("42").assertExists()
        composeTestRule.onNodeWithText("Threats Found").assertExists()
    }

    @Test
    fun rendersWithoutAnIcon_whenNoneIsProvided() {
        // icon defaults to null - this must not crash or require one.
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                AppStatCard(value = "5", label = "Trusted Items")
            }
        }

        composeTestRule.onNodeWithText("5").assertExists()
        composeTestRule.onNodeWithText("Trusted Items").assertExists()
    }

    @Test
    fun rendersWithAnIcon_whenOneIsProvided() {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                AppStatCard(value = "3", label = "Findings", icon = Icons.Default.Warning)
            }
        }

        // The icon itself has no text to assert on directly (contentDescription
        // is null, matching every other purely-decorative icon in this
        // project's own icon+label pattern) - reaching this line without
        // an exception is the assertion that passing a non-null icon
        // doesn't break rendering.
        composeTestRule.onNodeWithText("3").assertExists()
        composeTestRule.onNodeWithText("Findings").assertExists()
    }

    @Test
    fun differentValuesForDifferentInstances_eachShowTheirOwnValue() {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                Column {
                    AppStatCard(value = "467", label = "Apps Scanned")
                    AppStatCard(value = "0", label = "Threats Found")
                }
            }
        }

        composeTestRule.onNodeWithText("467").assertExists()
        composeTestRule.onNodeWithText("Apps Scanned").assertExists()
        composeTestRule.onNodeWithText("0").assertExists()
        composeTestRule.onNodeWithText("Threats Found").assertExists()
    }
}
