package com.space.antivirus.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room persistence for CleanupRecord (core:model, Sprint 039). A single,
 * standalone table with no relation to the scan-history schema — a
 * cleanup is not a scan, and the two have never shared a lifecycle.
 * Same no-TypeConverter convention as every entity since Sprint 010
 * (ADR 0023).
 *
 * Stores outcome totals only. There is deliberately no column for the
 * paths of deleted files: a durable inventory of what was on a user's
 * device is a data liability with no feature behind it.
 */
@Entity(tableName = "cleanup_records")
data class CleanupRecordEntity(
    @PrimaryKey val id: String,
    val completedAtEpochMillis: Long,
    val itemsDeleted: Int,
    val itemsFailed: Int,
    val bytesFreed: Long,
    val durationMillis: Long,
    val wasCancelled: Boolean,
)
