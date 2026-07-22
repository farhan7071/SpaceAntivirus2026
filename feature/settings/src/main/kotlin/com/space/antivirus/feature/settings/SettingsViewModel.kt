package com.space.antivirus.feature.settings

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
class SettingsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
}

sealed interface SettingsUiState {
    data object Idle : SettingsUiState
    data object Loading : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}
