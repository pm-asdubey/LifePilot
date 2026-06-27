package com.lifepilot.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifepilot.data.database.dao.MetadataDao
import com.lifepilot.data.database.entity.MetadataEntity
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
class MetadataDaoTest {

    private lateinit var db: LifePilotDatabase
    private lateinit var dao: MetadataDao

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LifePilotDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.metadataDao()

        db.profileDao().insertProfile(
            ProfileEntity(
                profileId = "profile1",
                displayName = "Test User",
                avatarPath = null,
                isPrimary = true,
                createdAt = 1_000_000L,
                updatedAt = 1_000_000L,
            )
        )
        db.objectDao().insertObject(
            ObjectEntity(
                objectId = "obj1",
                profileId = "profile1",
                objectType = "passport",
                domain = "identity",
                title = "Passport",
                description = null,
                status = "ACTIVE",
                archived = false,
                deleted = false,
                createdAt = 1_000_000L,
                updatedAt = 1_000_000L,
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertAndGetMetadata() = runTest {
        val entry = testMetadata("meta1", "expiry_date", "2030-01-01")
        dao.upsertMetadata(entry)

        val retrieved = dao.getMetadataByField("obj1", "expiry_date")
        assertNotNull(retrieved)
        assertEquals("2030-01-01", retrieved?.value)
    }

    @Test
    fun upsertMetadata_updatesExistingField() = runTest {
        dao.upsertMetadata(testMetadata("meta1", "expiry_date", "2025-01-01"))
        dao.upsertMetadata(testMetadata("meta1", "expiry_date", "2030-01-01"))

        val retrieved = dao.getMetadataByField("obj1", "expiry_date")
        assertEquals("2030-01-01", retrieved?.value)
    }

    @Test
    fun getMetadataByObject_returnsAllFieldsForObject() = runTest {
        dao.upsertMetadataBatch(
            listOf(
                testMetadata("meta1", "country", "India"),
                testMetadata("meta2", "expiry_date", "2030-01-01"),
                testMetadata("meta3", "issue_date", "2020-01-01"),
            )
        )

        val all = dao.getMetadataByObject("obj1")
        assertEquals(3, all.size)
    }

    @Test
    fun deleteMetadata_removesEntry() = runTest {
        dao.upsertMetadata(testMetadata("meta1", "country", "India"))
        dao.deleteMetadata("meta1")

        val retrieved = dao.getMetadataByField("obj1", "country")
        assertNull(retrieved)
    }

    @Test
    fun deleteAllMetadataForObject_clearsAll() = runTest {
        dao.upsertMetadataBatch(
            listOf(
                testMetadata("meta1", "country", "India"),
                testMetadata("meta2", "expiry_date", "2030-01-01"),
            )
        )
        dao.deleteAllMetadataForObject("obj1")

        val remaining = dao.getMetadataByObject("obj1")
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun searchMetadataValues_findsByValue() = runTest {
        dao.upsertMetadata(testMetadata("meta1", "country", "India"))
        dao.upsertMetadata(testMetadata("meta2", "expiry_date", "2030-01-01"))

        val results = dao.searchMetadataValues("profile1", "India")
        assertEquals(1, results.size)
        assertEquals("country", results[0].fieldId)
    }

    @Test
    fun observeMetadataByObject_emitsOnInsert() = runTest {
        dao.upsertMetadata(testMetadata("meta1", "country", "India"))

        val metadata = dao.observeMetadataByObject("obj1").first()
        assertEquals(1, metadata.size)
        assertEquals("India", metadata[0].value)
    }

    private fun testMetadata(id: String, fieldId: String, value: String) = MetadataEntity(
        metadataId = id,
        objectId = "obj1",
        fieldId = fieldId,
        fieldType = "TEXT",
        value = value,
        version = 1,
        confidence = null,
        source = "manual",
        updatedAt = 1_000_000L,
    )
}
