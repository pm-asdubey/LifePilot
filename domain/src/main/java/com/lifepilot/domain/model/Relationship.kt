package com.lifepilot.domain.model

import java.time.Instant

data class Relationship(
    val relationshipId: String,
    val sourceObjectId: String,
    val targetObjectId: String,
    val relationshipType: String,
    val status: RelationshipStatus,
    val createdAt: Instant,
)

enum class RelationshipStatus {
    ACTIVE,
    INACTIVE,
    SUGGESTED,
}
