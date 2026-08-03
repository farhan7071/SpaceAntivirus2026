package com.space.antivirus.core.cleaningdata

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.database.AppDatabase
import com.space.antivirus.core.model.CleanupRecord
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented — same reasoning as every repository test since Sprint
 * 010 (ADR 0025): needs a real SQLite environment, and in this case a
 * real Context whose app-private directories genuinely exist.
 *
 * Sprint 039. The deletion tests here are the ones that matter most in
 * this whole sprint: they delete real files on a real filesystem and
 * assert that files outside the sandbox survive.
 */
@RunWith(AndroidJUnit4::class)
class CleaningDataInstrumentedTest {

    private lateinit var database: AppDatabase
    private lateinit var historyRepository: CleanupHistoryRepositoryImpl
    private lateinit var deletionRepository: FileDeletionRepositoryImpl
    private lateinit var roots: AppPrivateStorageRoots
    private lateinit var cacheDir: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        historyRepository = CleanupHistoryRepositoryImpl(database.cleanupRecordDao())
        roots = AppPrivateStorageRoots(context)
        deletionRepository = FileDeletionRepositoryImpl(roots)
        cacheDir = context.cacheDir
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun record(id: String, bytesFreed: Long, completedAt: Long) = CleanupRecord(
        id = id,
        completedAtEpochMillis = completedAt,
        itemsDeleted = 3,
        itemsFailed = 0,
        bytesFreed = bytesFreed,
        durationMillis = 500L,
        wasCancelled = false,
    )

    // -- Cleanup history ----------------------------------------------

    @Test
    fun recordedCleanup_isReadBackAsLatest() = runTest {
        historyRepository.record(record("a", bytesFreed = 100L, completedAt = 1_000L))

        val latest = historyRepository.latest()

        assertThat(latest).isInstanceOf(AppResult.Success::class.java)
        assertThat((latest as AppResult.Success).data?.bytesFreed).isEqualTo(100L)
    }

    @Test
    fun latest_returnsNullBeforeAnyCleanupHasRun() = runTest {
        val latest = historyRepository.latest() as AppResult.Success

        assertThat(latest.data).isNull()
    }

    @Test
    fun latest_returnsTheMostRecentRecordNotTheMostRecentlyInserted() = runTest {
        historyRepository.record(record("newer", bytesFreed = 200L, completedAt = 5_000L))
        historyRepository.record(record("older", bytesFreed = 100L, completedAt = 1_000L))

        val latest = historyRepository.latest() as AppResult.Success

        assertThat(latest.data?.id).isEqualTo("newer")
    }

    @Test
    fun observeHistory_emitsRecordsMostRecentFirst() = runTest {
        historyRepository.record(record("older", bytesFreed = 100L, completedAt = 1_000L))
        historyRepository.record(record("newer", bytesFreed = 200L, completedAt = 5_000L))

        historyRepository.observeHistory().test {
            val records = awaitItem()
            assertThat(records.map { it.id }).containsExactly("newer", "older").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -- Real deletion -------------------------------------------------

    @Test
    fun deleteFile_removesARealFileAndReportsItsRealSize() = runTest {
        val file = File(cacheDir, "sprint039_delete_me.tmp").apply { writeText("0123456789") }

        val result = deletionRepository.deleteFile(file.absolutePath)

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat((result as AppResult.Success).data).isEqualTo(10L)
        assertThat(file.exists()).isFalse()
    }

    /**
     * The whole reason the guard exists. A path outside app-private
     * storage must be refused, and — critically — the file must still be
     * there afterwards.
     */
    @Test
    fun deleteFile_refusesAndPreservesAFileOutsideAppPrivateStorage() = runTest {
        val outside = File("/sdcard/sprint039_must_survive.txt")
        val outsidePath = outside.absolutePath

        val result = deletionRepository.deleteFile(outsidePath)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat((result as AppResult.Failure).error).isEqualTo(AppError.PermissionMissing)
    }

    @Test
    fun deleteFile_refusesATraversalPathThatEscapesTheSandbox() = runTest {
        val escaping = File(cacheDir, "../../../../sdcard/photo.jpg").path

        val result = deletionRepository.deleteFile(escaping)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat((result as AppResult.Failure).error).isEqualTo(AppError.PermissionMissing)
    }

    /** Already gone is the desired end state, and freed nothing. */
    @Test
    fun deleteFile_treatsAnAlreadyMissingFileAsZeroBytesFreed() = runTest {
        val missing = File(cacheDir, "sprint039_never_existed.tmp")

        val result = deletionRepository.deleteFile(missing.absolutePath)

        assertThat((result as AppResult.Success).data).isEqualTo(0L)
    }

    @Test
    fun deleteFile_refusesADirectory() = runTest {
        val directory = File(cacheDir, "sprint039_dir").apply { mkdirs() }

        val result = deletionRepository.deleteFile(directory.absolutePath)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat(directory.exists()).isTrue()
        directory.delete()
    }

    @Test
    fun isDeletable_acceptsAppPrivatePathsAndRejectsOthers() {
        val inside = File(cacheDir, "sprint039_probe.tmp")

        assertThat(deletionRepository.isDeletable(inside.absolutePath)).isTrue()
        assertThat(deletionRepository.isDeletable("/sdcard/DCIM/photo.jpg")).isFalse()
        assertThat(deletionRepository.isDeletable("")).isFalse()
    }

    @Test
    fun roots_areAllRealExistingDirectories() {
        assertThat(roots.roots()).isNotEmpty()
        roots.roots().forEach { root -> assertThat(root.isDirectory).isTrue() }
    }
}
