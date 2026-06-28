package com.lifepilot.data.mapper

import com.lifepilot.data.database.entity.MetadataEntity
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.MetadataFieldType
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.model.VerificationStatus
import java.time.Instant

fun MetadataEntity.toDomain(): MetadataEntry = MetadataEntry(
    metadataId = metadataId,
    objectId = objectId,
    fieldId = fieldId,
    fieldType = runCatching { MetadataFieldType.valueOf(fieldType) }.getOrElse { MetadataFieldType.TEXT },
    value = value,
    version = version,
    confidence = confidence,
    source = runCatching { MetadataSource.valueOf(source) }.getOrElse { MetadataSource.SYSTEM },
    verificationStatus = runCatching { VerificationStatus.valueOf(verificationStatus) }.getOrElse { VerificationStatus.UNVERIFIED },
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
    verificationStatus = verificationStatus.name,
    updatedAt = updatedAt.toEpochMilli(),
)
