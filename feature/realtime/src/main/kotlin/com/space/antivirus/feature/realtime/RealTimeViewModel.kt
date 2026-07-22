package com.space.antivirus.feature.realtime

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
class RealTimeViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<RealTimeUiState>(RealTimeUiState.Idle)
    val uiState: StateFlow<RealTimeUiState> = _uiState.asStateFlow()
}

sealed interface RealTimeUiState {
    data object Idle : RealTimeUiState
    data object Loading : RealTimeUiState
    data class Error(val message: String) : RealTimeUiState
}
