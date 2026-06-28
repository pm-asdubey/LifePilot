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
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val updatedAt: Instant,
)

enum class VerificationStatus {
    /** Value has not been reviewed by the user. */
    UNVERIFIED,
    /** User has explicitly confirmed the value is correct. */
    VERIFIED,
    /** Value was rejected by the user or is known to be wrong. */
    REJECTED,
}

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
