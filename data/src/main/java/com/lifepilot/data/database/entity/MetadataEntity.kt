package com.lifepilot.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "metadata",
    foreignKeys = [
        ForeignKey(
            entity = ObjectEntity::class,
            parentColumns = ["object_id"],
            childColumns = ["object_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["object_id"]),
        Index(value = ["field_id"]),
        Index(value = ["object_id", "field_id"], unique = true),
    ]
)
data class MetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "metadata_id")
    val metadataId: String,

    @ColumnInfo(name = "object_id")
    val objectId: String,

    @ColumnInfo(name = "field_id")
    val fieldId: String,

    @ColumnInfo(name = "field_type")
    val fieldType: String,

    @ColumnInfo(name = "value")
    val value: String,

    @ColumnInfo(name = "version")
    val version: Int,

    @ColumnInfo(name = "confidence")
    val confidence: Float?,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
