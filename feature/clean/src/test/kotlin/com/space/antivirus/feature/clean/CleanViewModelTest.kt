package com.space.antivirus.feature.clean

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.CleanableCategory
import com.space.antivirus.core.model.CleanableItem
import com.space.antivirus.core.model.ScanScope
import com.space.antivirus.core.testing.MainDispatcherRule
import com.space.antivirus.domain.usecase.FindCleanableItemsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Same proportionate testing choice as ScanViewModelTest (Sprint 020):
 * mockk directly on the concrete FindCleanableItemsUseCase class (mockk
 * mocks final Kotlin classes without needing them declared open), rather
 * than mocking EnumerationRepository and re-exercising
 * JunkFileClassifier's own rules here — those are already exhaustively
 * covered by JunkFileClassifierTest (Sprint 022).
 */
class CleanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val findCleanableItems = mockk<FindCleanableItemsUseCase>()

    private fun buildViewModel(): CleanViewModel = CleanViewModel(findCleanableItems)

    private fun cleanableItem(
        name: String = "notes.bak",
        sizeBytes: Long = 1_000L,
        category: CleanableCategory = CleanableCategory.TEMPORARY_FILE,
    ) = CleanableItem(
        path = "/storage/emulated/0/Documents/$name",
        name = name,
        sizeBytes = sizeBytes,
        category = category,
        reason = "test reason",
    )

    @Test
    fun `initial state is Idle`() {
        assertThat(buildViewModel().uiState.value).isEqualTo(CleanUiState.Idle)
    }

    @Test
    fun `scanForJunk transitions from Idle through Loading to Loaded with results`() = runTest {
        coEvery { findCleanableItems(ScanScope.InternalStorage) } returns
            AppResult.Success(listOf(cleanableItem(sizeBytes = 500L), cleanableItem(sizeBytes = 1_500L)))

        val viewModel = buildViewModel()
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(CleanUiState.Idle)

            viewModel.scanForJunk()

            assertThat(awaitItem()).isEqualTo(CleanUiState.Loading)
            val loaded = awaitItem() as CleanUiState.Loaded
            assertThat(loaded.items).hasSize(2)
            assertThat(loaded.totalSizeBytes).isEqualTo(2_000L)
        }
    }

    @Test
    fun `an empty result yields a Loaded state with zero total size, not an error`() = runTest {
        coEvery { findCleanableItems(ScanScope.InternalStorage) } returns AppResult.Success(emptyList())

        val viewModel = buildViewModel()
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(CleanUiState.Idle)
            viewModel.scanForJunk()
            assertThat(awaitItem()).isEqualTo(CleanUiState.Loading)
            val loaded = awaitItem() as CleanUiState.Loaded
            assertThat(loaded.items).isEmpty()
            assertThat(loaded.totalSizeBytes).isEqualTo(0L)
        }
    }

    @Test
    fun `a permission failure surfaces as a friendly Error state`() = runTest {
        coEvery { findCleanableItems(ScanScope.InternalStorage) } returns
            AppResult.Failure(AppError.PermissionMissing)

        val viewModel = buildViewModel()
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(CleanUiState.Idle)
            viewModel.scanForJunk()
            assertThat(awaitItem()).isEqualTo(CleanUiState.Loading)
            val error = awaitItem() as CleanUiState.Error
            assertThat(error.message).isEqualTo("Space Antivirus needs storage permission to scan for junk files.")
        }
    }

    @Test
    fun `calling scanForJunk again while already Loading does not trigger a second scan`() = runTest {
        coEvery { findCleanableItems(ScanScope.InternalStorage) } coAnswers {
            delay(10_000)
            AppResult.Success(emptyList())
        }

        val viewModel = buildViewModel()
        viewModel.scanForJunk()
        // Same reasoning as ScanViewModelTest's equivalent guard test
        // (ADR 0033): MainDispatcherRule uses StandardTestDispatcher,
        // which schedules launch{} rather than running it immediately —
        // runCurrent() lets the first call actually reach its suspension
        // point before the guard is exercised again.
        runCurrent()

        viewModel.scanForJunk()
        viewModel.scanForJunk()

        coVerify(exactly = 1) { findCleanableItems(ScanScope.InternalStorage) }
    }
}
