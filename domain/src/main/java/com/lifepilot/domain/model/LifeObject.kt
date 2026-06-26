package com.lifepilot.domain.model

import java.time.Instant

data class LifeObject(
    val objectId: String,
    val profileId: String,
    val objectType: String,
    val domain: String,
    val title: String,
    val description: String?,
    val status: ObjectStatus,
    val metadata: List<MetadataEntry>,
    val archived: Boolean,
    val deleted: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class ObjectStatus {
    DRAFT,
    ACTIVE,
    RENEWAL_DUE,
    EXPIRED,
    ARCHIVED,
}
