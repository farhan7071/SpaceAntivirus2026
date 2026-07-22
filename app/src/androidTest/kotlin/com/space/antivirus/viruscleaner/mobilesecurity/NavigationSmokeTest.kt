package com.space.antivirus.viruscleaner.mobilesecurity

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/**
 * Sprint 003's one required end-to-end signal: the app launches and the
 * navigation graph is wired (Task 4 / self-verification "Navigation
 * works"). Deliberately not a feature test — there's no feature yet.
 */
class NavigationSmokeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun onboardingIsTheStartDestination() {
        composeTestRule.onNodeWithText("Onboarding — placeholder (Sprint 004+)").assertExists()
    }
}
