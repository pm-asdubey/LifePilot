package com.lifepilot.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["object_id"]),
        Index(value = ["due_date"]),
        Index(value = ["status"]),
        Index(value = ["profile_id"]),
    ]
)
data class TaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "task_id")
    val taskId: String,

    @ColumnInfo(name = "profile_id")
    val profileId: String,

    @ColumnInfo(name = "object_id")
    val objectId: String?,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "priority")
    val priority: String,

    @ColumnInfo(name = "due_date")
    val dueDate: Long?,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long?,
)
