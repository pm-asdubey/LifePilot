package com.lifepilot.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifepilot.data.database.dao.ReminderDao
import com.lifepilot.data.database.entity.ObjectEntity
import com.lifepilot.data.database.entity.ProfileEntity
import com.lifepilot.data.database.entity.ReminderEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderDaoTest {

    private lateinit var db: LifePilotDatabase
    private lateinit var dao: ReminderDao

    private val baseTime = 1_000_000L

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LifePilotDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.reminderDao()

        db.profileDao().insertProfile(
            ProfileEntity(
                profileId = "profile1",
                displayName = "Test User",
                avatarPath = null,
                isPrimary = true,
                createdAt = baseTime,
                updatedAt = baseTime,
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
                createdAt = baseTime,
                updatedAt = baseTime,
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveReminder() = runTest {
        dao.insertReminder(testReminder("r1", triggerDate = baseTime + 1000))

        val reminders = dao.getRemindersForObject("obj1")
        assertEquals(1, reminders.size)
        assertEquals("r1", reminders[0].reminderId)
    }

    @Test
    fun observeUpcomingReminders_returnsOnlyScheduledBeforeCutoff() = runTest {
        val cutoff = baseTime + 5000
        dao.insertReminder(testReminder("r1", triggerDate = baseTime + 1000, status = "SCHEDULED"))
        dao.insertReminder(testReminder("r2", triggerDate = baseTime + 10000, status = "SCHEDULED"))
        dao.insertReminder(testReminder("r3", triggerDate = baseTime + 2000, status = "DISMISSED"))

        val upcoming = dao.observeUpcomingReminders(cutoff).first()
        assertEquals(1, upcoming.size)
        assertEquals("r1", upcoming[0].reminderId)
    }

    @Test
    fun updateStatus_changesReminderState() = runTest {
        dao.insertReminder(testReminder("r1", status = "SCHEDULED"))
        dao.updateStatus("r1", "DISMISSED")

        val reminders = dao.getRemindersForObject("obj1")
        assertEquals("DISMISSED", reminders.first().status)
    }

    @Test
    fun deleteReminder_removesFromObject() = runTest {
        dao.insertReminder(testReminder("r1"))
        dao.deleteReminder("r1")

        val reminders = dao.getRemindersForObject("obj1")
        assertTrue(reminders.isEmpty())
    }

    @Test
    fun observeRemindersByObject_orderedByTriggerDateAsc() = runTest {
        dao.insertReminder(testReminder("r2", triggerDate = baseTime + 2000))
        dao.insertReminder(testReminder("r1", triggerDate = baseTime + 1000))

        val reminders = dao.observeRemindersByObject("obj1").first()
        assertEquals("r1", reminders[0].reminderId)
        assertEquals("r2", reminders[1].reminderId)
    }

    private fun testReminder(
        id: String,
        triggerDate: Long = baseTime + 1000,
        status: String = "SCHEDULED",
    ) = ReminderEntity(
        reminderId = id,
        objectId = "obj1",
        reminderType = "expiry",
        triggerDate = triggerDate,
        priority = "HIGH",
        status = status,
        title = "Passport expiry reminder",
        message = null,
    )
}
