package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.Profile
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ImportDataUseCaseTest {

    private val profileRepository: ProfileRepository = mockk()
    private val objectRepository: ObjectRepository = mockk()
    private val metadataRepository: MetadataRepository = mockk()

    private lateinit var useCase: ImportDataUseCase

    private val testProfile = Profile(
        profileId = "profile-1",
        displayName = "Test User",
        avatarPath = null,
        isPrimary = true,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    private val createdObject = LifeObject(
        objectId = "new-obj-1",
        profileId = "profile-1",
        objectType = "Passport",
        domain = "Identity",
        title = "My Passport",
        description = null,
        status = ObjectStatus.ACTIVE,
        metadata = emptyList(),
        archived = false,
        deleted = false,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    @Before
    fun setUp() {
        useCase = ImportDataUseCase(
            profileRepository = profileRepository,
            objectRepository = objectRepository,
            metadataRepository = metadataRepository,
        )
    }

    @Test
    fun `import valid payload creates objects with metadata`() = runTest {
        val json = """
            {
              "version": 1,
              "objects": [
                {
                  "objectType": "Passport",
                  "domain": "Identity",
                  "title": "My Passport",
                  "status": "ACTIVE",
                  "metadata": {
                    "nationality": "British",
                    "passport_number": "AB123456"
                  }
                }
              ]
            }
        """.trimIndent()

        coEvery { profileRepository.getProfileById("profile-1") } returns testProfile
        coEvery {
            objectRepository.createObject("profile-1", "Passport", "Identity", "My Passport", null)
        } returns createdObject
        coEvery { objectRepository.updateObjectStatus(any(), any()) } returns Unit
        coEvery { metadataRepository.upsertMetadataBatch(any()) } returns Unit

        val result = useCase(json, "profile-1")

        assertTrue(result.isSuccess)
        val importResult = result.getOrThrow()
        assertEquals(1, importResult.objectsImported)
        assertEquals(0, importResult.objectsSkipped)
        assertEquals(2, importResult.metadataEntriesImported)
        assertTrue(importResult.errors.isEmpty())
    }

    @Test
    fun `import empty objects array returns zero counts`() = runTest {
        val json = """{"version": 1, "objects": []}"""

        coEvery { profileRepository.getProfileById("profile-1") } returns testProfile

        val result = useCase(json, "profile-1")

        assertTrue(result.isSuccess)
        val importResult = result.getOrThrow()
        assertEquals(0, importResult.objectsImported)
        assertEquals(0, importResult.objectsSkipped)
    }

    @Test
    fun `import returns failure when profile not found`() = runTest {
        val json = """{"version": 1, "objects": []}"""

        coEvery { profileRepository.getProfileById("profile-999") } returns null

        val result = useCase(json, "profile-999")

        assertTrue(result.isFailure)
    }

    @Test
    fun `import skips objects when createObject fails`() = runTest {
        val json = """
            {
              "version": 1,
              "objects": [
                {
                  "objectType": "Passport",
                  "domain": "Identity",
                  "title": "Good Passport",
                  "metadata": {}
                },
                {
                  "objectType": "BrokenType",
                  "domain": "Identity",
                  "title": "Bad Object",
                  "metadata": {}
                }
              ]
            }
        """.trimIndent()

        coEvery { profileRepository.getProfileById("profile-1") } returns testProfile
        coEvery {
            objectRepository.createObject("profile-1", "Passport", "Identity", "Good Passport", null)
        } returns createdObject
        coEvery {
            objectRepository.createObject("profile-1", "BrokenType", "Identity", "Bad Object", null)
        } throws RuntimeException("Unknown type")
        coEvery { metadataRepository.upsertMetadataBatch(any()) } returns Unit

        val result = useCase(json, "profile-1")

        assertTrue(result.isSuccess)
        val importResult = result.getOrThrow()
        assertEquals(1, importResult.objectsImported)
        assertEquals(1, importResult.objectsSkipped)
        assertEquals(1, importResult.errors.size)
    }

    @Test
    fun `import returns failure for unsupported version`() = runTest {
        val json = """{"version": 99, "objects": []}"""

        coEvery { profileRepository.getProfileById("profile-1") } returns testProfile

        val result = useCase(json, "profile-1")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("version") == true)
    }
}
