package com.lifepilot.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
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
        Index(value = ["trigger_date"]),
        Index(value = ["status"]),
    ]
)
data class ReminderEntity(
    @PrimaryKey
    @ColumnInfo(name = "reminder_id")
    val reminderId: String,

    @ColumnInfo(name = "object_id")
    val objectId: String,

    @ColumnInfo(name = "reminder_type")
    val reminderType: String,

    @ColumnInfo(name = "trigger_date")
    val triggerDate: Long,

    @ColumnInfo(name = "priority")
    val priority: String,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "message")
    val message: String?,
)
