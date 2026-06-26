package com.lifepilot.data.repository

import com.lifepilot.data.database.dao.DocumentDao
import com.lifepilot.data.database.entity.DocumentEntity
import com.lifepilot.data.database.entity.DocumentVersionEntity
import com.lifepilot.data.mapper.toDomain
import com.lifepilot.domain.model.Document
import com.lifepilot.domain.model.DocumentVersion
import com.lifepilot.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class DocumentRepositoryImpl @Inject constructor(
    private val documentDao: DocumentDao,
) : DocumentRepository {

    override fun observeDocumentsByObject(objectId: String): Flow<List<Document>> =
        documentDao.observeDocumentsByObject(objectId).map { entities ->
            entities.map { entity ->
                val versions = documentDao.getVersionsForDocument(entity.documentId)
                entity.toDomain(versions)
            }
        }

    override suspend fun getDocumentById(documentId: String): Document? {
        val entity = documentDao.getDocumentById(documentId) ?: return null
        val versions = documentDao.getVersionsForDocument(documentId)
        return entity.toDomain(versions)
    }

    override suspend fun uploadDocument(
        objectId: String,
        filePath: String,
        originalName: String,
        mimeType: String,
        documentType: String,
    ): Document {
        val now = Instant.now()
        val documentId = UUID.randomUUID().toString()
        val versionId = UUID.randomUUID().toString()

        val versionEntity = DocumentVersionEntity(
            versionId = versionId,
            documentId = documentId,
            filePath = filePath,
            originalName = originalName,
            mimeType = mimeType,
            checksum = computeChecksum(filePath),
            sizeBytes = 0L,
            uploadedAt = now.toEpochMilli(),
            ocrText = null,
        )

        val documentEntity = DocumentEntity(
            documentId = documentId,
            objectId = objectId,
            documentType = documentType,
            currentVersionId = versionId,
            createdAt = now.toEpochMilli(),
        )

        documentDao.insertDocument(documentEntity)
        documentDao.insertDocumentVersion(versionEntity)

        return documentEntity.toDomain(listOf(versionEntity))
    }

    override suspend fun addDocumentVersion(
        documentId: String,
        filePath: String,
        originalName: String,
        mimeType: String,
    ): DocumentVersion {
        val now = Instant.now()
        val versionId = UUID.randomUUID().toString()

        val versionEntity = DocumentVersionEntity(
            versionId = versionId,
            documentId = documentId,
            filePath = filePath,
            originalName = originalName,
            mimeType = mimeType,
            checksum = computeChecksum(filePath),
            sizeBytes = 0L,
            uploadedAt = now.toEpochMilli(),
            ocrText = null,
        )

        documentDao.insertDocumentVersion(versionEntity)

        val docEntity = documentDao.getDocumentById(documentId)
        if (docEntity != null) {
            documentDao.updateDocument(docEntity.copy(currentVersionId = versionId))
        }

        return versionEntity.toDomain()
    }

    override suspend fun deleteDocument(documentId: String) {
        documentDao.deleteDocument(documentId)
    }

    override suspend fun getDocumentVersionById(versionId: String): DocumentVersion? =
        documentDao.getVersionById(versionId)?.toDomain()

    private fun computeChecksum(filePath: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = java.io.File(filePath).readBytes()
            digest.digest(bytes).joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }
    }
}
