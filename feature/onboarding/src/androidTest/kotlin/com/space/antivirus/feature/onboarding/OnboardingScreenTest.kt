package com.space.antivirus.feature.onboarding

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
 * Tests the stateless OnboardingScreen directly with hand-built
 * OnboardingUiState + callbacks, same pattern HomeScreenTest established
 * (ADR 0030) — no Hilt test infrastructure needed.
 */
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(
        pageIndex: Int,
        onNext: () -> Unit = {},
        onBack: () -> Unit = {},
        onGetStarted: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                OnboardingScreen(
                    uiState = OnboardingUiState(currentPageIndex = pageIndex, totalPages = OnboardingPages.size),
                    onNext = onNext,
                    onBack = onBack,
                    onGetStarted = onGetStarted,
                )
            }
        }
    }

    @Test
    fun firstPage_showsItsHeadlineAndNextButton_notGetStarted() {
        setScreen(pageIndex = 0)

        composeTestRule.onNodeWithText(OnboardingPages[0].headline).assertExists()
        composeTestRule.onNodeWithText("Next").assertExists()
        composeTestRule.onNodeWithText("Get Started").assertDoesNotExist()
    }

    @Test
    fun lastPage_showsGetStartedButton_notNext() {
        setScreen(pageIndex = OnboardingPages.size - 1)

        composeTestRule.onNodeWithText(OnboardingPages.last().headline).assertExists()
        composeTestRule.onNodeWithText("Get Started").assertExists()
        composeTestRule.onNodeWithText("Next").assertDoesNotExist()
    }

    @Test
    fun aMiddlePage_showsBothBackAndNextButtons() {
        setScreen(pageIndex = 1)

        composeTestRule.onNodeWithText("Back").assertExists()
        composeTestRule.onNodeWithText("Next").assertExists()
    }

    @Test
    fun tappingNext_invokesTheOnNextCallback() {
        var nextTapped = false
        setScreen(pageIndex = 0, onNext = { nextTapped = true })

        composeTestRule.onNodeWithText("Next").performClick()

        assertThat(nextTapped).isTrue()
    }

    @Test
    fun tappingBack_invokesTheOnBackCallback() {
        var backTapped = false
        setScreen(pageIndex = 1, onBack = { backTapped = true })

        composeTestRule.onNodeWithText("Back").performClick()

        assertThat(backTapped).isTrue()
    }

    @Test
    fun tappingGetStarted_invokesTheOnGetStartedCallback() {
        var completed = false
        setScreen(pageIndex = OnboardingPages.size - 1, onGetStarted = { completed = true })

        composeTestRule.onNodeWithText("Get Started").performClick()

        assertThat(completed).isTrue()
    }

    @Test
    fun everyPageBodyText_isDisplayedWhenNavigatedTo() {
        OnboardingPages.forEachIndexed { index, page ->
            setScreen(pageIndex = index)
            composeTestRule.onNodeWithText(page.body).assertExists()
        }
    }
}
