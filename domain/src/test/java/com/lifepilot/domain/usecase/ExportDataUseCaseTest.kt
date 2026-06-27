package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.MetadataFieldType
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.Profile
import com.lifepilot.domain.repository.DocumentRepository
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.repository.ReminderRepository
import com.lifepilot.domain.repository.TaskRepository
import com.lifepilot.domain.repository.TimelineRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ExportDataUseCaseTest {

    private val profileRepository: ProfileRepository = mockk()
    private val objectRepository: ObjectRepository = mockk()
    private val documentRepository: DocumentRepository = mockk()
    private val metadataRepository: MetadataRepository = mockk()
    private val taskRepository: TaskRepository = mockk()
    private val reminderRepository: ReminderRepository = mockk()
    private val timelineRepository: TimelineRepository = mockk()

    private lateinit var useCase: ExportDataUseCase

    private val testProfile = Profile(
        profileId = "profile-1",
        displayName = "Test User",
        avatarPath = null,
        isPrimary = true,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    private val testObject = LifeObject(
        objectId = "obj-1",
        profileId = "profile-1",
        objectType = "passport",
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
        useCase = ExportDataUseCase(
            profileRepository = profileRepository,
            objectRepository = objectRepository,
            documentRepository = documentRepository,
            metadataRepository = metadataRepository,
            taskRepository = taskRepository,
            reminderRepository = reminderRepository,
            timelineRepository = timelineRepository,
        )

        every { objectRepository.observeObjectsByProfile("profile-1") } returns flowOf(listOf(testObject))
        every { taskRepository.observeTasksByProfile("profile-1") } returns flowOf(emptyList())
        every { timelineRepository.observeTimeline("profile-1") } returns flowOf(emptyList())
        coEvery { metadataRepository.getMetadataByObject("obj-1") } returns emptyList()
        every { documentRepository.observeDocumentsByObject("obj-1") } returns flowOf(emptyList())
        coEvery { reminderRepository.getRemindersForObject("obj-1") } returns emptyList()
    }

    @Test
    fun `invoke returns success with correct object count`() = runTest {
        coEvery { profileRepository.getProfileById("profile-1") } returns testProfile

        val result = useCase("profile-1")

        assertTrue(result.isSuccess)
        val bundle = result.getOrThrow()
        assertEquals(1, bundle.objectCount)
        assertEquals(1, bundle.profileCount)
        assertEquals(0, bundle.taskCount)
    }

    @Test
    fun `invoke returns failure when profile not found`() = runTest {
        coEvery { profileRepository.getProfileById("missing") } returns null

        val result = useCase("missing")

        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke includes metadata in json payload`() = runTest {
        coEvery { profileRepository.getProfileById("profile-1") } returns testProfile
        coEvery { metadataRepository.getMetadataByObject("obj-1") } returns listOf(
            MetadataEntry(
                metadataId = "m1",
                objectId = "obj-1",
                fieldId = "expiry_date",
                fieldType = MetadataFieldType.DATE,
                value = "2030-01-01",
                version = 1,
                confidence = null,
                source = MetadataSource.USER,
                updatedAt = Instant.now(),
            )
        )

        val result = useCase("profile-1")

        assertTrue(result.isSuccess)
        val bundle = result.getOrThrow()
        assertTrue(bundle.jsonPayload.contains("expiry_date"))
        assertTrue(bundle.jsonPayload.contains("2030-01-01"))
    }

    @Test
    fun `invoke json payload is valid version 1 structure`() = runTest {
        coEvery { profileRepository.getProfileById("profile-1") } returns testProfile

        val result = useCase("profile-1")

        assertTrue(result.isSuccess)
        val payload = result.getOrThrow().jsonPayload
        assertTrue(payload.contains("\"version\": 1"))
        assertTrue(payload.contains("\"profile\""))
        assertTrue(payload.contains("\"objects\""))
        assertTrue(payload.contains("My Passport"))
    }

    @Test
    fun `invoke escapes special characters in title`() = runTest {
        val objectWithQuotes = testObject.copy(title = "John's \"Passport\"")
        every { objectRepository.observeObjectsByProfile("profile-1") } returns flowOf(listOf(objectWithQuotes))
        coEvery { profileRepository.getProfileById("profile-1") } returns testProfile

        val result = useCase("profile-1")

        assertTrue(result.isSuccess)
        val payload = result.getOrThrow().jsonPayload
        assertTrue("Quotes should be escaped", payload.contains("\\\"Passport\\\""))
    }
}
