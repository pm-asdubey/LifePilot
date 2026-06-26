package com.lifepilot.data.mapper

import com.lifepilot.data.database.entity.ObjectEntity
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.ObjectStatus
import java.time.Instant

fun ObjectEntity.toDomain(metadata: List<MetadataEntry> = emptyList()): LifeObject = LifeObject(
    objectId = objectId,
    profileId = profileId,
    objectType = objectType,
    domain = domain,
    title = title,
    description = description,
    status = ObjectStatus.valueOf(status),
    metadata = metadata,
    archived = archived,
    deleted = deleted,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

fun LifeObject.toEntity(): ObjectEntity = ObjectEntity(
    objectId = objectId,
    profileId = profileId,
    objectType = objectType,
    domain = domain,
    title = title,
    description = description,
    status = status.name,
    archived = archived,
    deleted = deleted,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)
