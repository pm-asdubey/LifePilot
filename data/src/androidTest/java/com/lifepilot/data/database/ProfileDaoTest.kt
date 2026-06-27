package com.lifepilot.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifepilot.data.database.dao.ProfileDao
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
class ProfileDaoTest {

    private lateinit var db: LifePilotDatabase
    private lateinit var dao: ProfileDao

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LifePilotDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.profileDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun insertAndRetrieveProfile() = runTest {
        dao.insertProfile(testProfile("p1", "Alice"))
        val profile = dao.getProfileById("p1")
        assertNotNull(profile)
        assertEquals("Alice", profile?.displayName)
    }

    @Test
    fun observeProfiles_returnsAll() = runTest {
        dao.insertProfile(testProfile("p1", "Alice"))
        dao.insertProfile(testProfile("p2", "Bob", isPrimary = false))

        val profiles = dao.observeProfiles().first()
        assertEquals(2, profiles.size)
    }

    @Test
    fun deleteProfile_removesIt() = runTest {
        dao.insertProfile(testProfile("p1", "Alice"))
        dao.deleteProfile("p1")

        assertNull(dao.getProfileById("p1"))
    }

    @Test
    fun updateProfile_persistsChanges() = runTest {
        dao.insertProfile(testProfile("p1", "Alice"))
        val updated = testProfile("p1", "Alice Updated")
        dao.updateProfile(updated)

        val retrieved = dao.getProfileById("p1")
        assertEquals("Alice Updated", retrieved?.displayName)
    }

    @Test
    fun setPrimary_updatesCorrectly() = runTest {
        dao.insertProfile(testProfile("p1", "Alice", isPrimary = true))
        dao.insertProfile(testProfile("p2", "Bob", isPrimary = false))

        dao.clearAllPrimary()
        dao.setPrimary("p2")

        val primary = dao.observePrimaryProfile().first()
        assertEquals("p2", primary?.profileId)
    }

    @Test
    fun observePrimaryProfile_returnsNullWhenNoneSet() = runTest {
        dao.insertProfile(testProfile("p1", "Alice", isPrimary = false))

        val primary = dao.observePrimaryProfile().first()
        assertNull(primary)
    }

    @Test
    fun clearAllPrimary_removesAllPrimaryFlags() = runTest {
        dao.insertProfile(testProfile("p1", "Alice", isPrimary = true))
        dao.insertProfile(testProfile("p2", "Bob", isPrimary = true))

        dao.clearAllPrimary()

        val primary = dao.observePrimaryProfile().first()
        assertNull(primary)
    }

    private fun testProfile(
        id: String,
        name: String,
        isPrimary: Boolean = true,
    ) = ProfileEntity(
        profileId = id,
        displayName = name,
        avatarPath = null,
        isPrimary = isPrimary,
        createdAt = 1_000_000L,
        updatedAt = 1_000_000L,
    )
}
