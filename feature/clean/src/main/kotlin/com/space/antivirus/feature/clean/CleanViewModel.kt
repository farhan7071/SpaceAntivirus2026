package com.space.antivirus.feature.clean

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.CleanableItem
import com.space.antivirus.core.model.CleaningEvent
import com.space.antivirus.core.model.CleaningProgress
import com.space.antivirus.core.model.CleaningSummary
import com.space.antivirus.core.model.CleanupRecord
import com.space.antivirus.core.model.JunkScanProgress
import com.space.antivirus.core.model.StorageStatistics
import com.space.antivirus.domain.usecase.CleanJunkFilesUseCase
import com.space.antivirus.domain.usecase.GetLastCleanupUseCase
import com.space.antivirus.domain.usecase.GetStorageStatisticsUseCase
import com.space.antivirus.domain.usecase.JunkScanEvent
import com.space.antivirus.domain.usecase.ScanForJunkFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Sprint 039 — the Cleaner is now backed by a real cleaning engine.
 *
 * Sprint 038's version could only scan-and-display, because nothing in
 * the project deleted a file. That is no longer true: this ViewModel
 * drives a real streaming scan (`ScanForJunkFilesUseCase`) and real
 * deletion (`CleanJunkFilesUseCase`), and every number it puts into
 * `CleanUiState` came out of one of those two use cases.
 *
 * **Cancellation is a held Job, not a flag.** `scanJob`/`cleanJob` are
 * cancelled directly, which propagates into the enumeration walk and the
 * deletion loop where both check for it between items. A boolean the
 * loop consulted would be a slower, less honest imitation of what
 * structured concurrency already does properly.
 *
 * **No business logic here.** Grouping, byte formatting and copy live in
 * `CleanScreen.kt` (presentation) or the use cases (domain); this class
 * only maps use-case events onto UI states and holds the cancellation
 * handles.
 */
@HiltViewModel
class CleanViewModel @Inject constructor(
    private val scanForJunkFiles: ScanForJunkFilesUseCase,
    private val cleanJunkFiles: CleanJunkFilesUseCase,
    private val getLastCleanup: GetLastCleanupUseCase,
    private val getStorageStatistics: GetStorageStatisticsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CleanUiState>(CleanUiState.Idle())
    val uiState: StateFlow<CleanUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var cleanJob: Job? = null

    init {
        refreshIdleContext()
    }

    /**
     * Loads the two ambient facts the Idle screen shows — last cleanup
     * and storage totals. Both are best-effort: a failure leaves the
     * corresponding line absent rather than turning the whole screen
     * into an error, since neither is required to run a scan.
     */
    private fun refreshIdleContext() {
        viewModelScope.launch {
            val lastCleanup = (getLastCleanup() as? AppResult.Success)?.data
            val storage = (getStorageStatistics() as? AppResult.Success)?.data
            val current = _uiState.value
            if (current is CleanUiState.Idle) {
                _uiState.value = current.copy(lastCleanup = lastCleanup, storage = storage)
            }
        }
    }

    fun scanForJunk() {
        // Same double-trigger guard and same reasoning as ScanViewModel
        // (ADR 0033): viewModelScope's Dispatchers.Main.immediate runs
        // this synchronously up to the first suspension point, so the
        // check is genuinely protective in production.
        if (_uiState.value is CleanUiState.Scanning || _uiState.value is CleanUiState.Cleaning) return

        scanJob = viewModelScope.launch {
            _uiState.value = CleanUiState.Scanning(JunkScanProgress.STARTING)

            scanForJunkFiles()
                .catch { _uiState.value = CleanUiState.Error(GENERIC_SCAN_ERROR) }
                .collect { event ->
                    when (event) {
                        is JunkScanEvent.InProgress ->
                            _uiState.value = CleanUiState.Scanning(event.progress)
                        is JunkScanEvent.Completed ->
                            _uiState.value = CleanUiState.Loaded(
                                items = event.items,
                                totalSizeBytes = event.totalSizeBytes,
                                storage = loadStorage(),
                            )
                        is JunkScanEvent.Failed ->
                            _uiState.value = CleanUiState.Error(messageFor(event.error))
                    }
                }
        }
    }

    /** Returns to Idle. A cancelled scan found nothing the user asked to
     *  act on, so there is nothing to show — and reporting partial scan
     *  results as if the scan had finished would misstate them. */
    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        _uiState.value = CleanUiState.Idle()
        refreshIdleContext()
    }

    fun cleanJunk() {
        val loaded = _uiState.value as? CleanUiState.Loaded ?: return
        if (loaded.items.isEmpty()) return

        cleanJob = viewModelScope.launch {
            _uiState.value = CleanUiState.Cleaning(CleaningProgress.starting(loaded.items.size))

            try {
                cleanJunkFiles(loaded.items)
                    .catch { _uiState.value = CleanUiState.Error(GENERIC_CLEAN_ERROR) }
                    .collect { event ->
                        when (event) {
                            is CleaningEvent.InProgress ->
                                _uiState.value = CleanUiState.Cleaning(event.progress)
                            is CleaningEvent.Completed ->
                                _uiState.value = CleanUiState.Completed(
                                    summary = event.summary,
                                    storage = loadStorage(),
                                )
                        }
                    }
            } catch (cancellation: CancellationException) {
                // The use case persists a real record before rethrowing,
                // so the honest thing to show is that record — the files
                // deleted before Stop genuinely are gone.
                showCancelledOutcome(loaded.items.size)
                throw cancellation
            }
        }
    }

    fun cancelClean() {
        cleanJob?.cancel()
        cleanJob = null
    }

    /** Back to a fresh Idle screen, with the just-written cleanup record
     *  now available as "Last cleanup". */
    fun dismissCompletion() {
        _uiState.value = CleanUiState.Idle()
        refreshIdleContext()
    }

    private suspend fun loadStorage(): StorageStatistics? =
        (getStorageStatistics() as? AppResult.Success)?.data

    /**
     * Reads back what the cancelled cleanup actually achieved rather
     * than reconstructing it from the last progress emission — the
     * persisted record is the authoritative account, written by the use
     * case inside NonCancellable precisely so it survives this path.
     */
    private fun showCancelledOutcome(itemsRequested: Int) {
        viewModelScope.launch {
            val record = (getLastCleanup() as? AppResult.Success)?.data
            _uiState.value = CleanUiState.Completed(
                summary = record.toSummary(itemsRequested),
                storage = loadStorage(),
            )
        }
    }

    private fun CleanupRecord?.toSummary(itemsRequested: Int): CleaningSummary = CleaningSummary(
        itemsRequested = itemsRequested,
        itemsDeleted = this?.itemsDeleted ?: 0,
        itemsFailed = this?.itemsFailed ?: 0,
        bytesFreed = this?.bytesFreed ?: 0L,
        durationMillis = this?.durationMillis ?: 0L,
        completedAtEpochMillis = this?.completedAtEpochMillis ?: System.currentTimeMillis(),
        wasCancelled = true,
    )

    private companion object {
        const val GENERIC_SCAN_ERROR = "Something went wrong scanning for junk files. Please try again."
        const val GENERIC_CLEAN_ERROR = "Something went wrong while cleaning. Please try again."
    }
}

/**
 * Sprint 039 replaced `Loading` with `Scanning(progress)` and added
 * `Cleaning`/`Completed`. `Loading` carried no data because nothing
 * could report any; all three of these carry real, measured values.
 */
sealed interface CleanUiState {

    /** `lastCleanup` and `storage` are null until loaded, and stay null
     *  if unavailable — an absent line, never a placeholder value. */
    data class Idle(
        val lastCleanup: CleanupRecord? = null,
        val storage: StorageStatistics? = null,
    ) : CleanUiState

    data class Scanning(val progress: JunkScanProgress) : CleanUiState

    data class Loaded(
        val items: List<CleanableItem>,
        val totalSizeBytes: Long,
        val storage: StorageStatistics? = null,
    ) : CleanUiState

    data class Cleaning(val progress: CleaningProgress) : CleanUiState

    data class Completed(
        val summary: CleaningSummary,
        val storage: StorageStatistics? = null,
    ) : CleanUiState

    data class Error(val message: String) : CleanUiState
}

/** Kept for the one place a raw AppError still reaches the UI layer. */
internal fun messageFor(error: AppError): String = when (error) {
    is AppError.PermissionMissing -> "Space Antivirus doesn't have access to that location."
    is AppError.StorageUnavailable -> "Storage is currently unavailable. Please try again."
    else -> "Something went wrong. Please try again."
}
