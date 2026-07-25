package com.space.antivirus.feature.clean

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.CleanableItem
import com.space.antivirus.core.model.ScanScope
import com.space.antivirus.domain.usecase.FindCleanableItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Replaces the Sprint 003 placeholder — the last genuinely outstanding
 * Phase C screen (ADR 0035 built the domain layer this depends on;
 * feature:clean itself was never touched until now).
 *
 * Action-triggered, same shape as ScanViewModel (Sprint 020) rather than
 * the passive Flow-combine shape HomeViewModel/SecurityCenterViewModel/
 * HistoryViewModel use — FindCleanableItemsUseCase is a one-shot file
 * enumeration, not something backed by an ongoing, observable Flow the
 * way persisted scan history is. There's nothing to "subscribe" to here;
 * a junk scan is a user-triggered action with a result, not ambient state
 * to reflect.
 *
 * Display-only, deliberately: no selection, no deletion. ADR 0035
 * explicitly scoped CleanableItem as "candidates only... nothing in this
 * domain layer deletes a file" — no delete-capable UseCase or repository
 * method exists yet anywhere in this project. Building a delete button
 * that doesn't actually delete anything would be exactly the kind of
 * fake production code this project's standing rules prohibit; building
 * real file deletion is real, separate domain work for its own future
 * sprint, not something to fold into "add the Clean screen."
 */
@HiltViewModel
class CleanViewModel @Inject constructor(
    private val findCleanableItems: FindCleanableItemsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CleanUiState>(CleanUiState.Idle)
    val uiState: StateFlow<CleanUiState> = _uiState.asStateFlow()

    fun scanForJunk() {
        // Guard against double-trigger — same pattern and same reasoning
        // as ScanViewModel (ADR 0033): viewModelScope's real dispatcher
        // (Dispatchers.Main.immediate) runs this synchronously up to the
        // first suspension point when called from the main thread, so
        // this check is genuinely protective in production.
        if (_uiState.value is CleanUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = CleanUiState.Loading

            when (val result = findCleanableItems(SCAN_SCOPE)) {
                is AppResult.Success -> _uiState.value = result.data.toLoadedState()
                is AppResult.Failure -> _uiState.value = CleanUiState.Error(messageFor(result.error))
                AppResult.Loading -> _uiState.value =
                    CleanUiState.Error("Something went wrong scanning for junk files.")
            }
        }
    }

    private fun List<CleanableItem>.toLoadedState(): CleanUiState.Loaded = CleanUiState.Loaded(
        items = this,
        totalSizeBytes = sumOf { it.sizeBytes },
    )

    private fun messageFor(error: AppError): String = when (error) {
        is AppError.PermissionMissing -> "Space Antivirus needs storage permission to scan for junk files."
        is AppError.StorageUnavailable -> "Storage is currently unavailable. Please try again."
        else -> "Something went wrong scanning for junk files. Please try again."
    }

    private companion object {
        // Internal storage only, for now — matches FindCleanableItemsUseCaseTest's
        // own default assumption (Sprint 022). A future sprint could let a
        // user choose Downloads/external storage too; the UseCase already
        // takes ScanScope as a parameter specifically so that's a small,
        // additive change, not a redesign.
        val SCAN_SCOPE = ScanScope.InternalStorage
    }
}

sealed interface CleanUiState {
    data object Idle : CleanUiState

    data object Loading : CleanUiState

    data class Loaded(
        val items: List<CleanableItem>,
        val totalSizeBytes: Long,
    ) : CleanUiState

    data class Error(val message: String) : CleanUiState
}
