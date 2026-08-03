package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.CleanableItem
import com.space.antivirus.core.model.CleaningEvent
import com.space.antivirus.core.model.CleaningProgress
import com.space.antivirus.core.model.CleaningSummary
import com.space.antivirus.core.model.CleanupRecord
import com.space.antivirus.domain.repository.CleanupHistoryRepository
import com.space.antivirus.domain.repository.FileDeletionRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Deletes junk files for real, and reports what actually happened —
 * Sprint 039. The capability ADR 0035 deferred and Sprint 038 refused to
 * pretend to have.
 *
 * **Every number this emits is measured, not estimated.** `bytesFreed`
 * accumulates the size each file actually was at the moment it was
 * removed, as reported by `FileDeletionRepository.deleteFile`, never the
 * size recorded when it was scanned. A file whose deletion fails
 * contributes to `itemsFailed` and contributes zero bytes. `totalItems`
 * is the real length of the candidate list, so the progress fraction is
 * a real fraction.
 *
 * **A failed file does not abort the batch.** Files vanish between being
 * scanned and being deleted, and get held open by other processes; both
 * are ordinary. Stopping the whole cleanup because one file was stubborn
 * would leave the user worse off than counting it and moving on.
 *
 * **Cancellation is a first-class outcome, not an error.** The user
 * pressing Stop leaves real deletions already performed — those bytes
 * are genuinely freed, and the summary says so, with `wasCancelled` set
 * and `itemsSkipped` accounting for the rest. The record is still
 * persisted, inside `NonCancellable`, precisely because it describes
 * work that really happened: dropping it would lose a true fact about
 * the user's device because they pressed a button.
 *
 * The candidate list is filtered through `isDeletable` first, so items
 * outside app-private storage are never attempted. That filter is a
 * convenience for honest counting, not the safety boundary — the
 * boundary is enforced inside the repository, below this use case, where
 * no caller can route around it.
 */
class CleanJunkFilesUseCase @Inject constructor(
    private val fileDeletionRepository: FileDeletionRepository,
    private val cleanupHistoryRepository: CleanupHistoryRepository,
) {

    operator fun invoke(items: List<CleanableItem>): Flow<CleaningEvent> = flow {
        val deletable = items.filter { fileDeletionRepository.isDeletable(it.path) }
        val startedAt = System.currentTimeMillis()

        var itemsProcessed = 0
        var itemsDeleted = 0
        var itemsFailed = 0
        var bytesFreed = 0L
        var cancelled = false

        emit(CleaningEvent.InProgress(CleaningProgress.starting(deletable.size)))

        try {
            for (item in deletable) {
                currentCoroutineContext().ensureActive()

                when (val result = fileDeletionRepository.deleteFile(item.path)) {
                    is AppResult.Success -> {
                        itemsDeleted++
                        bytesFreed += result.data
                    }
                    is AppResult.Failure -> itemsFailed++
                    AppResult.Loading -> itemsFailed++
                }
                itemsProcessed++

                emit(
                    CleaningEvent.InProgress(
                        CleaningProgress(
                            itemsProcessed = itemsProcessed,
                            totalItems = deletable.size,
                            itemsDeleted = itemsDeleted,
                            itemsFailed = itemsFailed,
                            bytesFreed = bytesFreed,
                            currentItemName = item.name,
                        ),
                    ),
                )
            }
        } catch (cancellation: CancellationException) {
            cancelled = true
            // Persist what genuinely happened before rethrowing, so a
            // cancelled cleanup still leaves an accurate history entry.
            // NonCancellable because this coroutine is, by definition,
            // already cancelled at this point.
            withContext(NonCancellable) {
                persist(startedAt, deletable.size, itemsDeleted, itemsFailed, bytesFreed, cancelled = true)
            }
            throw cancellation
        }

        val summary = persist(
            startedAt = startedAt,
            itemsRequested = deletable.size,
            itemsDeleted = itemsDeleted,
            itemsFailed = itemsFailed,
            bytesFreed = bytesFreed,
            cancelled = cancelled,
        )
        emit(CleaningEvent.Completed(summary))
    }

    /**
     * Builds the summary and writes the history record.
     *
     * A failure to persist deliberately does not fail the cleanup: the
     * files are already gone, and reporting "cleanup failed" because a
     * history row could not be written would be false. The summary is
     * returned either way.
     */
    private suspend fun persist(
        startedAt: Long,
        itemsRequested: Int,
        itemsDeleted: Int,
        itemsFailed: Int,
        bytesFreed: Long,
        cancelled: Boolean,
    ): CleaningSummary {
        val completedAt = System.currentTimeMillis()
        val summary = CleaningSummary(
            itemsRequested = itemsRequested,
            itemsDeleted = itemsDeleted,
            itemsFailed = itemsFailed,
            bytesFreed = bytesFreed,
            durationMillis = (completedAt - startedAt).coerceAtLeast(0L),
            completedAtEpochMillis = completedAt,
            wasCancelled = cancelled,
        )
        cleanupHistoryRepository.record(
            CleanupRecord(
                id = UUID.randomUUID().toString(),
                completedAtEpochMillis = summary.completedAtEpochMillis,
                itemsDeleted = summary.itemsDeleted,
                itemsFailed = summary.itemsFailed,
                bytesFreed = summary.bytesFreed,
                durationMillis = summary.durationMillis,
                wasCancelled = summary.wasCancelled,
            ),
        )
        return summary
    }
}
