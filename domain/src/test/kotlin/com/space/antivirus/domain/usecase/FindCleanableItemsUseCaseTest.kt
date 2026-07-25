package com.space.antivirus.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.FileMetadata
import com.space.antivirus.core.model.ScanScope
import com.space.antivirus.domain.cleaning.JunkFileClassifier
import com.space.antivirus.domain.repository.EnumerationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * mockk on EnumerationRepository (an interface, so no concrete-class
 * mocking concerns), fed through a REAL JunkFileClassifier — same
 * proportionate testing choice as every ViewModel test in this project,
 * applied here at the UseCase level: the classifier's own rules are
 * already covered exhaustively by JunkFileClassifierTest, so this file
 * verifies the enumerate-then-classify orchestration, not the rules
 * themselves.
 */
class FindCleanableItemsUseCaseTest {

    private val enumerationRepository = mockk<EnumerationRepository>()

    private fun buildUseCase(dispatcher: CoroutineDispatcher) =
        FindCleanableItemsUseCase(enumerationRepository, JunkFileClassifier(), dispatcher)

    private fun file(path: String, lastModifiedEpochMillis: Long = 0L) = FileMetadata(
        path = path,
        name = path.substringAfterLast('/'),
        sizeBytes = 1_000L,
        mimeType = null,
        lastModifiedEpochMillis = lastModifiedEpochMillis,
        isDirectory = false,
    )

    @Test
    fun `junk files are identified and non-junk files are excluded`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        coEvery { enumerationRepository.enumerateFiles(ScanScope.InternalStorage) } returns AppResult.Success(
            listOf(
                file("/data/data/com.example/cache/thumb.jpg"),
                file("/storage/emulated/0/DCIM/Camera/photo.jpg"),
                file("/storage/emulated/0/Documents/notes.bak"),
            ),
        )

        val result = buildUseCase(dispatcher)(ScanScope.InternalStorage)

        val items = (result as AppResult.Success).data
        assertThat(items).hasSize(2)
        assertThat(items.map { it.path }).containsExactly(
            "/data/data/com.example/cache/thumb.jpg",
            "/storage/emulated/0/Documents/notes.bak",
        )
    }

    @Test
    fun `an empty file list yields an empty result, not a failure`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        coEvery { enumerationRepository.enumerateFiles(ScanScope.InternalStorage) } returns
            AppResult.Success(emptyList())

        val result = buildUseCase(dispatcher)(ScanScope.InternalStorage)

        assertThat(result).isEqualTo(AppResult.Success(emptyList<Nothing>()))
    }

    @Test
    fun `a repository failure is propagated as-is, not swallowed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        coEvery { enumerationRepository.enumerateFiles(ScanScope.InternalStorage) } returns
            AppResult.Failure(AppError.PermissionMissing)

        val result = buildUseCase(dispatcher)(ScanScope.InternalStorage)

        assertThat(result).isEqualTo(AppResult.Failure(AppError.PermissionMissing))
    }

    @Test
    fun `the requested scope is passed through to the repository unchanged`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        coEvery { enumerationRepository.enumerateFiles(ScanScope.DownloadsFolder) } returns
            AppResult.Success(emptyList())

        buildUseCase(dispatcher)(ScanScope.DownloadsFolder)

        coVerify { enumerationRepository.enumerateFiles(ScanScope.DownloadsFolder) }
    }
}
