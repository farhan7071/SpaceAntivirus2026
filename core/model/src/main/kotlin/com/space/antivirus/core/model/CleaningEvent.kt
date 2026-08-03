package com.space.antivirus.core.model

/**
 * What a running cleanup emits — Sprint 039.
 *
 * A sealed stream type rather than a bare `Flow<CleaningProgress>` so
 * that "still working" and "finished, here is the outcome" are different
 * types the collector must handle separately, instead of a progress
 * object the caller has to inspect for an `isComplete` flag and then
 * reconstruct a summary from. The terminal `Completed` event is the only
 * place a `CleaningSummary` comes from.
 */
sealed interface CleaningEvent {
    data class InProgress(val progress: CleaningProgress) : CleaningEvent
    data class Completed(val summary: CleaningSummary) : CleaningEvent
}
