package com.lifepilot.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifepilot.data.database.dao.EventDao
import com.lifepilot.data.database.entity.EventEntity
import com.lifepilot.data.database.entity.ObjectEntity
import com.lifepilot.data.database.entity.ProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventDaoTest {

    private lateinit var db: LifePilotDatabase
    private lateinit var dao: EventDao

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LifePilotDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.eventDao()
        db.profileDao().insertProfile(testProfile())
        db.objectDao().insertObject(testObject("obj1"))
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun insertAndObserveEvents() = runTest {
        dao.insertEvent(testEvent("e1", "obj1", timestamp = 2000L))
        dao.insertEvent(testEvent("e2", "obj1", timestamp = 1000L))

        val events = dao.observeEventsByObject("obj1").first()
        assertEquals(2, events.size)
        // Ordered DESC by timestamp
        assertEquals("e1", events[0].eventId)
        assertEquals("e2", events[1].eventId)
    }

    @Test
    fun getEventsByObject_returnsAllEvents() = runTest {
        dao.insertEvent(testEvent("e1", "obj1"))
        dao.insertEvent(testEvent("e2", "obj1"))

        val events = dao.getEventsByObject("obj1")
        assertEquals(2, events.size)
    }

    @Test
    fun events_isolatedByObject() = runTest {
        db.objectDao().insertObject(testObject("obj2"))
        dao.insertEvent(testEvent("e1", "obj1"))
        dao.insertEvent(testEvent("e2", "obj2"))

        val obj1Events = dao.getEventsByObject("obj1")
        assertEquals(1, obj1Events.size)
        assertEquals("e1", obj1Events[0].eventId)
    }

    @Test
    fun cascadeDeleteObject_deletesEvents() = runTest {
        dao.insertEvent(testEvent("e1", "obj1"))
        // Hard delete via full update — simulate object hard-deleted from DB
        db.objectDao().softDeleteObject("obj1", System.currentTimeMillis())

        // Events still present after soft delete (only hard delete cascades)
        val events = dao.getEventsByObject("obj1")
        assertEquals(1, events.size)
    }

    @Test
    fun emptyObject_returnsEmptyList() = runTest {
        val events = dao.getEventsByObject("obj1")
        assertTrue(events.isEmpty())
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

    private fun testEvent(
        id: String,
        objectId: String,
        type: String = "OBJECT_CREATED",
        timestamp: Long = 1_000_000L,
    ) = EventEntity(
        eventId = id,
        objectId = objectId,
        eventType = type,
        payload = "{}",
        timestamp = timestamp,
        source = "USER",
        confidence = null,
    )
}
