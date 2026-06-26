package com.lifepilot.domain.model

import java.time.Instant

data class MetadataEntry(
    val metadataId: String,
    val objectId: String,
    val fieldId: String,
    val fieldType: MetadataFieldType,
    val value: String,
    val version: Int,
    val confidence: Float?,
    val source: MetadataSource,
    val updatedAt: Instant,
)

enum class MetadataFieldType {
    TEXT,
    NUMBER,
    DATE,
    CURRENCY,
    BOOLEAN,
    URL,
    PHONE,
    EMAIL,
    COUNTRY,
    ENUM,
    MULTILINE_TEXT,
}

enum class MetadataSource {
    USER,
    AI_EXTRACTED,
    OCR,
    SYSTEM,
}
