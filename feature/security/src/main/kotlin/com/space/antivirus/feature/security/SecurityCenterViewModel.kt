package com.space.antivirus.feature.security

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
class SecurityCenterViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<SecurityCenterUiState>(SecurityCenterUiState.Idle)
    val uiState: StateFlow<SecurityCenterUiState> = _uiState.asStateFlow()
}

sealed interface SecurityCenterUiState {
    data object Idle : SecurityCenterUiState
    data object Loading : SecurityCenterUiState
    data class Error(val message: String) : SecurityCenterUiState
}
