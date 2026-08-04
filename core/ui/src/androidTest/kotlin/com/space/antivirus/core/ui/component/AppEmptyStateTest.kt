package com.space.antivirus.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import org.junit.Rule
import org.junit.Test

/**
 * Sprint 041. This component was an unstyled stub until this sprint, and
 * every empty state in the app renders through it — so the behavior
 * worth locking in is that the optional title is genuinely optional and
 * that both tones render their message.
 */
class AppEmptyStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersTheMessage() {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                AppEmptyState(icon = Icons.Filled.CheckCircle, message = "Nothing here yet.")
            }
        }

        composeTestRule.onNodeWithText("Nothing here yet.").assertExists()
    }

    @Test
    fun rendersTheTitleWhenOneIsSupplied() {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                AppEmptyState(
                    icon = Icons.Filled.CheckCircle,
                    title = "No scans yet",
                    message = "Run a scan to get started.",
                )
            }
        }

        composeTestRule.onNodeWithText("No scans yet").assertExists()
        composeTestRule.onNodeWithText("Run a scan to get started.").assertExists()
    }

    /**
     * A clean result is good news. Before Sprint 041 it rendered behind
     * the same warning triangle as a failure, which is the low-grade
     * version of the risk exaggeration ADR 0015 rules out.
     */
    @Test
    fun rendersAPositiveToneWithoutChangingTheMessage() {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                AppEmptyState(
                    icon = Icons.Filled.CheckCircle,
                    title = "Nothing to review",
                    message = "Your last scan completed and didn't flag anything.",
                    tone = EmptyStateTone.POSITIVE,
                )
            }
        }

        composeTestRule.onNodeWithText("Nothing to review").assertExists()
        composeTestRule.onNodeWithText("Your last scan completed and didn't flag anything.").assertExists()
    }
}
