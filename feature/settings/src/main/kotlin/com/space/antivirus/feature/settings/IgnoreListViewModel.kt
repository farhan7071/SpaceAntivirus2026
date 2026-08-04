package com.space.antivirus.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.space.antivirus.core.model.TrustedItem
import com.space.antivirus.domain.usecase.ObserveTrustedItemsUseCase
import com.space.antivirus.domain.usecase.RemoveTrustedItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The Ignore List — Sprint 043A.
 *
 * Reuses the trusted-item infrastructure entirely: `TrustedItem`
 * (Sprint 012), its Room table, `ObserveTrustedItemsUseCase` and
 * `RemoveTrustedItemUseCase`. No new model, no new storage, no
 * duplicated preference — "Ignore" in Security Center and this list are
 * two views of one table, which is why removing an entry here makes the
 * app flag that package again on the next scan.
 *
 * Same passive Flow shape as every other list ViewModel in the project.
 */
@HiltViewModel
class IgnoreListViewModel @Inject constructor(
    observeTrustedItems: ObserveTrustedItemsUseCase,
    private val removeTrustedItem: RemoveTrustedItemUseCase,
) : ViewModel() {

    val uiState: StateFlow<IgnoreListUiState> = observeTrustedItems()
        .map { items -> IgnoreListUiState.Loaded(items.sortedByDescending { it.addedAtEpochMillis }) }
        .catch<IgnoreListUiState> {
            emit(IgnoreListUiState.Error("Couldn't load your ignore list. Please try again."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = IgnoreListUiState.Loading,
        )

    /**
     * Removing is deliberately immediate and un-confirmed. The action is
     * not destructive — it restores the default (scan this app again) —
     * and re-ignoring is one tap away from the next finding. A
     * confirmation dialog here would be friction protecting nothing.
     */
    fun onRemoveClick(identifier: String) {
        viewModelScope.launch { removeTrustedItem(identifier) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

sealed interface IgnoreListUiState {
    data object Loading : IgnoreListUiState
    data class Loaded(val items: List<TrustedItem>) : IgnoreListUiState
    data class Error(val message: String) : IgnoreListUiState
}
