package com.space.antivirus.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room persistence for ScanSession (core:model, Sprint 004A). Enums are
 * stored as their `.name` String rather than via a Room TypeConverter —
 * fewer moving parts, and this sprint is deliberately scoped to the
 * simplest correct schema (entities/DAOs only; the domain-model mapping
 * layer is Sprint 011's job, see ADR 0023).
 */
@Entity(tableName = "scan_sessions")
data class ScanSessionEntity(
    @PrimaryKey val id: String,
    val scanType: String,
    val state: String,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long?,
)
