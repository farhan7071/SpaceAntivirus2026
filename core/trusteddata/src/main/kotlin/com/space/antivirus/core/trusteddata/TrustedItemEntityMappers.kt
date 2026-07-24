package com.space.antivirus.core.trusteddata

import com.space.antivirus.core.database.entity.TrustedItemEntity
import com.space.antivirus.core.model.TrustedItem
import com.space.antivirus.core.model.TrustedItemType

fun TrustedItem.toEntity(): TrustedItemEntity = TrustedItemEntity(
    id = id,
    identifier = identifier,
    type = type.name,
    addedAtEpochMillis = addedAtEpochMillis,
    reason = reason,
)

fun TrustedItemEntity.toDomain(): TrustedItem = TrustedItem(
    id = id,
    identifier = identifier,
    type = TrustedItemType.valueOf(type),
    addedAtEpochMillis = addedAtEpochMillis,
    reason = reason,
)
