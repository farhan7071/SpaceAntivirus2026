package com.space.antivirus.feature.home

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Establishes the ViewModel-test pattern (Task 13) every feature module
 * follows from Sprint 004 onward: MainDispatcherRule + StateFlow assertion,
 * no Android framework dependency needed since the ViewModel itself has
 * none yet.
 */
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state is Idle`() = runTest {
        val viewModel = HomeViewModel()
        assertThat(viewModel.uiState.value).isEqualTo(HomeUiState.Idle)
    }
}
