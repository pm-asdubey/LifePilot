package com.lifepilot.data

import com.lifepilot.data.database.dao.ObjectDao
import com.lifepilot.data.database.entity.ObjectEntity
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.data.repository.SearchRepositoryImpl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchRepositoryImplTest {

    private val objectDao: ObjectDao = mockk()
    private val preferenceManager: PreferenceManager = mockk()
    private lateinit var repository: SearchRepositoryImpl

    private fun objectEntity(
        id: String,
        title: String,
        type: String,
        domain: String,
        description: String? = null,
    ) = ObjectEntity(
        objectId = id,
        profileId = "profile-1",
        objectType = type,
        domain = domain,
        title = title,
        description = description,
        status = "ACTIVE",
        archived = false,
        deleted = false,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    @Before
    fun setUp() {
        repository = SearchRepositoryImpl(objectDao, preferenceManager)
    }

    @Test
    fun `search returns empty list for blank query`() = runTest {
        val results = repository.search("  ", "profile-1")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `search returns objects matching title`() = runTest {
        val entities = listOf(
            objectEntity("1", "My Passport", "passport", "Identity"),
            objectEntity("2", "Car Insurance", "insurance", "Finance"),
        )
        coEvery { objectDao.searchObjects("profile-1", "passport") } returns listOf(entities[0])

        val results = repository.search("passport", "profile-1")

        assertEquals(1, results.size)
        assertEquals("My Passport", results[0].title)
    }

    @Test
    fun `search returns results sorted by score`() = runTest {
        val entities = listOf(
            objectEntity("1", "Passport", "passport", "Identity"),
            objectEntity("2", "Old Passport Info", "document", "Identity"),
        )
        coEvery { objectDao.searchObjects("profile-1", "passport") } returns entities

        val results = repository.search("passport", "profile-1")

        assertEquals(2, results.size)
        assertTrue(results[0].relevanceScore >= results[1].relevanceScore)
    }
}
