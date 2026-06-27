package com.lifepilot.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifepilot.data.database.dao.TimelineDao
import com.lifepilot.data.database.entity.ProfileEntity
import com.lifepilot.data.database.entity.TimelineEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimelineDaoTest {

    private lateinit var db: LifePilotDatabase
    private lateinit var dao: TimelineDao

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LifePilotDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.timelineDao()
        db.profileDao().insertProfile(testProfile())
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun insertAndObserveTimeline() = runTest {
        dao.insertTimelineEntry(testEntry("t1", timestamp = 3000L))
        dao.insertTimelineEntry(testEntry("t2", timestamp = 1000L))
        dao.insertTimelineEntry(testEntry("t3", timestamp = 2000L))

        val entries = dao.observeTimeline("profile1").first()
        assertEquals(3, entries.size)
        // Should be ordered by timestamp DESC
        assertEquals("t1", entries[0].timelineId)
        assertEquals("t3", entries[1].timelineId)
        assertEquals("t2", entries[2].timelineId)
    }

    @Test
    fun observeTimeline_isolatedByProfile() = runTest {
        db.profileDao().insertProfile(testProfile("profile2", "Other User"))
        dao.insertTimelineEntry(testEntry("t1", profileId = "profile1"))
        dao.insertTimelineEntry(testEntry("t2", profileId = "profile2"))

        val entries = dao.observeTimeline("profile1").first()
        assertEquals(1, entries.size)
        assertEquals("t1", entries[0].timelineId)
    }

    @Test
    fun observeTimelineByObject_filtersCorrectly() = runTest {
        dao.insertTimelineEntry(testEntry("t1", objectId = "obj1"))
        dao.insertTimelineEntry(testEntry("t2", objectId = "obj2"))
        dao.insertTimelineEntry(testEntry("t3", objectId = "obj1"))

        val entries = dao.observeTimelineByObject("obj1").first()
        assertEquals(2, entries.size)
        assertTrue(entries.all { it.objectId == "obj1" })
    }

    @Test
    fun observeTimelineByDomain_filtersCorrectly() = runTest {
        dao.insertTimelineEntry(testEntry("t1", domain = "Finance"))
        dao.insertTimelineEntry(testEntry("t2", domain = "Identity"))
        dao.insertTimelineEntry(testEntry("t3", domain = "Finance"))

        val entries = dao.observeTimelineByDomain("profile1", "Finance").first()
        assertEquals(2, entries.size)
        assertTrue(entries.all { it.domain == "Finance" })
    }

    @Test
    fun deleteTimelineEntry_removesEntry() = runTest {
        dao.insertTimelineEntry(testEntry("t1"))
        dao.deleteTimelineEntry("t1")

        val entries = dao.observeTimeline("profile1").first()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun observeTimeline_limitsTo100() = runTest {
        repeat(110) { i ->
            dao.insertTimelineEntry(testEntry("t$i", timestamp = i.toLong()))
        }

        val entries = dao.observeTimeline("profile1").first()
        assertEquals(100, entries.size)
    }

    private fun testProfile(id: String = "profile1", name: String = "Test User") = ProfileEntity(
        profileId = id,
        displayName = name,
        avatarPath = null,
        isPrimary = true,
        createdAt = 1_000_000L,
        updatedAt = 1_000_000L,
    )

    private fun testEntry(
        id: String,
        profileId: String = "profile1",
        objectId: String? = null,
        domain: String? = null,
        timestamp: Long = 1_000_000L,
    ) = TimelineEntity(
        timelineId = id,
        profileId = profileId,
        sourceId = "src_$id",
        sourceType = "OBJECT",
        timestamp = timestamp,
        title = "Timeline event $id",
        summary = null,
        objectId = objectId,
        objectType = if (objectId != null) "passport" else null,
        domain = domain,
    )
}
