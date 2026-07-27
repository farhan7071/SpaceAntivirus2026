package com.space.antivirus.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room persistence for Detection (core:model, Sprint 004C/0015). A
 * genuine normalized table, not a JSON blob column on ThreatEntity —
 * Detection is a first-class domain concept with its own meaningful
 * fields (analyzer provenance, per-detection risk level per ADR 0015),
 * and collapsing that structure into an opaque serialized column would
 * be exactly the kind of shortcut this project avoids. CASCADE delete
 * from ThreatEntity: a detection never outlives the threat it's evidence
 * for.
 *
 * confidence added Sprint 029 — a real, pre-existing gap found while
 * extending this exact schema for another reason: Detection.confidence
 * was added to the domain model in Sprint 027 but this entity was never
 * updated to persist it, so it silently reverted to its default
 * (MODERATE) on every read from the database, regardless of what an
 * analyzer actually set. Not yet user-visible (nothing currently
 * displays confidence directly, and RiskLevel — which IS persisted
 * directly on ThreatEntity — is computed once at scan time, not
 * recomputed from reconstituted Detections on read), but a genuine data-
 * integrity gap worth closing while already here. See ADR 0043.
 */
@Entity(
    tableName = "detections",
    foreignKeys = [
        ForeignKey(
            entity = ThreatEntity::class,
            parentColumns = ["id"],
            childColumns = ["threatId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("threatId")],
)
data class DetectionEntity(
    @PrimaryKey val id: String,
    val threatId: String,
    val analyzerId: String,
    val threatType: String,
    val evidenceDescription: String,
    val riskLevel: String,
    val confidence: String,
)
