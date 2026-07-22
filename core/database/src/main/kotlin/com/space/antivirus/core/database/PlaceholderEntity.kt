package com.space.antivirus.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Temporary infrastructure entity. Room requires at least one entity in the
 * @Database annotation to compile. This will be replaced by real business
 * entities (e.g., ScanResultEntity) in Sprint 004+.
 */
@Entity(tableName = "placeholder_table")
data class PlaceholderEntity(
    @PrimaryKey val id: Int = 1
)
