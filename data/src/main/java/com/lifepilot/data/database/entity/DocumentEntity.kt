package com.lifepilot.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = ObjectEntity::class,
            parentColumns = ["object_id"],
            childColumns = ["object_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["object_id"])]
)
data class DocumentEntity(
    @PrimaryKey
    @ColumnInfo(name = "document_id")
    val documentId: String,

    @ColumnInfo(name = "object_id")
    val objectId: String,

    @ColumnInfo(name = "document_type")
    val documentType: String,

    @ColumnInfo(name = "current_version_id")
    val currentVersionId: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

@Entity(
    tableName = "document_versions",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["document_id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["document_id"])]
)
data class DocumentVersionEntity(
    @PrimaryKey
    @ColumnInfo(name = "version_id")
    val versionId: String,

    @ColumnInfo(name = "document_id")
    val documentId: String,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "original_name")
    val originalName: String,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "checksum")
    val checksum: String,

    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,

    @ColumnInfo(name = "uploaded_at")
    val uploadedAt: Long,

    @ColumnInfo(name = "ocr_text")
    val ocrText: String?,
)
