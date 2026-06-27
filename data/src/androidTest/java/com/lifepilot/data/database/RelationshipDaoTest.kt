package com.lifepilot.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifepilot.data.database.dao.RelationshipDao
import com.lifepilot.data.database.entity.ObjectEntity
import com.lifepilot.data.database.entity.ProfileEntity
import com.lifepilot.data.database.entity.RelationshipEntity
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
class RelationshipDaoTest {

    private lateinit var db: LifePilotDatabase
    private lateinit var dao: RelationshipDao

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LifePilotDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.relationshipDao()
        db.profileDao().insertProfile(testProfile())
        db.objectDao().insertObject(testObject("obj1"))
        db.objectDao().insertObject(testObject("obj2"))
        db.objectDao().insertObject(testObject("obj3"))
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun insertAndRetrieveRelationship() = runTest {
        dao.insertRelationship(testRelationship("r1", "obj1", "obj2"))

        val rels = dao.getRelationshipsByObject("obj1")
        assertEquals(1, rels.size)
        assertEquals("r1", rels[0].relationshipId)
    }

    @Test
    fun observeRelationships_bidirectional() = runTest {
        dao.insertRelationship(testRelationship("r1", "obj1", "obj2"))

        // obj2 should also see the relationship as target
        val fromTarget = dao.observeRelationshipsByObject("obj2").first()
        assertEquals(1, fromTarget.size)
        assertEquals("r1", fromTarget[0].relationshipId)
    }

    @Test
    fun getRelationshipBetween_detectsEitherDirection() = runTest {
        dao.insertRelationship(testRelationship("r1", "obj1", "obj2"))

        val forward = dao.getRelationshipBetween("obj1", "obj2")
        assertNotNull(forward)

        val reverse = dao.getRelationshipBetween("obj2", "obj1")
        assertNotNull(reverse)
        assertEquals("r1", reverse?.relationshipId)
    }

    @Test
    fun getRelationshipBetween_returnsNullIfNone() = runTest {
        val result = dao.getRelationshipBetween("obj1", "obj3")
        assertNull(result)
    }

    @Test
    fun deleteRelationship_removesIt() = runTest {
        dao.insertRelationship(testRelationship("r1", "obj1", "obj2"))
        dao.deleteRelationship("r1")

        val rels = dao.getRelationshipsByObject("obj1")
        assertTrue(rels.isEmpty())
    }

    @Test
    fun multipleRelationships_filteredPerObject() = runTest {
        dao.insertRelationship(testRelationship("r1", "obj1", "obj2"))
        dao.insertRelationship(testRelationship("r2", "obj2", "obj3"))
        dao.insertRelationship(testRelationship("r3", "obj1", "obj3"))

        val obj1Rels = dao.getRelationshipsByObject("obj1")
        assertEquals(2, obj1Rels.size)
        assertTrue(obj1Rels.any { it.relationshipId == "r1" })
        assertTrue(obj1Rels.any { it.relationshipId == "r3" })
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
        title = "Object $id",
        description = null,
        status = "ACTIVE",
        archived = false,
        deleted = false,
        createdAt = 1_000_000L,
        updatedAt = 1_000_000L,
    )

    private fun testRelationship(
        id: String,
        sourceId: String,
        targetId: String,
        type: String = "RELATED_TO",
    ) = RelationshipEntity(
        relationshipId = id,
        sourceObjectId = sourceId,
        targetObjectId = targetId,
        relationshipType = type,
        status = "ACTIVE",
        createdAt = 1_000_000L,
    )
}
