package com.space.antivirus.core.enumeration

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.EnumerationFilter
import com.space.antivirus.core.model.FileMetadata
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.ScanRequest
import com.space.antivirus.core.model.ScanScope
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.core.model.ScanType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File

/**
 * Tests the coordination logic in isolation from any real Android API —
 * all three collaborators are mocked, since EnumerationRepositoryImpl
 * itself contains zero direct Android imports (only its dependencies do).
 */
class EnumerationRepositoryImplTest {

    private val sampleApp = InstalledApplicationInfo(
        packageName = "com.example.app",
        appLabel = "Example",
        versionName = "1.0",
        versionCode = 1L,
        installedAtEpochMillis = 0L,
        isSystemApp = false,
        apkPath = "/data/app/example.apk",
        requestedPermissions = emptyList(),
    )

    private val sampleFile = FileMetadata(
        path = "/storage/emulated/0/Download/file.txt",
        name = "file.txt",
        sizeBytes = 100L,
        mimeType = "text/plain",
        lastModifiedEpochMillis = 0L,
        isDirectory = false,
    )

    @Test
    fun `resolveScanTargets combines file and application scopes`() = runTest {
        val appEnumerator = mockk<InstalledApplicationEnumerator> {
            every { enumerate() } returns AppResult.Success(listOf(sampleApp))
        }
        val pathResolver = mockk<ScanScopePathResolver> {
            every { resolve(ScanScope.DownloadsFolder) } returns AppResult.Success(File("/downloads"))
        }
        val treeWalker = mockk<FileTreeWalker> {
            every { walk(any(), any()) } returns listOf(sampleFile)
        }
        val repository = EnumerationRepositoryImpl(appEnumerator, pathResolver, treeWalker)

        val request = ScanRequest(
            id = "req-1",
            scanType = ScanType.FULL,
            scopes = listOf(ScanScope.InstalledApplications, ScanScope.DownloadsFolder),
            createdAtEpochMillis = 0L,
        )

        val result = repository.resolveScanTargets(request)

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        val targets = (result as AppResult.Success).data
        assertThat(targets).containsExactly(
            ScanTarget.ApplicationTarget(sampleApp),
            ScanTarget.FileTarget(sampleFile),
        )
    }

    @Test
    fun `resolveScanTargets fails fast when a file scope can't be resolved`() = runTest {
        val appEnumerator = mockk<InstalledApplicationEnumerator>()
        val pathResolver = mockk<ScanScopePathResolver> {
            every { resolve(ScanScope.DownloadsFolder) } returns AppResult.Failure(AppError.StorageUnavailable)
        }
        val treeWalker = mockk<FileTreeWalker>()
        val repository = EnumerationRepositoryImpl(appEnumerator, pathResolver, treeWalker)

        val request = ScanRequest(
            id = "req-2",
            scanType = ScanType.QUICK,
            scopes = listOf(ScanScope.DownloadsFolder),
            createdAtEpochMillis = 0L,
        )

        val result = repository.resolveScanTargets(request)

        assertThat(result).isEqualTo(AppResult.Failure(AppError.StorageUnavailable))
    }

    @Test
    fun `enumerateFiles delegates to the resolved root`() = runTest {
        val appEnumerator = mockk<InstalledApplicationEnumerator>()
        val root = File("/some/root")
        val pathResolver = mockk<ScanScopePathResolver> {
            every { resolve(ScanScope.InternalStorage) } returns AppResult.Success(root)
        }
        val treeWalker = mockk<FileTreeWalker> {
            every { walk(root, EnumerationFilter.DEFAULT) } returns listOf(sampleFile)
        }
        val repository = EnumerationRepositoryImpl(appEnumerator, pathResolver, treeWalker)

        val result = repository.enumerateFiles(ScanScope.InternalStorage)

        assertThat(result).isEqualTo(AppResult.Success(listOf(sampleFile)))
    }
}
