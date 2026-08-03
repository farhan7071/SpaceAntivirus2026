package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.CleanableItem
import com.space.antivirus.core.model.JunkScanProgress
import com.space.antivirus.core.model.ScanScope
import com.space.antivirus.domain.cleaning.JunkFileClassifier
import com.space.antivirus.domain.repository.EnumerationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A junk scan that reports genuine progress while it runs — Sprint 039.
 *
 * Replaces `FindCleanableItemsUseCase` (Sprint 022), which this sprint
 * deletes. Keeping both would have left two use cases performing the
 * same enumerate-then-classify orchestration in two shapes, with the
 * older one having no production caller at all once the ViewModel moved
 * across — duplicated implementation kept alive by nothing but its own
 * test. `JunkFileClassifier` is untouched and is still the single place
 * junk policy lives.
 *
 * **Scopes.** Scans app-private internal files *and* the app's own cache
 * directory. Those are the only places this app may delete from (see
 * `AppPrivateStorageRoots` and ADR 0054), and scanning somewhere it
 * could never act on would surface junk the Clean button then failed to
 * remove — worse than not showing it.
 *
 * **Progress.** Emits a running `JunkScanProgress` as files are visited:
 * real counts, no total, no percentage. The total is unknowable until
 * the walk finishes, and inventing a denominator to animate a bar is the
 * fabrication this project has declined at every prior opportunity.
 *
 * **Cancellation** falls out of ordinary structured concurrency —
 * `enumerateFilesAsFlow` checks for it between files, so collecting this
 * flow in a cancellable coroutine is all a caller has to do.
 */
class ScanForJunkFilesUseCase @Inject constructor(
    private val enumerationRepository: EnumerationRepository,
    private val classifier: JunkFileClassifier,
) {

    /** Scanned in this order, and deliberately fixed rather than a
     *  parameter: the caller does not get to point the cleaner at an
     *  arbitrary location. */
    private val scopes = listOf(ScanScope.InternalStorage, ScanScope.ApplicationCache)

    operator fun invoke(): Flow<JunkScanEvent> = flow {
        val found = mutableListOf<CleanableItem>()
        var filesInspected = 0
        var bytesFound = 0L
        val nowEpochMillis = System.currentTimeMillis()

        emit(JunkScanEvent.InProgress(JunkScanProgress.STARTING))

        for (scope in scopes) {
            // Checked before streaming: enumerateFilesAsFlow cannot
            // report an unresolvable root per element, and completing
            // empty would tell the user their storage was clean when it
            // had never actually been read.
            val availability = enumerationRepository.isScopeAvailable(scope)
            if (availability is AppResult.Failure) {
                emit(JunkScanEvent.Failed(availability.error))
                return@flow
            }

            enumerationRepository.enumerateFilesAsFlow(scope).collect { file ->
                filesInspected++
                classifier.classify(file, nowEpochMillis)?.let { item ->
                    found += item
                    bytesFound += item.sizeBytes
                }
                emit(
                    JunkScanEvent.InProgress(
                        JunkScanProgress(
                            filesInspected = filesInspected,
                            junkFound = found.size,
                            bytesFound = bytesFound,
                            currentPath = file.path,
                        ),
                    ),
                )
            }
        }

        emit(JunkScanEvent.Completed(items = found.toList(), totalSizeBytes = bytesFound))
    }
}

/**
 * What a running junk scan emits. Same sealed-stream reasoning as
 * `CleaningEvent`: "still looking" and "here is the result" are
 * genuinely different things and shouldn't be one type the collector has
 * to interrogate.
 *
 * Lives here rather than in `core:model` because, unlike CleaningEvent,
 * it carries no data of its own beyond this use case's own inputs and
 * outputs — there is nothing for another layer to model.
 */
sealed interface JunkScanEvent {
    data class InProgress(val progress: JunkScanProgress) : JunkScanEvent
    data class Completed(val items: List<CleanableItem>, val totalSizeBytes: Long) : JunkScanEvent

    /** A scope could not be read at all. Distinct from Completed with an
     *  empty list, which means "looked, found nothing" — the two must
     *  never render as the same screen. */
    data class Failed(val error: AppError) : JunkScanEvent
}
