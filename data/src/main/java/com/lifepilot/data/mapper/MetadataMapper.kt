package com.lifepilot.data.mapper

import com.lifepilot.data.database.entity.MetadataEntity
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.MetadataFieldType
import com.lifepilot.domain.model.MetadataSource
import java.time.Instant

fun MetadataEntity.toDomain(): MetadataEntry = MetadataEntry(
    metadataId = metadataId,
    objectId = objectId,
    fieldId = fieldId,
    fieldType = MetadataFieldType.valueOf(fieldType),
    value = value,
    version = version,
    confidence = confidence,
    source = MetadataSource.valueOf(source),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

fun MetadataEntry.toEntity(): MetadataEntity = MetadataEntity(
    metadataId = metadataId,
    objectId = objectId,
    fieldId = fieldId,
    fieldType = fieldType.name,
    value = value,
    version = version,
    confidence = confidence,
    source = source.name,
    updatedAt = updatedAt.toEpochMilli(),
)
