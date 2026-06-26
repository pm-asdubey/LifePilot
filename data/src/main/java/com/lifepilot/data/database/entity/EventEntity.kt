package com.lifepilot.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = ObjectEntity::class,
            parentColumns = ["object_id"],
            childColumns = ["object_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["object_id"]), Index(value = ["timestamp"])]
)
data class EventEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,

    @ColumnInfo(name = "object_id")
    val objectId: String,

    @ColumnInfo(name = "event_type")
    val eventType: String,

    @ColumnInfo(name = "payload")
    val payload: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "confidence")
    val confidence: Float?,
)
