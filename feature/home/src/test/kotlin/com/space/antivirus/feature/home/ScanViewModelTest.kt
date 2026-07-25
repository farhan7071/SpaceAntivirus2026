package com.space.antivirus.feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.ScanProgress
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.core.model.ScanSession
import com.space.antivirus.core.model.ScanSessionState
import com.space.antivirus.core.model.ScanStatistics
import com.space.antivirus.core.model.ScanType
import com.space.antivirus.core.testing.MainDispatcherRule
import com.space.antivirus.domain.usecase.GetActiveScanSessionUseCase
import com.space.antivirus.domain.usecase.ObserveScanProgressUseCase
import com.space.antivirus.domain.usecase.RunScanRequestUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * The most concurrency-sensitive tests in this project so far — verifies
 * both state transitions AND the polling bridge documented in ADR 0033.
 * Uses mockk directly on the three UseCases involved, same proportionate
 * choice as every prior ViewModel test in this project.
 */
class ScanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val runScanRequest = mockk<RunScanRequestUseCase>()
    private val getActiveScanSession = mockk<GetActiveScanSessionUseCase>()
    private val observeScanProgress = mockk<ObserveScanProgressUseCase>()

    private fun buildViewModel(): ScanViewModel =
        ScanViewModel(runScanRequest, getActiveScanSession, observeScanProgress)

    private fun runningSession(id: String = "s1") = ScanSession(
        id = id,
        scanType = ScanType.QUICK,
        state = ScanSessionState.RUNNING,
        startedAtEpochMillis = 0L,
        completedAtEpochMillis = null,
    )

    private fun cleanScanResult(itemsScanned: Int = 10) = ScanResult(
        session = ScanSession(
            id = "s1",
            scanType = ScanType.QUICK,
            state = ScanSessionState.COMPLETED,
            startedAtEpochMillis = 0L,
            completedAtEpochMillis = 1_000L,
        ),
        statistics = ScanStatistics(
            itemsScanned = itemsScanned,
            threatsFound = 0,
            itemsInconclusive = 0,
            itemsTrusted = 0,
            durationMillis = 1_000,
        ),
        threats = emptyList(),
    )

    @Test
    fun `initial state is Idle`() {
        assertThat(buildViewModel().uiState.value).isEqualTo(ScanUiState.Idle)
    }

    @Test
    fun `startScan transitions from Idle through Running to Completed on success`() = runTest {
        coEvery { getActiveScanSession() } returns AppResult.Success(null)
        coEvery { runScanRequest(any()) } returns AppResult.Success(cleanScanResult())

        val viewModel = buildViewModel()
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(ScanUiState.Idle)

            viewModel.startScan()

            assertThat(awaitItem()).isEqualTo(ScanUiState.Running(progress = null))
            val completed = awaitItem() as ScanUiState.Completed
            assertThat(completed.isClean).isTrue()
            assertThat(completed.threatsFound).isEqualTo(0)
            assertThat(completed.itemsScanned).isEqualTo(10)
        }
    }

    @Test
    fun `live progress updates are surfaced while the scan is running`() = runTest {
        val session = runningSession()
        val firstSnapshot = ScanProgress("s1", itemsProcessed = 1, totalItems = 5, threatsFoundSoFar = 0)
        val secondSnapshot = ScanProgress("s1", itemsProcessed = 5, totalItems = 5, threatsFoundSoFar = 0)
        coEvery { getActiveScanSession() } returns AppResult.Success(session)
        every { observeScanProgress("s1") } returns flowOf(firstSnapshot, secondSnapshot)
        coEvery { runScanRequest(any()) } coAnswers {
            delay(10_000)
            AppResult.Success(cleanScanResult())
        }

        val viewModel = buildViewModel()
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(ScanUiState.Idle)

            viewModel.startScan()

            assertThat(awaitItem()).isEqualTo(ScanUiState.Running(progress = null))
            assertThat(awaitItem()).isEqualTo(ScanUiState.Running(progress = firstSnapshot))
            assertThat(awaitItem()).isEqualTo(ScanUiState.Running(progress = secondSnapshot))
            val completed = awaitItem() as ScanUiState.Completed
            assertThat(completed.isClean).isTrue()
        }
    }

    @Test
    fun `if the session never becomes active, the scan still completes normally without progress`() = runTest {
        // Every poll attempt reports no active session — the bridge
        // documented in ADR 0033 times out gracefully rather than
        // blocking the scan's own result from reaching the UI.
        coEvery { getActiveScanSession() } returns AppResult.Success(null)
        coEvery { runScanRequest(any()) } coAnswers {
            delay(5_000)
            AppResult.Success(cleanScanResult())
        }

        val viewModel = buildViewModel()
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(ScanUiState.Idle)

            viewModel.startScan()

            assertThat(awaitItem()).isEqualTo(ScanUiState.Running(progress = null))
            // No Running(progress = X) ever appears — straight to Completed.
            val completed = awaitItem() as ScanUiState.Completed
            assertThat(completed.isClean).isTrue()
        }
    }

    @Test
    fun `a scan failure surfaces as a friendly Error state`() = runTest {
        coEvery { getActiveScanSession() } returns AppResult.Success(null)
        coEvery { runScanRequest(any()) } returns AppResult.Failure(AppError.ScanAlreadyInProgress("other-session"))

        val viewModel = buildViewModel()
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(ScanUiState.Idle)

            viewModel.startScan()

            assertThat(awaitItem()).isEqualTo(ScanUiState.Running(progress = null))
            val error = awaitItem() as ScanUiState.Error
            assertThat(error.message).isEqualTo("A scan is already running.")
        }
    }

    @Test
    fun `calling startScan again while already Running does not trigger a second scan`() = runTest {
        coEvery { getActiveScanSession() } returns AppResult.Success(null)
        coEvery { runScanRequest(any()) } coAnswers {
            delay(10_000)
            AppResult.Success(cleanScanResult())
        }

        val viewModel = buildViewModel()
        viewModel.startScan()
        // Let the first launch{} actually reach its suspension point
        // (inside the mocked delay) before checking the guard again —
        // MainDispatcherRule uses StandardTestDispatcher, which merely
        // schedules launch{} rather than running it immediately the way
        // production's Dispatchers.Main.immediate does. Without this,
        // all three calls below would see uiState still Idle (none of
        // their launched coroutines would have run yet) and the guard
        // would never actually be exercised — a test artifact of
        // non-immediate dispatching, not a reflection of real behavior.
        runCurrent()

        viewModel.startScan()
        viewModel.startScan()

        coVerify(exactly = 1) { runScanRequest(any()) }
    }

    @Test
    fun `acknowledgeResult returns to Idle from Completed`() = runTest {
        coEvery { getActiveScanSession() } returns AppResult.Success(null)
        coEvery { runScanRequest(any()) } returns AppResult.Success(cleanScanResult())

        val viewModel = buildViewModel()
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(ScanUiState.Idle)
            viewModel.startScan()
            assertThat(awaitItem()).isEqualTo(ScanUiState.Running(progress = null))
            awaitItem() // Completed

            viewModel.acknowledgeResult()

            assertThat(awaitItem()).isEqualTo(ScanUiState.Idle)
        }
    }

    @Test
    fun `acknowledgeResult does nothing while a scan is Running`() = runTest {
        coEvery { getActiveScanSession() } returns AppResult.Success(null)
        coEvery { runScanRequest(any()) } coAnswers {
            delay(10_000)
            AppResult.Success(cleanScanResult())
        }

        val viewModel = buildViewModel()
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(ScanUiState.Idle)
            viewModel.startScan()
            assertThat(awaitItem()).isEqualTo(ScanUiState.Running(progress = null))

            viewModel.acknowledgeResult()

            // No new emission — acknowledgeResult is a no-op while Running.
            expectNoEvents()
        }
    }
}
