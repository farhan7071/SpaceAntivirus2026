package com.space.antivirus.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.model.FileMetadata
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.domain.cleaning.JunkFileClassifier
import com.space.antivirus.domain.fake.FakeEnumerationRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Sprint 039. Covers the use case that replaced FindCleanableItemsUseCase.
 * JunkFileClassifier's own rules stay covered by JunkFileClassifierTest —
 * what matters here is that progress is real and that an unreadable
 * scope is reported rather than swallowed.
 */
class ScanForJunkFilesUseCaseTest {

    private fun fileTarget(name: String, sizeBytes: Long = 100L) = ScanTarget.FileTarget(
        FileMetadata(
            path = "/app-private/cache/$name",
            name = name,
            sizeBytes = sizeBytes,
            mimeType = null,
            lastModifiedEpochMillis = 0L,
            isDirectory = false,
        ),
    )

    @Test
    fun `emits running progress and completes with the classified items`() = runTest {
        val repository = FakeEnumerationRepository(
            fileTargets = listOf(fileTarget("a.tmp"), fileTarget("b.log")),
        )

        val events = ScanForJunkFilesUseCase(repository, JunkFileClassifier()).invoke().toList()

        val progressEvents = events.filterIsInstance<JunkScanEvent.InProgress>()
        // A starting emission plus one per file, per scope.
        assertThat(progressEvents.size).isAtLeast(2)
        assertThat(progressEvents.last().progress.filesInspected).isGreaterThan(0)

        val completed = events.last() as JunkScanEvent.Completed
        assertThat(completed.items).isNotEmpty()
        assertThat(completed.totalSizeBytes).isEqualTo(completed.items.sumOf { it.sizeBytes })
    }

    /**
     * The failure that matters most: a scope that cannot be read must
     * produce Failed, never Completed-with-nothing. The second reads as
     * "your storage is clean" on screen, which would be a claim about a
     * device that was never actually examined.
     */
    @Test
    fun `an unreadable scope reports failure rather than an empty result`() = runTest {
        val repository = FakeEnumerationRepository(forcedFailure = AppError.StorageUnavailable)

        val events = ScanForJunkFilesUseCase(repository, JunkFileClassifier()).invoke().toList()

        assertThat(events.last()).isInstanceOf(JunkScanEvent.Failed::class.java)
        assertThat(events.filterIsInstance<JunkScanEvent.Completed>()).isEmpty()
    }

    @Test
    fun `a scope with no junk completes with an empty list`() = runTest {
        val repository = FakeEnumerationRepository(fileTargets = emptyList())

        val events = ScanForJunkFilesUseCase(repository, JunkFileClassifier()).invoke().toList()

        val completed = events.last() as JunkScanEvent.Completed
        assertThat(completed.items).isEmpty()
        assertThat(completed.totalSizeBytes).isEqualTo(0L)
    }
}
