package com.space.antivirus.core.cleaningdata

import com.space.antivirus.core.database.entity.CleanupRecordEntity
import com.space.antivirus.core.model.CleanupRecord

/** Entity <-> domain mapping lives in its own file, same split as
 *  SecurityEntityMappers.kt (Sprint 011) and TrustedItemEntityMappers.kt
 *  (Sprint 012) — repositories orchestrate, mappers translate. */
internal fun CleanupRecordEntity.toDomain(): CleanupRecord = CleanupRecord(
    id = id,
    completedAtEpochMillis = completedAtEpochMillis,
    itemsDeleted = itemsDeleted,
    itemsFailed = itemsFailed,
    bytesFreed = bytesFreed,
    durationMillis = durationMillis,
    wasCancelled = wasCancelled,
)

internal fun CleanupRecord.toEntity(): CleanupRecordEntity = CleanupRecordEntity(
    id = id,
    completedAtEpochMillis = completedAtEpochMillis,
    itemsDeleted = itemsDeleted,
    itemsFailed = itemsFailed,
    bytesFreed = bytesFreed,
    durationMillis = durationMillis,
    wasCancelled = wasCancelled,
)
