package com.lifepilot.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "relationships",
    indices = [
        Index(value = ["source_object_id"]),
        Index(value = ["target_object_id"]),
        Index(value = ["relationship_type"]),
    ]
)
data class RelationshipEntity(
    @PrimaryKey
    @ColumnInfo(name = "relationship_id")
    val relationshipId: String,

    @ColumnInfo(name = "source_object_id")
    val sourceObjectId: String,

    @ColumnInfo(name = "target_object_id")
    val targetObjectId: String,

    @ColumnInfo(name = "relationship_type")
    val relationshipType: String,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
