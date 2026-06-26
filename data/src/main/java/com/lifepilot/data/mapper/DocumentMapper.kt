package com.lifepilot.data.mapper

import com.lifepilot.data.database.entity.DocumentEntity
import com.lifepilot.data.database.entity.DocumentVersionEntity
import com.lifepilot.domain.model.Document
import com.lifepilot.domain.model.DocumentVersion
import java.time.Instant

fun DocumentEntity.toDomain(versions: List<DocumentVersionEntity>): Document = Document(
    documentId = documentId,
    objectId = objectId,
    documentType = documentType,
    currentVersionId = currentVersionId,
    versions = versions.map { it.toDomain() },
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun DocumentVersionEntity.toDomain(): DocumentVersion = DocumentVersion(
    versionId = versionId,
    documentId = documentId,
    filePath = filePath,
    originalName = originalName,
    mimeType = mimeType,
    checksum = checksum,
    sizeBytes = sizeBytes,
    uploadedAt = Instant.ofEpochMilli(uploadedAt),
    ocrText = ocrText,
)
