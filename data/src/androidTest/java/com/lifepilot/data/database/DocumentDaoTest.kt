package com.lifepilot.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifepilot.data.database.dao.DocumentDao
import com.lifepilot.data.database.entity.DocumentEntity
import com.lifepilot.data.database.entity.DocumentVersionEntity
import com.lifepilot.data.database.entity.ObjectEntity
import com.lifepilot.data.database.entity.ProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentDaoTest {

    private lateinit var db: LifePilotDatabase
    private lateinit var dao: DocumentDao

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LifePilotDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.documentDao()
        db.profileDao().insertProfile(testProfile())
        db.objectDao().insertObject(testObject("obj1"))
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun insertAndRetrieveDocument() = runTest {
        dao.insertDocument(testDocument("doc1", "obj1"))
        val retrieved = dao.getDocumentById("doc1")
        assertNotNull(retrieved)
        assertEquals("doc1", retrieved?.documentId)
        assertEquals("obj1", retrieved?.objectId)
    }

    @Test
    fun observeDocumentsByObject_returnsOnlyMatchingObject() = runTest {
        db.objectDao().insertObject(testObject("obj2"))
        dao.insertDocument(testDocument("doc1", "obj1"))
        dao.insertDocument(testDocument("doc2", "obj2"))

        val docs = dao.observeDocumentsByObject("obj1").first()
        assertEquals(1, docs.size)
        assertEquals("doc1", docs[0].documentId)
    }

    @Test
    fun deleteDocument_removesFromDb() = runTest {
        dao.insertDocument(testDocument("doc1", "obj1"))
        dao.deleteDocument("doc1")
        assertNull(dao.getDocumentById("doc1"))
    }

    @Test
    fun insertDocumentVersion_retrievable() = runTest {
        dao.insertDocument(testDocument("doc1", "obj1"))
        dao.insertDocumentVersion(testVersion("ver1", "doc1"))

        val versions = dao.getVersionsForDocument("doc1")
        assertEquals(1, versions.size)
        assertEquals("ver1", versions[0].versionId)
    }

    @Test
    fun updateVersionOcrText_persists() = runTest {
        dao.insertDocument(testDocument("doc1", "obj1"))
        dao.insertDocumentVersion(testVersion("ver1", "doc1"))
        dao.updateVersionOcrText("ver1", "extracted text content")

        val version = dao.getVersionById("ver1")
        assertEquals("extracted text content", version?.ocrText)
    }

    @Test
    fun searchDocuments_byDocumentType() = runTest {
        dao.insertDocument(testDocument("doc1", "obj1", type = "PASSPORT_SCAN"))
        dao.insertDocument(testDocument("doc2", "obj1", type = "BANK_STATEMENT"))

        val results = dao.searchDocuments("profile1", "PASSPORT")
        assertEquals(1, results.size)
        assertEquals("doc1", results[0].documentId)
    }

    @Test
    fun searchDocuments_byOriginalName() = runTest {
        dao.insertDocument(testDocument("doc1", "obj1"))
        dao.insertDocumentVersion(testVersion("ver1", "doc1", name = "passport_scan.pdf"))

        val results = dao.searchDocuments("profile1", "passport_scan")
        assertEquals(1, results.size)
        assertEquals("doc1", results[0].documentId)
    }

    @Test
    fun softDeleteObject_documentsRemainIntact() = runTest {
        dao.insertDocument(testDocument("doc1", "obj1"))
        db.objectDao().softDeleteObject("obj1", System.currentTimeMillis())

        // Soft delete marks object as deleted but documents persist in storage layer
        val docs = dao.getDocumentsByObjectSync("obj1")
        assertEquals(1, docs.size)
    }

    private fun testProfile() = ProfileEntity(
        profileId = "profile1",
        displayName = "Test User",
        avatarPath = null,
        isPrimary = true,
        createdAt = 1_000_000L,
        updatedAt = 1_000_000L,
    )

    private fun testObject(id: String) = ObjectEntity(
        objectId = id,
        profileId = "profile1",
        objectType = "passport",
        domain = "Identity",
        title = "Test Object",
        description = null,
        status = "ACTIVE",
        archived = false,
        deleted = false,
        createdAt = 1_000_000L,
        updatedAt = 1_000_000L,
    )

    private fun testDocument(
        id: String,
        objectId: String,
        type: String = "DOCUMENT",
    ) = DocumentEntity(
        documentId = id,
        objectId = objectId,
        documentType = type,
        currentVersionId = null,
        createdAt = 1_000_000L,
    )

    private fun testVersion(
        id: String,
        documentId: String,
        name: String = "file.pdf",
    ) = DocumentVersionEntity(
        versionId = id,
        documentId = documentId,
        filePath = "/storage/test/$name",
        originalName = name,
        mimeType = "application/pdf",
        checksum = "abc123",
        sizeBytes = 1024L,
        uploadedAt = 1_000_000L,
        ocrText = null,
    )
}
