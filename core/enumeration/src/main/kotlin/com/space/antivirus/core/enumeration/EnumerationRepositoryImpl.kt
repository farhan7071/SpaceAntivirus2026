package com.space.antivirus.core.enumeration

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.EnumerationFilter
import com.space.antivirus.core.model.FileMetadata
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.ScanRequest
import com.space.antivirus.core.model.ScanScope
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.domain.repository.EnumerationRepository
import javax.inject.Inject

/**
 * Implements the contract domain defined. Coordinates the three pieces
 * above (path resolution, tree walking, package enumeration) but contains
 * no traversal or PackageManager logic of its own — kept a thin
 * coordinator on purpose so each piece stays independently testable.
 */
class EnumerationRepositoryImpl @Inject constructor(
    private val installedApplicationEnumerator: InstalledApplicationEnumerator,
    private val scanScopePathResolver: ScanScopePathResolver,
    private val fileTreeWalker: FileTreeWalker,
) : EnumerationRepository {

    override suspend fun enumerateInstalledApplications(): AppResult<List<InstalledApplicationInfo>> =
        installedApplicationEnumerator.enumerate()

    override suspend fun enumerateFiles(
        scope: ScanScope,
        filter: EnumerationFilter,
    ): AppResult<List<FileMetadata>> {
        val root = when (val rootResult = scanScopePathResolver.resolve(scope)) {
            is AppResult.Success -> rootResult.data
            is AppResult.Failure -> return AppResult.Failure(rootResult.error)
            AppResult.Loading -> return AppResult.Loading
        }
        return AppResult.Success(fileTreeWalker.walk(root, filter))
    }

    override suspend fun resolveScanTargets(request: ScanRequest): AppResult<List<ScanTarget>> {
        val targets = mutableListOf<ScanTarget>()

        for (scope in request.scopes) {
            if (scope is ScanScope.InstalledApplications) {
                when (val appsResult = enumerateInstalledApplications()) {
                    is AppResult.Success -> targets += appsResult.data.map { ScanTarget.ApplicationTarget(it) }
                    is AppResult.Failure -> return AppResult.Failure(appsResult.error)
                    AppResult.Loading -> return AppResult.Loading
                }
            } else {
                when (val filesResult = enumerateFiles(scope, request.filter)) {
                    is AppResult.Success -> targets += filesResult.data.map { ScanTarget.FileTarget(it) }
                    is AppResult.Failure -> return AppResult.Failure(filesResult.error)
                    AppResult.Loading -> return AppResult.Loading
                }
            }
        }

        return AppResult.Success(targets)
    }
}
