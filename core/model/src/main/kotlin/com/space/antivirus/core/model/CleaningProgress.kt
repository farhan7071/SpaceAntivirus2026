package com.space.antivirus.core.model

/**
 * A real-time snapshot of an in-progress cleanup — Sprint 039.
 *
 * Every field is a genuine count maintained by `CleanJunkFilesUseCase`
 * as it deletes, never an estimate. `totalItems` is known up front (it
 * is the size of the candidate list the user chose to clean), so unlike
 * a junk *scan*, a cleanup can honestly report a real percentage. See
 * `JunkScanProgress` for why the scan side cannot.
 *
 * `bytesFreed` counts only bytes from files that were actually deleted —
 * a file whose deletion failed contributes to `itemsFailed` and nothing
 * else. That distinction is the whole point of tracking failures
 * separately: "freed 240 MB" must never include bytes still on disk.
 */
data class CleaningProgress(
    val itemsProcessed: Int,
    val totalItems: Int,
    val itemsDeleted: Int,
    val itemsFailed: Int,
    val bytesFreed: Long,
    val currentItemName: String?,
) {
    init {
        require(itemsProcessed >= 0) { "itemsProcessed cannot be negative" }
        require(totalItems >= 0) { "totalItems cannot be negative" }
        require(itemsProcessed <= totalItems) {
            "itemsProcessed ($itemsProcessed) cannot exceed totalItems ($totalItems)"
        }
        require(itemsDeleted >= 0) { "itemsDeleted cannot be negative" }
        require(itemsFailed >= 0) { "itemsFailed cannot be negative" }
        require(itemsDeleted + itemsFailed == itemsProcessed) {
            "itemsDeleted ($itemsDeleted) + itemsFailed ($itemsFailed) must equal " +
                "itemsProcessed ($itemsProcessed)"
        }
        require(bytesFreed >= 0) { "bytesFreed cannot be negative" }
    }

    /** A real 0f..1f fraction, or null when there is nothing to clean —
     *  callers must render an indeterminate indicator rather than
     *  inventing a value for the empty case. */
    val fraction: Float?
        get() = if (totalItems == 0) null else itemsProcessed.toFloat() / totalItems.toFloat()

    val isComplete: Boolean
        get() = totalItems > 0 && itemsProcessed == totalItems

    companion object {
        fun starting(totalItems: Int): CleaningProgress = CleaningProgress(
            itemsProcessed = 0,
            totalItems = totalItems,
            itemsDeleted = 0,
            itemsFailed = 0,
            bytesFreed = 0L,
            currentItemName = null,
        )
    }
}
