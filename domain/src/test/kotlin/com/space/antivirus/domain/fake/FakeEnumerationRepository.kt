package com.space.antivirus.domain.fake

import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.EnumerationFilter
import com.space.antivirus.core.model.FileMetadata
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.ScanRequest
import com.space.antivirus.core.model.ScanScope
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.domain.repository.EnumerationRepository

/**
 * Local to :domain's own test source set, same reasoning as every other
 * Fake* here. Deliberately minimal: resolveScanTargets just wraps the
 * given fileTargets/appTargets, regardless of which ScanScope was asked
 * for — good enough for testing RunScanRequestUseCase's orchestration
 * logic, which doesn't care how targets were resolved, only that they were.
 */
class FakeEnumerationRepository(
    private val fileTargets: List<ScanTarget.FileTarget> = emptyList(),
    private val appTargets: List<ScanTarget.ApplicationTarget> = emptyList(),
    private val forcedFailure: AppError? = null,
) : EnumerationRepository {

    override suspend fun enumerateInstalledApplications(): AppResult<List<InstalledApplicationInfo>> {
        forcedFailure?.let { return AppResult.Failure(it) }
        return AppResult.Success(appTargets.map { it.application })
    }

    override suspend fun enumerateFiles(
        scope: ScanScope,
        filter: EnumerationFilter,
    ): AppResult<List<FileMetadata>> {
        forcedFailure?.let { return AppResult.Failure(it) }
        return AppResult.Success(fileTargets.map { it.metadata })
    }

    override suspend fun resolveScanTargets(request: ScanRequest): AppResult<List<ScanTarget>> {
        forcedFailure?.let { return AppResult.Failure(it) }
        return AppResult.Success(fileTargets + appTargets)
    }
}
