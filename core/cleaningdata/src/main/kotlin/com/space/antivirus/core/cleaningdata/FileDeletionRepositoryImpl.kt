package com.space.antivirus.core.cleaningdata

import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.domain.repository.FileDeletionRepository
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * The real, and only, file deletion in this project — Sprint 039.
 *
 * Thin by design. It does exactly three things: check the containment
 * guard, measure the file, delete it. No batching, no ordering, no
 * progress — that coordination is `CleanJunkFilesUseCase`'s job, per
 * this project's standing "orchestration stays out of repositories"
 * split (ADR 0023).
 *
 * The guard is applied here rather than in the use case deliberately.
 * A use case is one caller among possible future many; putting the
 * boundary at the lowest level means a future background cleaner, a
 * settings action, or a mistake in a candidate list cannot route around
 * it. See `AppPrivateStorageRoots` for what the boundary is and ADR 0054
 * for why it sits there.
 */
class FileDeletionRepositoryImpl @Inject constructor(
    private val appPrivateStorageRoots: AppPrivateStorageRoots,
) : FileDeletionRepository {

    override fun isDeletable(path: String): Boolean = appPrivateStorageRoots.contains(path)

    override suspend fun deleteFile(path: String): AppResult<Long> = safeCall {
        if (!appPrivateStorageRoots.contains(path)) {
            // PermissionMissing, not Unexpected: from the caller's point
            // of view this is precisely "the app is not allowed to touch
            // that", which is the category this error exists for and the
            // one whose user-facing copy already says the right thing.
            return@safeCall AppResult.Failure(AppError.PermissionMissing)
        }

        val file = File(path)
        if (!file.exists()) {
            // Already gone. The desired end state holds and this call
            // freed nothing — both true, so neither a failure nor a
            // fabricated byte count.
            return@safeCall AppResult.Success(0L)
        }
        if (file.isDirectory) {
            // Directories are never junk candidates (JunkFileClassifier
            // classifies files only), so being handed one means the
            // caller is confused. Refusing beats a recursive delete that
            // nothing asked for.
            return@safeCall AppResult.Failure(
                AppError.InvalidScanConfiguration("Refusing to delete a directory: $path"),
            )
        }

        // Measured immediately before deletion, not taken from the
        // CleanableItem recorded at scan time — a file can grow or
        // shrink in between, and "bytes freed" must describe what was
        // actually on disk at the moment it was removed.
        val sizeBytes = file.length()
        if (!file.delete()) {
            return@safeCall AppResult.Failure(AppError.StorageUnavailable)
        }
        AppResult.Success(sizeBytes)
    }

    /** Same CancellationException discipline as SecurityRepositoryImpl
     *  (ADR 0024), TrustedItemRepositoryImpl and AnalyzerExecutor
     *  (ADR 0019) — rethrown before the general catch so structured
     *  cancellation isn't silently swallowed. Cancellation matters more
     *  here than anywhere it has been needed before: the user pressing
     *  Stop mid-cleanup is a first-class outcome, not an error. */
    private suspend fun <T> safeCall(block: suspend () -> AppResult<T>): AppResult<T> =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unexpected(e))
        }
}
