package com.space.antivirus.core.model

/**
 * One persisted cleanup, as shown by "Last cleanup" — Sprint 039.
 *
 * Deliberately records outcome totals only, never the paths of deleted
 * files. Keeping a durable list of what was on a user's device is a data
 * liability with no feature behind it, and this project stores no cloud
 * data and no file inventories by standing rule.
 */
data class CleanupRecord(
    val id: String,
    val completedAtEpochMillis: Long,
    val itemsDeleted: Int,
    val itemsFailed: Int,
    val bytesFreed: Long,
    val durationMillis: Long,
    val wasCancelled: Boolean,
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank" }
        require(itemsDeleted >= 0) { "itemsDeleted cannot be negative" }
        require(itemsFailed >= 0) { "itemsFailed cannot be negative" }
        require(bytesFreed >= 0) { "bytesFreed cannot be negative" }
        require(durationMillis >= 0) { "durationMillis cannot be negative" }
    }
}
