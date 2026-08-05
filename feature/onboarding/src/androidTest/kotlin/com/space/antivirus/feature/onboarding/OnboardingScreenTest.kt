package com.space.antivirus.feature.onboarding

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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

    /**
     * Sprint 045 replaced the "1 of 3" label with dots. The count is not
     * lost — it moves into the indicator row's contentDescription, so a
     * screen-reader user still hears the position a sighted user reads
     * from the dots, announced once rather than per dot.
     */
    @Test
    fun pageIndicator_announcesThePositionToScreenReaders() {
        setScreen(pageIndex = 0)

        composeTestRule
            .onNodeWithContentDescription("Page 1 of ${OnboardingPages.size}")
            .assertExists()
    }

    @Test
    fun pageIndicator_announcesLaterPages() {
        setScreen(pageIndex = 1)

        composeTestRule
            .onNodeWithContentDescription("Page 2 of ${OnboardingPages.size}")
            .assertExists()
    }
}
