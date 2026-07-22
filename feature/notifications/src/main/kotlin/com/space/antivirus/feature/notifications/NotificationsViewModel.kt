package com.space.antivirus.feature.notifications

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
class NotificationsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Idle)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()
}

sealed interface NotificationsUiState {
    data object Idle : NotificationsUiState
    data object Loading : NotificationsUiState
    data class Error(val message: String) : NotificationsUiState
}
