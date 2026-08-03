package com.space.antivirus.feature.clean

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.CleanableCategory
import com.space.antivirus.core.model.CleanableItem
import com.space.antivirus.core.model.CleaningEvent
import com.space.antivirus.core.model.CleaningProgress
import com.space.antivirus.core.model.CleaningSummary
import com.space.antivirus.core.model.JunkScanProgress
import com.space.antivirus.core.model.StorageStatistics
import com.space.antivirus.core.testing.MainDispatcherRule
import com.space.antivirus.domain.usecase.CleanJunkFilesUseCase
import com.space.antivirus.domain.usecase.GetLastCleanupUseCase
import com.space.antivirus.domain.usecase.GetStorageStatisticsUseCase
import com.space.antivirus.domain.usecase.JunkScanEvent
import com.space.antivirus.domain.usecase.ScanForJunkFilesUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Same proportionate testing choice as before (Sprint 022/038): mockk on
 * the concrete use cases rather than re-exercising JunkFileClassifier's
 * rules, which JunkFileClassifierTest already covers exhaustively.
 *
 * Sprint 039 rewrote this suite: the ViewModel now drives two streaming
 * use cases instead of one suspend call, so the states under test are
 * the real event sequences those flows produce.
 */
class CleanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val scanForJunkFiles = mockk<ScanForJunkFilesUseCase>()
    private val cleanJunkFiles = mockk<CleanJunkFilesUseCase>()
    private val getLastCleanup = mockk<GetLastCleanupUseCase>()
    private val getStorageStatistics = mockk<GetStorageStatisticsUseCase>()

    private fun buildViewModel(): CleanViewModel {
        coEvery { getLastCleanup() } returns AppResult.Success(null)
        coEvery { getStorageStatistics() } returns AppResult.Success(
            StorageStatistics(totalBytes = 100L, freeBytes = 40L),
        )
        return CleanViewModel(scanForJunkFiles, cleanJunkFiles, getLastCleanup, getStorageStatistics)
    }

    private fun cleanableItem(name: String = "notes.bak", sizeBytes: Long = 1_000L) = CleanableItem(
        path = "/data/user/0/com.space.antivirus/cache/$name",
        name = name,
        sizeBytes = sizeBytes,
        category = CleanableCategory.TEMPORARY_FILE,
        reason = "test reason",
    )

    @Test
    fun `initial state is Idle`() {
        every { scanForJunkFiles() } returns flowOf()
        assertThat(buildViewModel().uiState.value).isInstanceOf(CleanUiState.Idle::class.java)
    }

    @Test
    fun `scan emits real progress then loaded results`() = runTest {
        val item = cleanableItem()
        every { scanForJunkFiles() } returns flowOf(
            JunkScanEvent.InProgress(
                JunkScanProgress(filesInspected = 4, junkFound = 1, bytesFound = 1_000L, currentPath = "/a/b.tmp"),
            ),
            JunkScanEvent.Completed(items = listOf(item), totalSizeBytes = 1_000L),
        )
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            skipItems(1)
            viewModel.scanForJunk()

            val scanning = awaitItem()
            assertThat(scanning).isInstanceOf(CleanUiState.Scanning::class.java)

            var latest = awaitItem()
            while (latest is CleanUiState.Scanning && latest.progress.filesInspected == 0) {
                latest = awaitItem()
            }
            assertThat((latest as CleanUiState.Scanning).progress.filesInspected).isEqualTo(4)

            val loaded = awaitItem() as CleanUiState.Loaded
            assertThat(loaded.items).containsExactly(item)
            assertThat(loaded.totalSizeBytes).isEqualTo(1_000L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * An unreadable scope must never render as "your storage is clean" —
     * that would tell the user their device was checked when it wasn't.
     */
    @Test
    fun `scan failure surfaces an error rather than an empty clean result`() = runTest {
        every { scanForJunkFiles() } returns flowOf(JunkScanEvent.Failed(AppError.StorageUnavailable))
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            skipItems(1)
            viewModel.scanForJunk()
            skipItems(1)

            assertThat(awaitItem()).isInstanceOf(CleanUiState.Error::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cleaning emits real progress then a completed summary`() = runTest {
        val item = cleanableItem()
        every { scanForJunkFiles() } returns flowOf(
            JunkScanEvent.Completed(items = listOf(item), totalSizeBytes = 1_000L),
        )
        every { cleanJunkFiles(any()) } returns flowOf(
            CleaningEvent.InProgress(
                CleaningProgress(
                    itemsProcessed = 1,
                    totalItems = 1,
                    itemsDeleted = 1,
                    itemsFailed = 0,
                    bytesFreed = 1_000L,
                    currentItemName = "notes.bak",
                ),
            ),
            CleaningEvent.Completed(
                CleaningSummary(
                    itemsRequested = 1,
                    itemsDeleted = 1,
                    itemsFailed = 0,
                    bytesFreed = 1_000L,
                    durationMillis = 12L,
                    completedAtEpochMillis = 99L,
                    wasCancelled = false,
                ),
            ),
        )
        val viewModel = buildViewModel()
        viewModel.scanForJunk()

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(CleanUiState.Loaded::class.java)
            viewModel.cleanJunk()

            var latest = awaitItem()
            while (latest is CleanUiState.Cleaning && !latest.progress.isComplete) {
                latest = awaitItem()
            }
            val completed = latest as CleanUiState.Completed
            assertThat(completed.summary.bytesFreed).isEqualTo(1_000L)
            assertThat(completed.summary.itemsDeleted).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cleaning is not started when there is nothing to clean`() = runTest {
        every { scanForJunkFiles() } returns flowOf(
            JunkScanEvent.Completed(items = emptyList(), totalSizeBytes = 0L),
        )
        val viewModel = buildViewModel()
        viewModel.scanForJunk()

        viewModel.cleanJunk()

        assertThat(viewModel.uiState.value).isInstanceOf(CleanUiState.Loaded::class.java)
    }

    @Test
    fun `cancelling a scan returns to Idle`() = runTest {
        every { scanForJunkFiles() } returns flowOf(JunkScanEvent.InProgress(JunkScanProgress.STARTING))
        val viewModel = buildViewModel()
        viewModel.scanForJunk()

        viewModel.cancelScan()

        assertThat(viewModel.uiState.value).isInstanceOf(CleanUiState.Idle::class.java)
    }

    @Test
    fun `idle exposes the real last cleanup and storage totals`() = runTest {
        every { scanForJunkFiles() } returns flowOf()
        coEvery { getStorageStatistics() } returns AppResult.Success(
            StorageStatistics(totalBytes = 100L, freeBytes = 40L),
        )
        coEvery { getLastCleanup() } returns AppResult.Success(null)

        val viewModel = buildViewModel()

        val idle = viewModel.uiState.value as CleanUiState.Idle
        assertThat(idle.storage?.totalBytes).isEqualTo(100L)
    }
}
