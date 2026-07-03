package com.lifepilot.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goals",
    indices = [
        Index(value = ["profile_id"]),
        Index(value = ["status"]),
        Index(value = ["deadline"]),
        Index(value = ["project_id"]),
    ]
)
data class GoalEntity(
    @PrimaryKey
    @ColumnInfo(name = "goal_id")
    val goalId: String,

    @ColumnInfo(name = "profile_id")
    val profileId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "deadline")
    val deadline: Long?,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "progress")
    val progress: Int,

    @ColumnInfo(name = "object_id")
    val objectId: String?,

    @ColumnInfo(name = "notes")
    val notes: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "project_id")
    val projectId: String? = null,
)
