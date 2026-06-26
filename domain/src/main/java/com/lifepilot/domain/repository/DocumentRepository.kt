package com.lifepilot.domain.repository

import com.lifepilot.domain.model.Document
import com.lifepilot.domain.model.DocumentVersion
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeDocumentsByObject(objectId: String): Flow<List<Document>>
    suspend fun getDocumentById(documentId: String): Document?
    suspend fun uploadDocument(
        objectId: String,
        filePath: String,
        originalName: String,
        mimeType: String,
        documentType: String,
    ): Document
    suspend fun addDocumentVersion(
        documentId: String,
        filePath: String,
        originalName: String,
        mimeType: String,
    ): DocumentVersion
    suspend fun deleteDocument(documentId: String)
    suspend fun getDocumentVersionById(versionId: String): DocumentVersion?
}
