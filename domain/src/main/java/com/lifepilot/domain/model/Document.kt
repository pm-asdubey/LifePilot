package com.lifepilot.domain.model

import java.time.Instant

data class Document(
    val documentId: String,
    val objectId: String,
    val documentType: String,
    val currentVersionId: String?,
    val versions: List<DocumentVersion>,
    val createdAt: Instant,
)

data class DocumentVersion(
    val versionId: String,
    val documentId: String,
    val filePath: String,
    val originalName: String,
    val mimeType: String,
    val checksum: String,
    val sizeBytes: Long,
    val uploadedAt: Instant,
    val ocrText: String?,
)
