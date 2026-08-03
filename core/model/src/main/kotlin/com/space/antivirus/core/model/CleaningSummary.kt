package com.space.antivirus.core.model

/**
 * The outcome of a completed cleanup — Sprint 039. Produced by
 * `CleanJunkFilesUseCase` from its own real counters and persisted as a
 * `CleanupRecord`.
 *
 * `itemsRequested` can legitimately exceed `itemsDeleted + itemsFailed`:
 * that is what a cancelled cleanup looks like. Cancellation is a real,
 * expected outcome here, not an error — the user asked to stop, and the
 * files deleted before that point genuinely were deleted. `wasCancelled`
 * exists so the completion UI can say so plainly instead of reporting a
 * partial run as if it were a full one.
 */
data class CleaningSummary(
    val itemsRequested: Int,
    val itemsDeleted: Int,
    val itemsFailed: Int,
    val bytesFreed: Long,
    val durationMillis: Long,
    val completedAtEpochMillis: Long,
    val wasCancelled: Boolean,
) {
    init {
        require(itemsRequested >= 0) { "itemsRequested cannot be negative" }
        require(itemsDeleted >= 0) { "itemsDeleted cannot be negative" }
        require(itemsFailed >= 0) { "itemsFailed cannot be negative" }
        require(itemsDeleted + itemsFailed <= itemsRequested) {
            "itemsDeleted ($itemsDeleted) + itemsFailed ($itemsFailed) cannot exceed " +
                "itemsRequested ($itemsRequested)"
        }
        require(bytesFreed >= 0) { "bytesFreed cannot be negative" }
        require(durationMillis >= 0) { "durationMillis cannot be negative" }
    }

    /** Items never reached, because the user cancelled. */
    val itemsSkipped: Int
        get() = itemsRequested - itemsDeleted - itemsFailed
}
