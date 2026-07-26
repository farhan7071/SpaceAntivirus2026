package com.space.antivirus.feature.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.testing.MainDispatcherRule
import com.space.antivirus.domain.usecase.CancelBackgroundScanUseCase
import com.space.antivirus.domain.usecase.ObserveBackgroundProtectionEnabledUseCase
import com.space.antivirus.domain.usecase.ObserveLastScheduledAtUseCase
import com.space.antivirus.domain.usecase.ObserveScanIntervalUseCase
import com.space.antivirus.domain.usecase.RecordBackgroundProtectionDisabledUseCase
import com.space.antivirus.domain.usecase.RecordBackgroundProtectionEnabledUseCase
import com.space.antivirus.domain.usecase.ScheduleBackgroundScanUseCase
import com.space.antivirus.domain.usecase.SetScanIntervalUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Same proportionate testing choice as every prior ViewModel in this
 * project: mockk on each injected UseCase directly. Eight dependencies —
 * the most this project's ViewModels have needed yet — but each one is
 * already independently tested (ScheduleBackgroundScanUseCaseTest,
 * CancelBackgroundScanUseCaseTest, and the four new preferences
 * UseCases' own domain-layer tests), so this file focuses on
 * orchestration: does SettingsViewModel call the right ones, in the
 * right order, only under the right conditions.
 *
 * Two real, non-obvious bugs were found and fixed while writing these:
 *
 * 1. onBackgroundProtectionToggled/onIntervalSelected both read
 *    uiState.value synchronously (to get the current interval or
 *    enabled state). uiState is stateIn(WhileSubscribed(...))-backed —
 *    its upstream combine() pipeline does not start, and .value does
 *    not populate with real data, until something actually COLLECTS the
 *    flow; merely reading .value doesn't count as subscribing. Every
 *    test here collects via uiState.test{} (awaiting Loading + the
 *    initial Loaded emission) BEFORE calling any action method, with
 *    the action called INSIDE that same block so the subscription stays
 *    active while the action's internal .value read happens. Skipping
 *    this would let a test silently pass for the wrong reason —
 *    onIntervalSelected's `?: false` and onBackgroundProtectionToggled's
 *    `?: ScanInterval.DAILY.hours` fallbacks can accidentally coincide
 *    with a test's intended values even when the real subscribed-state
 *    read was never exercised.
 *
 * 2. The mocked observeEnabled()/observeInterval()/observeLastScheduledAt()
 *    Flows are static flowOf(...) values — calling setScanInterval(...)
 *    or recordEnabled(...) on the SEPARATE mocked "write" UseCases never
 *    causes them to re-emit, since these mocks don't simulate a real
 *    DataStore's write-then-read round trip. Success-path tests
 *    therefore use runCurrent() (to force the launched coroutine to
 *    actually execute) followed by cancelAndIgnoreRemainingEvents() —
 *    not a further awaitItem(), which would hang waiting for an
 *    emission that structurally cannot arrive. Only the failure-path
 *    tests genuinely produce a new emission, since transientError
 *    itself changes value (null -> a message, or back) — those keep
 *    awaitItem().
 */
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeEnabled = mockk<ObserveBackgroundProtectionEnabledUseCase>()
    private val observeInterval = mockk<ObserveScanIntervalUseCase>()
    private val observeLastScheduledAt = mockk<ObserveLastScheduledAtUseCase>()
    private val scheduleBackgroundScan = mockk<ScheduleBackgroundScanUseCase>()
    private val cancelBackgroundScan = mockk<CancelBackgroundScanUseCase>()
    private val recordEnabled = mockk<RecordBackgroundProtectionEnabledUseCase>()
    private val recordDisabled = mockk<RecordBackgroundProtectionDisabledUseCase>()
    private val setScanInterval = mockk<SetScanIntervalUseCase>()

    private fun buildViewModel(
        enabled: Boolean = false,
        intervalHours: Long = 24L,
        lastScheduledAt: Long? = null,
    ): SettingsViewModel {
        every { observeEnabled() } returns flowOf(enabled)
        every { observeInterval() } returns flowOf(intervalHours)
        every { observeLastScheduledAt() } returns flowOf(lastScheduledAt)
        return SettingsViewModel(
            observeEnabled,
            observeInterval,
            observeLastScheduledAt,
            scheduleBackgroundScan,
            cancelBackgroundScan,
            recordEnabled,
            recordDisabled,
            setScanInterval,
        )
    }

    @Test
    fun `initial Loaded state reflects the persisted preferences`() = runTest {
        val viewModel = buildViewModel(enabled = true, intervalHours = 72L, lastScheduledAt = 5_000L)

        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(SettingsUiState.Loading)
            val state = awaitItem() as SettingsUiState.Loaded
            assertThat(state.backgroundProtectionEnabled).isTrue()
            assertThat(state.selectedInterval).isEqualTo(ScanInterval.EVERY_3_DAYS)
            assertThat(state.lastScheduledAtEpochMillis).isEqualTo(5_000L)
        }
    }

    @Test
    fun `toggling on schedules with the current interval and records success on completion`() = runTest {
        val viewModel = buildViewModel(enabled = false, intervalHours = 24L)
        coEvery { scheduleBackgroundScan(24L) } returns AppResult.Success(Unit)
        coEvery { recordEnabled(any()) } returns AppResult.Success(Unit)

        viewModel.uiState.test {
            awaitItem() // Loading
            awaitItem() // initial Loaded — subscription now active, uiState.value is real

            viewModel.onBackgroundProtectionToggled(true)
            runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { scheduleBackgroundScan(24L) }
        coVerify(exactly = 1) { recordEnabled(match { it.intervalHours == 24L }) }
    }

    @Test
    fun `toggling off cancels the schedule and records disabled on completion`() = runTest {
        val viewModel = buildViewModel(enabled = true, intervalHours = 24L)
        coEvery { cancelBackgroundScan() } returns AppResult.Success(Unit)
        coEvery { recordDisabled() } returns AppResult.Success(Unit)

        viewModel.uiState.test {
            awaitItem() // Loading
            awaitItem() // initial Loaded

            viewModel.onBackgroundProtectionToggled(false)
            runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { cancelBackgroundScan() }
        coVerify(exactly = 1) { recordDisabled() }
    }

    @Test
    fun `a scheduling failure surfaces a transient error and does NOT record enabled`() = runTest {
        val viewModel = buildViewModel(enabled = false, intervalHours = 24L)
        coEvery { scheduleBackgroundScan(24L) } returns
            AppResult.Failure(AppError.InvalidScheduleConfiguration("bad interval"))

        viewModel.uiState.test {
            awaitItem() // Loading
            awaitItem() // initial Loaded

            viewModel.onBackgroundProtectionToggled(true)

            // transientError genuinely changes (null -> a message), a
            // real emission this time, unlike the success-path tests.
            val stateWithError = awaitItem() as SettingsUiState.Loaded
            assertThat(stateWithError.errorMessage).isEqualTo(
                "Couldn't turn on background protection. Please try again.",
            )
        }
        coVerify(exactly = 0) { recordEnabled(any()) }
    }

    @Test
    fun `selecting an interval while disabled persists the choice without re-scheduling`() = runTest {
        val viewModel = buildViewModel(enabled = false, intervalHours = 24L)
        coEvery { setScanInterval(168L) } returns AppResult.Success(Unit)

        viewModel.uiState.test {
            awaitItem() // Loading
            awaitItem() // initial Loaded — subscription active, backgroundProtectionEnabled=false is real

            viewModel.onIntervalSelected(ScanInterval.WEEKLY)
            runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { setScanInterval(168L) }
        coVerify(exactly = 0) { scheduleBackgroundScan(any()) }
    }

    @Test
    fun `selecting an interval while enabled persists the choice and re-schedules immediately`() = runTest {
        val viewModel = buildViewModel(enabled = true, intervalHours = 24L)
        coEvery { setScanInterval(168L) } returns AppResult.Success(Unit)
        coEvery { scheduleBackgroundScan(168L) } returns AppResult.Success(Unit)
        coEvery { recordEnabled(any()) } returns AppResult.Success(Unit)

        viewModel.uiState.test {
            awaitItem() // Loading
            awaitItem() // initial Loaded — subscription active, backgroundProtectionEnabled=true is real

            viewModel.onIntervalSelected(ScanInterval.WEEKLY)
            runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { setScanInterval(168L) }
        coVerify(exactly = 1) { scheduleBackgroundScan(168L) }
        coVerify(exactly = 1) { recordEnabled(match { it.intervalHours == 168L }) }
    }

    @Test
    fun `dismissError clears a previously surfaced transient error`() = runTest {
        val viewModel = buildViewModel(enabled = false, intervalHours = 24L)
        coEvery { scheduleBackgroundScan(24L) } returns AppResult.Failure(AppError.Unexpected(null))

        viewModel.uiState.test {
            awaitItem() // Loading
            awaitItem() // initial Loaded
            viewModel.onBackgroundProtectionToggled(true)
            val withError = awaitItem() as SettingsUiState.Loaded // real emission: transientError null -> message
            assertThat(withError.errorMessage).isNotNull()

            viewModel.dismissError()

            val cleared = awaitItem() as SettingsUiState.Loaded // real emission: transientError message -> null
            assertThat(cleared.errorMessage).isNull()
        }
    }
}
