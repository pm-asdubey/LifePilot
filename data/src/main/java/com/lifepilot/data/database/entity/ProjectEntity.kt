package com.lifepilot.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["profile_id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["profile_id"]),
        Index(value = ["status"]),
        Index(value = ["domain"]),
    ]
)
data class ProjectEntity(
    @PrimaryKey
    @ColumnInfo(name = "project_id")
    val projectId: String,

    @ColumnInfo(name = "profile_id")
    val profileId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "domain")
    val domain: String?,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "emoji", defaultValue = "🎯")
    val emoji: String = "🎯",

    @ColumnInfo(name = "target_date")
    val targetDate: Long? = null,

    @ColumnInfo(name = "is_ai_proposed", defaultValue = "0")
    val isAiProposed: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
