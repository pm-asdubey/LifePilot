package com.lifepilot.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifepilot.data.database.entity.DocumentEntity
import com.lifepilot.data.database.entity.DocumentVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE object_id = :objectId ORDER BY created_at DESC")
    fun observeDocumentsByObject(objectId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE document_id = :documentId")
    fun observeDocumentById(documentId: String): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE document_id = :documentId")
    suspend fun getDocumentById(documentId: String): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE document_id = :documentId")
    suspend fun deleteDocument(documentId: String)

    @Query("SELECT * FROM document_versions WHERE document_id = :documentId ORDER BY uploaded_at DESC")
    suspend fun getVersionsForDocument(documentId: String): List<DocumentVersionEntity>

    @Query("SELECT * FROM document_versions WHERE version_id = :versionId")
    suspend fun getVersionById(versionId: String): DocumentVersionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentVersion(version: DocumentVersionEntity)

    @Query("UPDATE document_versions SET ocr_text = :text WHERE version_id = :versionId")
    suspend fun updateVersionOcrText(versionId: String, text: String)

    @Query("""
        SELECT d.* FROM documents d
        INNER JOIN objects o ON d.object_id = o.object_id
        WHERE o.profile_id = :profileId
          AND o.deleted = 0
          AND d.document_type LIKE '%' || :query || '%'
        UNION
        SELECT d.* FROM documents d
        INNER JOIN document_versions dv ON d.document_id = dv.document_id
        INNER JOIN objects o ON d.object_id = o.object_id
        WHERE o.profile_id = :profileId
          AND o.deleted = 0
          AND dv.original_name LIKE '%' || :query || '%'
        ORDER BY created_at DESC
        LIMIT 20
    """)
    suspend fun searchDocuments(profileId: String, query: String): List<DocumentEntity>
}
