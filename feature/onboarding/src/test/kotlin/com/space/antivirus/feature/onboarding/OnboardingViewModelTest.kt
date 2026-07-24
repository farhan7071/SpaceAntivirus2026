package com.space.antivirus.feature.onboarding

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state starts on the first page`() = runTest {
        val viewModel = OnboardingViewModel()

        assertThat(viewModel.uiState.value.currentPageIndex).isEqualTo(0)
        assertThat(viewModel.uiState.value.totalPages).isEqualTo(OnboardingPages.size)
        assertThat(viewModel.uiState.value.isFirstPage).isTrue()
    }

    @Test
    fun `onNext advances to the next page`() = runTest {
        val viewModel = OnboardingViewModel()

        viewModel.onNext()

        assertThat(viewModel.uiState.value.currentPageIndex).isEqualTo(1)
    }

    @Test
    fun `onNext never advances past the last page`() = runTest {
        val viewModel = OnboardingViewModel()

        repeat(OnboardingPages.size + 5) { viewModel.onNext() }

        assertThat(viewModel.uiState.value.currentPageIndex).isEqualTo(OnboardingPages.size - 1)
        assertThat(viewModel.uiState.value.isLastPage).isTrue()
    }

    @Test
    fun `onBack moves to the previous page`() = runTest {
        val viewModel = OnboardingViewModel()
        viewModel.onNext()
        viewModel.onNext()

        viewModel.onBack()

        assertThat(viewModel.uiState.value.currentPageIndex).isEqualTo(1)
    }

    @Test
    fun `onBack never moves before the first page`() = runTest {
        val viewModel = OnboardingViewModel()

        repeat(5) { viewModel.onBack() }

        assertThat(viewModel.uiState.value.currentPageIndex).isEqualTo(0)
        assertThat(viewModel.uiState.value.isFirstPage).isTrue()
    }

    @Test
    fun `uiState reflects each navigation step in order`() = runTest {
        val viewModel = OnboardingViewModel()

        viewModel.uiState.test {
            assertThat(awaitItem().currentPageIndex).isEqualTo(0)

            viewModel.onNext()
            assertThat(awaitItem().currentPageIndex).isEqualTo(1)

            viewModel.onNext()
            assertThat(awaitItem().currentPageIndex).isEqualTo(2)

            viewModel.onBack()
            assertThat(awaitItem().currentPageIndex).isEqualTo(1)
        }
    }
}
