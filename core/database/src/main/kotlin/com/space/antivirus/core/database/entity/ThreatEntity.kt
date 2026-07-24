package com.space.antivirus.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room persistence for Threat (core:model, Sprint 004A/004C). Many
 * threats per session — CASCADE delete so removing a session (or
 * clearing history) doesn't leave orphaned threat rows.
 */
@Entity(
    tableName = "threats",
    foreignKeys = [
        ForeignKey(
            entity = ScanSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class ThreatEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val targetIdentifier: String,
    val threatType: String,
    val riskLevel: String,
    val title: String,
    val description: String,
    val discoveredAtEpochMillis: Long,
)
