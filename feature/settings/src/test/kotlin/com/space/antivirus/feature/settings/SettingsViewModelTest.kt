package com.space.antivirus.feature.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.ProtectionState
import com.space.antivirus.core.testing.MainDispatcherRule
import com.space.antivirus.domain.usecase.GetBatteryOptimizationStatusUseCase
import com.space.antivirus.domain.usecase.ObserveProtectionStateUseCase
import com.space.antivirus.domain.usecase.SetNotifyAfterScanUseCase
import com.space.antivirus.domain.usecase.SetProtectionEnabledUseCase
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
 * Sprint 042 rewrote this suite. SettingsViewModel used to orchestrate
 * six use cases itself; it now delegates to ProtectionManager through
 * two, so what is worth testing here is the delegation and the error
 * copy — the schedule/persist/notify ordering moved to
 * ProtectionManagerImplTest, where it can be tested directly.
 *
 * The Sprint 025 subscription caveat still applies and is still the
 * reason every test collects uiState before calling an action: uiState
 * is stateIn(WhileSubscribed)-backed, so its .value — which the action
 * methods read to find the current interval — does not populate until
 * something actually collects it.
 */
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeProtectionState = mockk<ObserveProtectionStateUseCase>()
    private val setProtectionEnabled = mockk<SetProtectionEnabledUseCase>(relaxed = true)
    private val setNotifyAfterScan = mockk<SetNotifyAfterScanUseCase>(relaxed = true)
    private val setScanInterval = mockk<SetScanIntervalUseCase>(relaxed = true)
    private val getBatteryOptimizationStatus = mockk<GetBatteryOptimizationStatusUseCase>()

    private fun protectionState(
        isEnabled: Boolean = false,
        intervalHours: Long = 24L,
        lastScheduledAt: Long? = null,
        notifyAfterScan: Boolean = false,
    ) = ProtectionState(
        isEnabled = isEnabled,
        intervalHours = intervalHours,
        lastScheduledAtEpochMillis = lastScheduledAt,
        notifyAfterScan = notifyAfterScan,
    )

    private fun buildViewModel(
        state: ProtectionState = protectionState(),
        ignoringBatteryOptimizations: Boolean = true,
    ): SettingsViewModel {
        every { observeProtectionState() } returns flowOf(state)
        every { getBatteryOptimizationStatus() } returns ignoringBatteryOptimizations
        coEvery { setProtectionEnabled(any(), any()) } returns AppResult.Success(Unit)
        return SettingsViewModel(
            observeProtectionState = observeProtectionState,
            setProtectionEnabled = setProtectionEnabled,
            setNotifyAfterScan = setNotifyAfterScan,
            setScanInterval = setScanInterval,
            getBatteryOptimizationStatus = getBatteryOptimizationStatus,
        )
    }

    @Test
    fun `exposes the protection state it is given`() = runTest {
        val viewModel = buildViewModel(
            protectionState(isEnabled = true, intervalHours = 72L, notifyAfterScan = true),
        )

        viewModel.uiState.test {
            skipItems(1)
            val loaded = awaitItem() as SettingsUiState.Loaded
            assertThat(loaded.backgroundProtectionEnabled).isTrue()
            assertThat(loaded.selectedInterval).isEqualTo(ScanInterval.EVERY_3_DAYS)
            assertThat(loaded.notifyAfterScan).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `enabling protection delegates with the currently selected interval`() = runTest {
        val viewModel = buildViewModel(protectionState(isEnabled = false, intervalHours = 168L))

        viewModel.uiState.test {
            skipItems(2)
            viewModel.onBackgroundProtectionToggled(true)
            runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { setProtectionEnabled(enabled = true, intervalHours = 168L) }
    }

    @Test
    fun `disabling protection delegates without an interval`() = runTest {
        val viewModel = buildViewModel(protectionState(isEnabled = true))

        viewModel.uiState.test {
            skipItems(2)
            viewModel.onBackgroundProtectionToggled(false)
            runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { setProtectionEnabled(enabled = false, intervalHours = null) }
    }

    @Test
    fun `a failure to enable surfaces an error rather than pretending it worked`() = runTest {
        val viewModel = buildViewModel()
        coEvery { setProtectionEnabled(any(), any()) } returns
            AppResult.Failure(AppError.InvalidScheduleConfiguration("nope"))

        viewModel.uiState.test {
            skipItems(2)
            viewModel.onBackgroundProtectionToggled(true)
            runCurrent()
            val errored = expectMostRecentItem() as SettingsUiState.Loaded
            assertThat(errored.errorMessage).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `changing the interval while enabled persists it and re-schedules`() = runTest {
        val viewModel = buildViewModel(protectionState(isEnabled = true))

        viewModel.uiState.test {
            skipItems(2)
            viewModel.onIntervalSelected(ScanInterval.WEEKLY)
            runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { setScanInterval(168L) }
        coVerify { setProtectionEnabled(enabled = true, intervalHours = 168L) }
    }

    /** Nothing is scheduled yet, so there is nothing to re-schedule. */
    @Test
    fun `changing the interval while disabled persists it without scheduling`() = runTest {
        val viewModel = buildViewModel(protectionState(isEnabled = false))

        viewModel.uiState.test {
            skipItems(2)
            viewModel.onIntervalSelected(ScanInterval.WEEKLY)
            runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { setScanInterval(168L) }
        coVerify(exactly = 0) { setProtectionEnabled(any(), any()) }
    }

    @Test
    fun `toggling notify after scan delegates`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onNotifyAfterScanToggled(true)
        runCurrent()

        coVerify { setNotifyAfterScan(true) }
    }

    @Test
    fun `battery optimisation status is surfaced so the card can be shown`() = runTest {
        val viewModel = buildViewModel(ignoringBatteryOptimizations = false)

        viewModel.uiState.test {
            skipItems(1)
            assertThat((awaitItem() as SettingsUiState.Loaded).isIgnoringBatteryOptimizations).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
