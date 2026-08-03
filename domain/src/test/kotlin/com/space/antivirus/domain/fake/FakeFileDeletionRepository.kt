package com.space.antivirus.domain.fake

import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.domain.repository.FileDeletionRepository

/**
 * Local to :domain's own test source set, same reasoning as every other
 * Fake* here. Records what was asked for so a test can assert that a
 * cleanup attempted exactly the files it should have, and no others.
 */
class FakeFileDeletionRepository(
    private val deletableRoots: List<String> = listOf("/app-private"),
    private val sizesByPath: Map<String, Long> = emptyMap(),
    private val failingPaths: Set<String> = emptySet(),
) : FileDeletionRepository {

    val attemptedPaths = mutableListOf<String>()

    override fun isDeletable(path: String): Boolean = deletableRoots.any { path.startsWith(it) }

    override suspend fun deleteFile(path: String): AppResult<Long> {
        attemptedPaths += path
        if (!isDeletable(path)) return AppResult.Failure(AppError.PermissionMissing)
        if (path in failingPaths) return AppResult.Failure(AppError.StorageUnavailable)
        return AppResult.Success(sizesByPath[path] ?: 0L)
    }
}
