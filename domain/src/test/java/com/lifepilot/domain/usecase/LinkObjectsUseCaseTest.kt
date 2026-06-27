package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.Event
import com.lifepilot.domain.model.EventSource
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.Relationship
import com.lifepilot.domain.model.RelationshipStatus
import com.lifepilot.domain.model.TimelineEntry
import com.lifepilot.domain.repository.EventRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.RelationshipRepository
import com.lifepilot.domain.repository.TimelineRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class LinkObjectsUseCaseTest {

    private val objectRepository: ObjectRepository = mockk()
    private val relationshipRepository: RelationshipRepository = mockk()
    private val eventRepository: EventRepository = mockk()
    private val timelineRepository: TimelineRepository = mockk()

    private lateinit var useCase: LinkObjectsUseCase

    private val sourceObject = LifeObject(
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

    private val targetObject = LifeObject(
        objectId = "obj-2",
        profileId = "profile-1",
        objectType = "travel",
        domain = "Travel",
        title = "USA Trip 2025",
        description = null,
        status = ObjectStatus.ACTIVE,
        metadata = emptyList(),
        archived = false,
        deleted = false,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    private val testRelationship = Relationship(
        relationshipId = "rel-1",
        sourceObjectId = "obj-1",
        targetObjectId = "obj-2",
        relationshipType = "RELATED_TO",
        status = RelationshipStatus.ACTIVE,
        createdAt = Instant.now(),
    )

    private val testEvent = Event(
        eventId = "event-1",
        objectId = "obj-1",
        eventType = "OBJECT_LINKED",
        payload = "",
        timestamp = Instant.now(),
        source = EventSource.USER,
        confidence = null,
    )

    @Before
    fun setUp() {
        useCase = LinkObjectsUseCase(
            objectRepository = objectRepository,
            relationshipRepository = relationshipRepository,
            eventRepository = eventRepository,
            timelineRepository = timelineRepository,
        )
    }

    @Test
    fun `links objects successfully`() = runTest {
        coEvery { objectRepository.getObjectById("obj-1") } returns sourceObject
        coEvery { objectRepository.getObjectById("obj-2") } returns targetObject
        coEvery { relationshipRepository.createRelationship(any(), any(), any()) } returns testRelationship
        coEvery { eventRepository.recordEvent(any(), any(), any(), any(), any()) } returns testEvent
        coEvery { timelineRepository.addTimelineEntry(any()) } returns Unit

        val result = useCase("obj-1", "obj-2", "RELATED_TO")

        assertTrue(result.isSuccess)
        coVerify { relationshipRepository.createRelationship("obj-1", "obj-2", "RELATED_TO") }
        coVerify { eventRepository.recordEvent("obj-1", "OBJECT_LINKED", any(), EventSource.USER, null) }
        coVerify { timelineRepository.addTimelineEntry(any<TimelineEntry>()) }
    }

    @Test
    fun `fails when source object not found`() = runTest {
        coEvery { objectRepository.getObjectById("obj-1") } returns null

        val result = useCase("obj-1", "obj-2", "RELATED_TO")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Source object not found") == true)
    }

    @Test
    fun `fails when target object not found`() = runTest {
        coEvery { objectRepository.getObjectById("obj-1") } returns sourceObject
        coEvery { objectRepository.getObjectById("obj-2") } returns null

        val result = useCase("obj-1", "obj-2", "RELATED_TO")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Target object not found") == true)
    }
}
