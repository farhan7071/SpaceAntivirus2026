package com.space.antivirus.feature.premium

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Empty ViewModel establishing the StateFlow + sealed UiState pattern
 * (Sprint 002 §7) every feature module follows. No use case is injected
 * yet — Sprint 003 has no business logic to call.
 */
@HiltViewModel
class PremiumViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<PremiumUiState>(PremiumUiState.Idle)
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()
}

sealed interface PremiumUiState {
    data object Idle : PremiumUiState
    data object Loading : PremiumUiState
    data class Error(val message: String) : PremiumUiState
}
