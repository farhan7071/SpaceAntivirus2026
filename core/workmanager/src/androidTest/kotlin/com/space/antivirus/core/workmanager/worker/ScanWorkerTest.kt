package com.space.antivirus.core.workmanager.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.core.model.ScanSession
import com.space.antivirus.core.model.ScanSessionState
import com.space.antivirus.core.model.ScanStatistics
import com.space.antivirus.core.model.ScanType
import com.space.antivirus.domain.usecase.RunScanRequestUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @AssistedInject constructors remain plain, directly-callable Kotlin
 * constructors — this test never needs real Hilt test infrastructure
 * (no HiltAndroidTest, no HiltTestRunner). TestListenableWorkerBuilder
 * (androidx.work:work-testing) is still needed because WorkerParameters
 * itself isn't meant to be constructed directly by app code; its
 * setWorkerFactory(...) hook is used here to supply ScanWorker with a
 * mocked RunScanRequestUseCase instead of going through Hilt at all.
 */
@RunWith(AndroidJUnit4::class)
class ScanWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val runScanRequest = mockk<RunScanRequestUseCase>()

    private fun buildWorker(): ScanWorker {
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker = ScanWorker(appContext, workerParameters, runScanRequest)
        }
        return TestListenableWorkerBuilder<ScanWorker>(context)
            .setWorkerFactory(factory)
            .build()
    }

    private fun cleanScanResult() = ScanResult(
        session = ScanSession(
            id = "s1",
            scanType = ScanType.QUICK,
            state = ScanSessionState.COMPLETED,
            startedAtEpochMillis = 0L,
            completedAtEpochMillis = 1_000L,
        ),
        statistics = ScanStatistics(
            itemsScanned = 10,
            threatsFound = 0,
            itemsInconclusive = 0,
            itemsTrusted = 0,
            durationMillis = 1_000,
        ),
        threats = emptyList(),
    )

    @Test
    fun doWork_onSuccess_returnsSuccess() = runTest {
        coEvery { runScanRequest(any()) } returns AppResult.Success(cleanScanResult())

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun doWork_whenAScanIsAlreadyInProgress_stillReturnsSuccess() = runTest {
        // The concurrent-scan guard (ADR 0020) rejecting this attempt
        // means a scan is already covering this cycle — that's the guard
        // working as designed, not this worker failing (ADR 0037).
        coEvery { runScanRequest(any()) } returns
            AppResult.Failure(AppError.ScanAlreadyInProgress("other-session"))

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun doWork_whenPermissionIsMissing_returnsFailureNotRetry() = runTest {
        // Retrying can't fix a missing permission — Result.failure(),
        // not Result.retry(), is the honest signal to WorkManager.
        coEvery { runScanRequest(any()) } returns AppResult.Failure(AppError.PermissionMissing)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun doWork_onATransientFailure_returnsRetry() = runTest {
        coEvery { runScanRequest(any()) } returns AppResult.Failure(AppError.StorageUnavailable)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }
}
