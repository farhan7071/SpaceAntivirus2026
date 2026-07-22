package com.space.antivirus.feature.clean

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
class CleanViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<CleanUiState>(CleanUiState.Idle)
    val uiState: StateFlow<CleanUiState> = _uiState.asStateFlow()
}

sealed interface CleanUiState {
    data object Idle : CleanUiState
    data object Loading : CleanUiState
    data class Error(val message: String) : CleanUiState
}
