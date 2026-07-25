package com.space.antivirus.core.workmanager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.workmanager.worker.ScanWorker
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A real, test-mode WorkManager instance (WorkManagerTestInitHelper),
 * not a mock — WorkManager itself is complex enough that a mock would
 * mostly just re-assert whatever the mock was told to return, matching
 * how this project has always preferred real infrastructure over
 * fragile mocks for Room/Hilt-graph verification (Sprint 010+).
 */
@RunWith(AndroidJUnit4::class)
class WorkManagerBackgroundScanSchedulerTest {

    private lateinit var context: Context
    private lateinit var scheduler: WorkManagerBackgroundScanScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder().build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        scheduler = WorkManagerBackgroundScanScheduler(context)
    }

    @Test
    fun schedulePeriodicScan_enqueuesWorkUnderTheExpectedUniqueName() = runTest {
        val result = scheduler.schedulePeriodicScan()

        assertThat(result).isEqualTo(AppResult.Success(Unit))
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(ScanWorker.UNIQUE_WORK_NAME)
            .get()
        assertThat(workInfos).isNotEmpty()
        assertThat(workInfos.first().state).isEqualTo(WorkInfo.State.ENQUEUED)
    }

    @Test
    fun schedulePeriodicScan_calledTwice_doesNotDuplicateWork() = runTest {
        scheduler.schedulePeriodicScan()
        scheduler.schedulePeriodicScan()

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(ScanWorker.UNIQUE_WORK_NAME)
            .get()
        // ExistingPeriodicWorkPolicy.REPLACE means re-scheduling replaces
        // rather than accumulates a second entry under the same name.
        assertThat(workInfos).hasSize(1)
    }

    @Test
    fun cancelScheduledScan_removesPreviouslyScheduledWork() = runTest {
        scheduler.schedulePeriodicScan()

        val result = scheduler.cancelScheduledScan()

        assertThat(result).isEqualTo(AppResult.Success(Unit))
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(ScanWorker.UNIQUE_WORK_NAME)
            .get()
        assertThat(workInfos.all { it.state == WorkInfo.State.CANCELLED }).isTrue()
    }

    @Test
    fun cancelScheduledScan_whenNothingIsScheduled_isANoOpSuccess() = runTest {
        val result = scheduler.cancelScheduledScan()

        assertThat(result).isEqualTo(AppResult.Success(Unit))
    }
}
