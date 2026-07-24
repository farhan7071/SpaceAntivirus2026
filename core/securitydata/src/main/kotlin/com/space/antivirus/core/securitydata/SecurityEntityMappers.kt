package com.space.antivirus.core.securitydata

import com.space.antivirus.core.database.entity.DetectionEntity
import com.space.antivirus.core.database.entity.ScanSessionEntity
import com.space.antivirus.core.database.entity.ScanStatisticsEntity
import com.space.antivirus.core.database.entity.ThreatEntity
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanSession
import com.space.antivirus.core.model.ScanSessionState
import com.space.antivirus.core.model.ScanStatistics
import com.space.antivirus.core.model.ScanType
import com.space.antivirus.core.model.Threat
import com.space.antivirus.core.model.ThreatType

/**
 * Entity <-> domain-model mapping, the piece Sprint 010 (ADR 0023)
 * deliberately deferred out of the DAO layer. Enums round-trip through
 * their plain .name string (matching how Sprint 010's entities store
 * them, no TypeConverter) via Enum.valueOf — this is the one place in the
 * whole pipeline where a mismatched stored string would surface, and it
 * surfaces as an ordinary thrown exception that SecurityRepositoryImpl's
 * safeCall wrapper converts to AppError.Unexpected, not a crash.
 */

fun ScanSession.toEntity(): ScanSessionEntity = ScanSessionEntity(
    id = id,
    scanType = scanType.name,
    state = state.name,
    startedAtEpochMillis = startedAtEpochMillis,
    completedAtEpochMillis = completedAtEpochMillis,
)

fun ScanSessionEntity.toDomain(): ScanSession = ScanSession(
    id = id,
    scanType = ScanType.valueOf(scanType),
    state = ScanSessionState.valueOf(state),
    startedAtEpochMillis = startedAtEpochMillis,
    completedAtEpochMillis = completedAtEpochMillis,
)

fun ScanStatistics.toEntity(sessionId: String): ScanStatisticsEntity = ScanStatisticsEntity(
    sessionId = sessionId,
    itemsScanned = itemsScanned,
    threatsFound = threatsFound,
    itemsInconclusive = itemsInconclusive,
    itemsTrusted = itemsTrusted,
    durationMillis = durationMillis,
)

fun ScanStatisticsEntity.toDomain(): ScanStatistics = ScanStatistics(
    itemsScanned = itemsScanned,
    threatsFound = threatsFound,
    itemsInconclusive = itemsInconclusive,
    itemsTrusted = itemsTrusted,
    durationMillis = durationMillis,
)

fun Threat.toEntity(sessionId: String): ThreatEntity = ThreatEntity(
    id = id,
    sessionId = sessionId,
    targetIdentifier = targetIdentifier,
    threatType = threatType.name,
    riskLevel = riskLevel.name,
    title = title,
    description = description,
    discoveredAtEpochMillis = discoveredAtEpochMillis,
)

/** Detections live in their own table (ADR 0023) — a ThreatEntity alone
 *  can't reconstruct a full Threat, so its already-mapped detections must
 *  be supplied by the caller (SecurityRepositoryImpl fetches them via
 *  DetectionDao first). */
fun ThreatEntity.toDomain(detections: List<Detection>): Threat = Threat(
    id = id,
    targetIdentifier = targetIdentifier,
    threatType = ThreatType.valueOf(threatType),
    riskLevel = RiskLevel.valueOf(riskLevel),
    title = title,
    description = description,
    detections = detections,
    discoveredAtEpochMillis = discoveredAtEpochMillis,
)

fun Detection.toEntity(threatId: String): DetectionEntity = DetectionEntity(
    id = id,
    threatId = threatId,
    analyzerId = analyzerId.value,
    threatType = threatType.name,
    evidenceDescription = evidenceDescription,
    riskLevel = riskLevel.name,
)

fun DetectionEntity.toDomain(): Detection = Detection(
    id = id,
    analyzerId = AnalyzerId(analyzerId),
    threatType = ThreatType.valueOf(threatType),
    evidenceDescription = evidenceDescription,
    riskLevel = RiskLevel.valueOf(riskLevel),
)
