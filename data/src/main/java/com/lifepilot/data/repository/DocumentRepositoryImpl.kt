package com.lifepilot.data.repository

import android.net.Uri
import com.lifepilot.data.database.dao.DocumentDao
import com.lifepilot.data.database.entity.DocumentEntity
import com.lifepilot.data.database.entity.DocumentVersionEntity
import com.lifepilot.data.mapper.toDomain
import com.lifepilot.data.storage.FileStorageManager
import com.lifepilot.data.storage.PdfNormalizer
import java.io.File
import com.lifepilot.data.worker.WorkManagerScheduler
import com.lifepilot.domain.model.Document
import com.lifepilot.domain.model.DocumentVersion
import com.lifepilot.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class DocumentRepositoryImpl @Inject constructor(
    private val documentDao: DocumentDao,
    private val fileStorageManager: FileStorageManager,
    private val pdfNormalizer: PdfNormalizer,
    private val workManagerScheduler: WorkManagerScheduler,
) : DocumentRepository {

    override fun observeDocumentsByObject(objectId: String): Flow<List<Document>> =
        documentDao.observeDocumentsByObject(objectId).map { entities ->
            entities.map { entity ->
                val versions = documentDao.getVersionsForDocument(entity.documentId)
                entity.toDomain(versions)
            }
        }

    override fun observeDocumentById(documentId: String): Flow<Document?> =
        documentDao.observeDocumentById(documentId).map { entity ->
            entity?.let {
                val versions = documentDao.getVersionsForDocument(entity.documentId)
                entity.toDomain(versions)
            }
        }

    override suspend fun getDocumentById(documentId: String): Document? {
        val entity = documentDao.getDocumentById(documentId) ?: return null
        val versions = documentDao.getVersionsForDocument(documentId)
        return entity.toDomain(versions)
    }

    override suspend fun getDocumentsByObject(objectId: String): List<Document> =
        documentDao.getDocumentsByObjectSync(objectId).map { entity ->
            val versions = documentDao.getVersionsForDocument(entity.documentId)
            entity.toDomain(versions)
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

        val (storedPath, checksum, sizeBytes) = if (filePath.startsWith("content://")) {
            val stored = fileStorageManager.copyFromUri(
                uri = Uri.parse(filePath),
                objectId = objectId,
                originalName = originalName,
            )
            Triple(
                stored?.absolutePath ?: filePath,
                stored?.checksum ?: "",
                stored?.sizeBytes ?: 0L,
            )
        } else {
            Triple(filePath, computeLocalChecksum(filePath), java.io.File(filePath).length())
        }

        // Normalise every stored document to PDF so each record holds one downloadable format.
        val versionEntity = buildPdfNormalizedVersion(
            versionId, documentId, storedPath, originalName, mimeType, checksum, sizeBytes, now.toEpochMilli(),
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

        workManagerScheduler.scheduleDocumentOcr(versionId)
        Timber.d("Document created: $documentId for object $objectId")
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

        val docEntity = documentDao.getDocumentById(documentId)
            ?: error("Document not found: $documentId")

        val (storedPath, checksum, sizeBytes) = if (filePath.startsWith("content://")) {
            val stored = fileStorageManager.copyFromUri(
                uri = Uri.parse(filePath),
                objectId = docEntity.objectId,
                originalName = originalName,
            )
            Triple(
                stored?.absolutePath ?: filePath,
                stored?.checksum ?: "",
                stored?.sizeBytes ?: 0L,
            )
        } else {
            Triple(filePath, computeLocalChecksum(filePath), java.io.File(filePath).length())
        }

        // Normalise every stored document to PDF so each record holds one downloadable format.
        val versionEntity = buildPdfNormalizedVersion(
            versionId, documentId, storedPath, originalName, mimeType, checksum, sizeBytes, now.toEpochMilli(),
        )

        documentDao.insertDocumentVersion(versionEntity)
        documentDao.updateDocument(docEntity.copy(currentVersionId = versionId))

        workManagerScheduler.scheduleDocumentOcr(versionId)
        return versionEntity.toDomain()
    }

    override suspend fun deleteDocument(documentId: String) {
        val doc = documentDao.getDocumentById(documentId) ?: return
        val versions = documentDao.getVersionsForDocument(documentId)
        versions.forEach { version ->
            if (version.filePath.startsWith("/")) {
                fileStorageManager.deleteFile(version.filePath)
            }
        }
        documentDao.deleteDocument(documentId)
    }

    override suspend fun getDocumentVersionById(versionId: String): DocumentVersion? =
        documentDao.getVersionById(versionId)?.toDomain()

    /**
     * Builds a [DocumentVersionEntity], first normalising the stored file to PDF (via [PdfNormalizer])
     * so every record holds a single downloadable PDF regardless of how it was captured. Already-PDF
     * and non-image files pass through unchanged; the redundant source image is deleted after wrapping.
     */
    private fun buildPdfNormalizedVersion(
        versionId: String,
        documentId: String,
        storedPath: String,
        originalName: String,
        mimeType: String,
        checksum: String,
        sizeBytes: Long,
        uploadedAtMs: Long,
    ): DocumentVersionEntity {
        val pdf = runCatching { pdfNormalizer.toPdf(File(storedPath), mimeType) }.getOrNull()
        val finalPath = pdf?.absolutePath ?: storedPath
        if (pdf != null && storedPath != finalPath) fileStorageManager.deleteFile(storedPath)
        return DocumentVersionEntity(
            versionId = versionId,
            documentId = documentId,
            filePath = finalPath,
            originalName = if (pdf != null) "${originalName.substringBeforeLast('.', originalName)}.pdf" else originalName,
            mimeType = if (pdf != null) "application/pdf" else mimeType,
            checksum = pdf?.let { computeLocalChecksum(it.absolutePath) } ?: checksum,
            sizeBytes = pdf?.length() ?: sizeBytes,
            uploadedAt = uploadedAtMs,
            ocrText = null,
        )
    }

    private fun computeLocalChecksum(filePath: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = java.io.File(filePath).readBytes()
            digest.digest(bytes).joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }
    }
}
