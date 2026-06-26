package com.lifepilot.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timeline",
    indices = [
        Index(value = ["profile_id"]),
        Index(value = ["object_id"]),
        Index(value = ["timestamp"]),
        Index(value = ["domain"]),
    ]
)
data class TimelineEntity(
    @PrimaryKey
    @ColumnInfo(name = "timeline_id")
    val timelineId: String,

    @ColumnInfo(name = "profile_id")
    val profileId: String,

    @ColumnInfo(name = "source_id")
    val sourceId: String,

    @ColumnInfo(name = "source_type")
    val sourceType: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "summary")
    val summary: String?,

    @ColumnInfo(name = "object_id")
    val objectId: String?,

    @ColumnInfo(name = "object_type")
    val objectType: String?,

    @ColumnInfo(name = "domain")
    val domain: String?,
)
