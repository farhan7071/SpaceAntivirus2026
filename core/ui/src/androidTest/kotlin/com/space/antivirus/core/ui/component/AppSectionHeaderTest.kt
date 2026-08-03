package com.space.antivirus.core.ui.component

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import org.junit.Rule
import org.junit.Test

/**
 * Sprint 038. Covers the one behavior worth locking in: the trailing
 * action is genuinely optional, and a half-specified action (text with
 * no handler, or a handler with no text) renders nothing rather than an
 * inert control.
 */
class AppSectionHeaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersTheTitle() {
        composeTestRule.setContent {
            SpaceAntivirusTheme { AppSectionHeader(title = "Junk breakdown") }
        }

        composeTestRule.onNodeWithText("Junk breakdown").assertExists()
    }

    @Test
    fun withoutAnAction_rendersNoTrailingButton() {
        composeTestRule.setContent {
            SpaceAntivirusTheme { AppSectionHeader(title = "Recent Activity") }
        }

        composeTestRule.onNodeWithText("See all").assertDoesNotExist()
    }

    @Test
    fun withAnAction_rendersItAndForwardsClicks() {
        var clicked = false
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                AppSectionHeader(
                    title = "Recent Activity",
                    actionText = "See all",
                    onActionClick = { clicked = true },
                )
            }
        }

        composeTestRule.onNodeWithText("See all").performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun withActionTextButNoHandler_rendersNoTrailingButton() {
        composeTestRule.setContent {
            SpaceAntivirusTheme { AppSectionHeader(title = "Recent Activity", actionText = "See all") }
        }

        composeTestRule.onNodeWithText("See all").assertDoesNotExist()
    }
}
