package com.lifepilot.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "objects",
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
        Index(value = ["object_type"]),
        Index(value = ["domain"]),
        Index(value = ["status"]),
    ]
)
data class ObjectEntity(
    @PrimaryKey
    @ColumnInfo(name = "object_id")
    val objectId: String,

    @ColumnInfo(name = "profile_id")
    val profileId: String,

    @ColumnInfo(name = "object_type")
    val objectType: String,

    @ColumnInfo(name = "domain")
    val domain: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "archived")
    val archived: Boolean,

    @ColumnInfo(name = "deleted")
    val deleted: Boolean,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
