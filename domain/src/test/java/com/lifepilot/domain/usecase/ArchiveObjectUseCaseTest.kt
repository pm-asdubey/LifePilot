package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.EventSource
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.repository.EventRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.TimelineRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ArchiveObjectUseCaseTest {

    private val objectRepository: ObjectRepository = mockk()
    private val eventRepository: EventRepository = mockk()
    private val timelineRepository: TimelineRepository = mockk()

    private lateinit var useCase: ArchiveObjectUseCase

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
        useCase = ArchiveObjectUseCase(
            objectRepository = objectRepository,
            eventRepository = eventRepository,
            timelineRepository = timelineRepository,
        )
    }

    @Test
    fun `invoke archives object and records event and timeline entry`() = runTest {
        coEvery { objectRepository.getObjectById("obj-1") } returns testObject
        coEvery { objectRepository.archiveObject("obj-1") } returns Unit
        coEvery {
            eventRepository.recordEvent(
                objectId = "obj-1",
                eventType = "OBJECT_ARCHIVED",
                payload = any<String>(),
                source = EventSource.USER,
                confidence = null,
            )
        } returns mockk { every { eventId } returns "event-1" }
        coEvery { timelineRepository.addTimelineEntry(any()) } answers { firstArg() }

        val result = useCase("obj-1")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { objectRepository.archiveObject("obj-1") }
        coVerify(exactly = 1) {
            eventRepository.recordEvent(
                objectId = "obj-1",
                eventType = "OBJECT_ARCHIVED",
                payload = any<String>(),
                source = EventSource.USER,
                confidence = null,
            )
        }
        coVerify(exactly = 1) { timelineRepository.addTimelineEntry(any()) }
    }

    @Test
    fun `invoke returns failure when object not found`() = runTest {
        coEvery { objectRepository.getObjectById("obj-999") } returns null

        val result = useCase("obj-999")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not found") == true)
    }

    @Test
    fun `invoke returns failure when repository throws`() = runTest {
        coEvery { objectRepository.getObjectById("obj-1") } returns testObject
        coEvery { objectRepository.archiveObject("obj-1") } throws RuntimeException("DB error")

        val result = useCase("obj-1")

        assertTrue(result.isFailure)
    }
}
