package com.space.antivirus.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.model.CleanableCategory
import com.space.antivirus.core.model.CleanableItem
import com.space.antivirus.core.model.CleaningEvent
import com.space.antivirus.domain.fake.FakeCleanupHistoryRepository
import com.space.antivirus.domain.fake.FakeFileDeletionRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Sprint 039. The point of this suite is that every number the Cleaner
 * shows is measured rather than assumed — so most of these tests are
 * about what happens when reality disagrees with the candidate list.
 */
class CleanJunkFilesUseCaseTest {

    private fun item(name: String, sizeBytes: Long, root: String = "/app-private") = CleanableItem(
        path = "$root/$name",
        name = name,
        sizeBytes = sizeBytes,
        category = CleanableCategory.CACHE_FILE,
        reason = "test",
    )

    @Test
    fun `deletes every candidate and reports the real total freed`() = runTest {
        val items = listOf(item("a.tmp", 100L), item("b.tmp", 200L))
        val deletion = FakeFileDeletionRepository(
            sizesByPath = mapOf("/app-private/a.tmp" to 100L, "/app-private/b.tmp" to 200L),
        )
        val history = FakeCleanupHistoryRepository()

        val events = CleanJunkFilesUseCase(deletion, history)(items).toList()

        val summary = (events.last() as CleaningEvent.Completed).summary
        assertThat(summary.itemsDeleted).isEqualTo(2)
        assertThat(summary.itemsFailed).isEqualTo(0)
        assertThat(summary.bytesFreed).isEqualTo(300L)
        assertThat(deletion.attemptedPaths).containsExactly("/app-private/a.tmp", "/app-private/b.tmp")
    }

    /**
     * The size reported by the deletion repository is authoritative, not
     * the size recorded at scan time — a file can change in between, and
     * "bytes freed" must describe what was actually removed.
     */
    @Test
    fun `reports the size measured at deletion, not the size recorded at scan time`() = runTest {
        val items = listOf(item("a.tmp", sizeBytes = 999_999L))
        val deletion = FakeFileDeletionRepository(sizesByPath = mapOf("/app-private/a.tmp" to 42L))

        val events = CleanJunkFilesUseCase(deletion, FakeCleanupHistoryRepository())(items).toList()

        assertThat((events.last() as CleaningEvent.Completed).summary.bytesFreed).isEqualTo(42L)
    }

    /** A failed file contributes zero bytes and does not abort the run. */
    @Test
    fun `a failed deletion is counted and contributes no bytes`() = runTest {
        val items = listOf(item("a.tmp", 100L), item("b.tmp", 200L), item("c.tmp", 300L))
        val deletion = FakeFileDeletionRepository(
            sizesByPath = mapOf(
                "/app-private/a.tmp" to 100L,
                "/app-private/b.tmp" to 200L,
                "/app-private/c.tmp" to 300L,
            ),
            failingPaths = setOf("/app-private/b.tmp"),
        )

        val events = CleanJunkFilesUseCase(deletion, FakeCleanupHistoryRepository())(items).toList()

        val summary = (events.last() as CleaningEvent.Completed).summary
        assertThat(summary.itemsDeleted).isEqualTo(2)
        assertThat(summary.itemsFailed).isEqualTo(1)
        assertThat(summary.bytesFreed).isEqualTo(400L)
        // The batch continued past the failure.
        assertThat(deletion.attemptedPaths).hasSize(3)
    }

    /**
     * Items outside app-private storage are never even attempted. The
     * repository would refuse them anyway — that is the real safety
     * boundary — but attempting them would inflate the failure count
     * with items the user was never going to lose.
     */
    @Test
    fun `candidates outside app-private storage are never attempted`() = runTest {
        val items = listOf(item("a.tmp", 100L), item("photo.jpg", 900L, root = "/sdcard/DCIM"))
        val deletion = FakeFileDeletionRepository(sizesByPath = mapOf("/app-private/a.tmp" to 100L))

        val events = CleanJunkFilesUseCase(deletion, FakeCleanupHistoryRepository())(items).toList()

        assertThat(deletion.attemptedPaths).containsExactly("/app-private/a.tmp")
        val summary = (events.last() as CleaningEvent.Completed).summary
        assertThat(summary.itemsRequested).isEqualTo(1)
    }

    @Test
    fun `progress is emitted per item with running real totals`() = runTest {
        val items = listOf(item("a.tmp", 100L), item("b.tmp", 200L))
        val deletion = FakeFileDeletionRepository(
            sizesByPath = mapOf("/app-private/a.tmp" to 100L, "/app-private/b.tmp" to 200L),
        )

        CleanJunkFilesUseCase(deletion, FakeCleanupHistoryRepository())(items).test {
            assertThat((awaitItem() as CleaningEvent.InProgress).progress.totalItems).isEqualTo(2)

            val first = (awaitItem() as CleaningEvent.InProgress).progress
            assertThat(first.itemsProcessed).isEqualTo(1)
            assertThat(first.bytesFreed).isEqualTo(100L)

            val second = (awaitItem() as CleaningEvent.InProgress).progress
            assertThat(second.itemsProcessed).isEqualTo(2)
            assertThat(second.bytesFreed).isEqualTo(300L)
            assertThat(second.isComplete).isTrue()

            assertThat(awaitItem()).isInstanceOf(CleaningEvent.Completed::class.java)
            awaitComplete()
        }
    }

    @Test
    fun `a completed cleanup is persisted to history`() = runTest {
        val items = listOf(item("a.tmp", 100L))
        val deletion = FakeFileDeletionRepository(sizesByPath = mapOf("/app-private/a.tmp" to 100L))
        val history = FakeCleanupHistoryRepository()

        CleanJunkFilesUseCase(deletion, history)(items).toList()

        assertThat(history.recorded).hasSize(1)
        assertThat(history.recorded.first().bytesFreed).isEqualTo(100L)
        assertThat(history.recorded.first().wasCancelled).isFalse()
    }

    @Test
    fun `an empty candidate list completes immediately with a zero summary`() = runTest {
        val events = CleanJunkFilesUseCase(
            FakeFileDeletionRepository(),
            FakeCleanupHistoryRepository(),
        )(emptyList()).toList()

        val summary = (events.last() as CleaningEvent.Completed).summary
        assertThat(summary.itemsRequested).isEqualTo(0)
        assertThat(summary.bytesFreed).isEqualTo(0L)
    }
}
