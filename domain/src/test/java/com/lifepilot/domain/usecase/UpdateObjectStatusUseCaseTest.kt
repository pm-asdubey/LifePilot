package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.Event
import com.lifepilot.domain.model.EventSource
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.repository.EventRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.TimelineRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class UpdateObjectStatusUseCaseTest {

    private val objectRepository: ObjectRepository = mockk()
    private val eventRepository: EventRepository = mockk()
    private val timelineRepository: TimelineRepository = mockk()

    private lateinit var useCase: UpdateObjectStatusUseCase

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

    private val testEvent = Event(
        eventId = "event-1",
        objectId = "obj-1",
        eventType = "STATUS_CHANGED",
        payload = "",
        timestamp = Instant.now(),
        source = EventSource.USER,
        confidence = null,
    )

    @Before
    fun setUp() {
        useCase = UpdateObjectStatusUseCase(
            objectRepository = objectRepository,
            eventRepository = eventRepository,
            timelineRepository = timelineRepository,
        )
    }

    @Test
    fun `updates status and records event`() = runTest {
        coEvery { objectRepository.getObjectById("obj-1") } returns testObject
        coEvery { objectRepository.updateObjectStatus("obj-1", ObjectStatus.EXPIRED) } returns Unit
        coEvery { eventRepository.recordEvent(any(), any(), any(), any(), any()) } returns testEvent
        coEvery { timelineRepository.addTimelineEntry(any()) } answers { firstArg() }

        val result = useCase("obj-1", ObjectStatus.EXPIRED)

        assertTrue(result.isSuccess)
        coVerify { objectRepository.updateObjectStatus("obj-1", ObjectStatus.EXPIRED) }
        coVerify { eventRepository.recordEvent("obj-1", "STATUS_CHANGED", any(), EventSource.USER, null) }
    }

    @Test
    fun `fails when object not found`() = runTest {
        coEvery { objectRepository.getObjectById("obj-1") } returns null

        val result = useCase("obj-1", ObjectStatus.EXPIRED)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Object not found") == true)
    }

    @Test
    fun `records from and to in event payload`() = runTest {
        var capturedPayload = ""
        coEvery { objectRepository.getObjectById("obj-1") } returns testObject
        coEvery { objectRepository.updateObjectStatus(any(), any()) } returns Unit
        coEvery {
            eventRepository.recordEvent(any(), any(), capture(mutableListOf<String>().also { list ->
                capturedPayload = ""
            }), any(), any())
        } answers {
            capturedPayload = thirdArg()
            testEvent
        }
        coEvery { timelineRepository.addTimelineEntry(any()) } answers { firstArg() }

        useCase("obj-1", ObjectStatus.RENEWAL_DUE)

        // Payload should contain from/to status names
        assertTrue(capturedPayload.contains("ACTIVE"))
        assertTrue(capturedPayload.contains("RENEWAL_DUE"))
    }
}
