package com.space.antivirus.feature.onboarding

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Replaces the Sprint 003 placeholder. Deliberately minimal — needs no
 * domain UseCase (onboarding content is static, per OnboardingContent.kt),
 * but kept as a real ViewModel rather than local Compose state for
 * consistency with the pattern ADR 0030 established (every feature screen
 * gets a testable ViewModel) and because navigation bounds-checking
 * (never advancing past the last page or before the first) is exactly
 * the kind of logic that belongs outside Compose, however small.
 *
 * onGetStarted/onboarding-complete is NOT modeled as ViewModel state —
 * navigating to Home is a one-time event triggered from OnboardingRoute
 * via a caller-supplied callback (wired in SpaceAntivirusNavHost), not
 * something a ViewModel should hold a NavController reference to drive
 * itself.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(currentPageIndex = 0, totalPages = OnboardingPages.size),
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNext() {
        _uiState.update { current ->
            current.copy(currentPageIndex = (current.currentPageIndex + 1).coerceAtMost(current.totalPages - 1))
        }
    }

    fun onBack() {
        _uiState.update { current ->
            current.copy(currentPageIndex = (current.currentPageIndex - 1).coerceAtLeast(0))
        }
    }
}

data class OnboardingUiState(
    val currentPageIndex: Int,
    val totalPages: Int,
) {
    val isFirstPage: Boolean get() = currentPageIndex == 0
    val isLastPage: Boolean get() = currentPageIndex == totalPages - 1
}
