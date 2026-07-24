package com.space.antivirus.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Room persistence for ScanStatistics (core:model, Sprint 004A/005/009).
 * One row per COMPLETED session — a PENDING/RUNNING session has no
 * statistics yet (matches ScanStatistics.EMPTY's meaning in domain).
 * sessionId is both the primary key and the foreign key: this is
 * genuinely a 1:1 relationship, not a 1:many one.
 */
@Entity(
    tableName = "scan_statistics",
    foreignKeys = [
        ForeignKey(
            entity = ScanSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ScanStatisticsEntity(
    @PrimaryKey val sessionId: String,
    val itemsScanned: Int,
    val threatsFound: Int,
    val itemsInconclusive: Int,
    val itemsTrusted: Int,
    val durationMillis: Long,
)
