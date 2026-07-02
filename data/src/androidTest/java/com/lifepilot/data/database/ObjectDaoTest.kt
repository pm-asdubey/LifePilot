package com.lifepilot.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifepilot.data.database.dao.ObjectDao
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
class ObjectDaoTest {

    private lateinit var db: LifePilotDatabase
    private lateinit var dao: ObjectDao

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LifePilotDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.objectDao()
        db.profileDao().insertProfile(testProfile())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveObject() = runTest {
        val obj = testObject("obj1")
        dao.insertObject(obj)

        val retrieved = dao.getObjectById("obj1")
        assertNotNull(retrieved)
        assertEquals("obj1", retrieved?.objectId)
        assertEquals("Test Object", retrieved?.title)
    }

    @Test
    fun observeObjectsByProfile_returnsNonDeletedObjects() = runTest {
        dao.insertObject(testObject("obj1", deleted = false))
        dao.insertObject(testObject("obj2", deleted = true))

        val objects = dao.observeObjectsByProfile("profile1").first()
        assertEquals(1, objects.size)
        assertEquals("obj1", objects[0].objectId)
    }

    @Test
    fun softDeleteObject_excludesFromQueries() = runTest {
        dao.insertObject(testObject("obj1"))
        dao.softDeleteObject("obj1", System.currentTimeMillis())

        val retrieved = dao.getObjectById("obj1")
        assertNull(retrieved)

        val all = dao.observeObjectsByProfile("profile1").first()
        assertTrue(all.isEmpty())
    }

    @Test
    fun archiveObject_setsArchivedFlag() = runTest {
        dao.insertObject(testObject("obj1"))
        dao.archiveObject("obj1", System.currentTimeMillis())

        val obj = dao.observeObjectsByProfile("profile1").first()
            .firstOrNull { it.objectId == "obj1" }
        assertNotNull(obj)
        assertEquals(true, obj!!.archived)
    }

    @Test
    fun updateStatus_updatesCorrectly() = runTest {
        dao.insertObject(testObject("obj1", status = "ACTIVE"))
        val now = System.currentTimeMillis()
        dao.updateStatus("obj1", "EXPIRED", now)

        val retrieved = dao.getObjectById("obj1")
        assertEquals("EXPIRED", retrieved?.status)
    }

    @Test
    fun searchObjects_findsByTitle() = runTest {
        dao.insertObject(testObject("obj1", title = "Passport Document"))
        dao.insertObject(testObject("obj2", title = "Job Offer Letter"))

        val results = dao.searchObjects("profile1", "passport")
        assertEquals(1, results.size)
        assertEquals("obj1", results[0].objectId)
    }

    @Test
    fun searchObjects_caseInsensitive() = runTest {
        dao.insertObject(testObject("obj1", title = "Insurance Policy"))

        val results = dao.searchObjects("profile1", "INSURANCE")
        assertEquals(1, results.size)
    }

    @Test
    fun searchObjects_noMatch_returnsEmpty() = runTest {
        dao.insertObject(testObject("obj1", title = "Passport"))

        val results = dao.searchObjects("profile1", "xyz_nonexistent")
        assertTrue(results.isEmpty())
    }

    @Test
    fun observeDomainCounts_groupsByDomain() = runTest {
        dao.insertObject(testObject("obj1", domain = "identity"))
        dao.insertObject(testObject("obj2", domain = "identity"))
        dao.insertObject(testObject("obj3", domain = "employment"))

        val counts = dao.observeDomainCounts("profile1").first()
        val countMap = counts.associate { it.domain to it.count }
        assertEquals(2, countMap["identity"])
        assertEquals(1, countMap["employment"])
    }

    private fun testProfile() = ProfileEntity(
        profileId = "profile1",
        displayName = "Test User",
        avatarPath = null,
        isPrimary = true,
        createdAt = 1_000_000L,
        updatedAt = 1_000_000L,
    )

    private fun testObject(
        id: String,
        title: String = "Test Object",
        domain: String = "identity",
        status: String = "ACTIVE",
        deleted: Boolean = false,
    ) = ObjectEntity(
        objectId = id,
        profileId = "profile1",
        objectType = "passport",
        domain = domain,
        title = title,
        description = null,
        status = status,
        archived = false,
        deleted = deleted,
        createdAt = 1_000_000L,
        updatedAt = 1_000_000L,
    )
}
