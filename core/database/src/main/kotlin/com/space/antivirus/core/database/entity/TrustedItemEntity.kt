package com.space.antivirus.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room persistence for TrustedItem (core:model, Sprint 008). A single,
 * standalone table — no foreign keys, no relations to the scan-history
 * schema (ScanSessionEntity et al.). type is stored as its plain .name
 * string, same no-TypeConverter convention as every other entity since
 * Sprint 010 (ADR 0023).
 */
@Entity(tableName = "trusted_items")
data class TrustedItemEntity(
    @PrimaryKey val id: String,
    val identifier: String,
    val type: String,
    val addedAtEpochMillis: Long,
    val reason: String?,
)
