package com.lifepilot.domain.usecase

import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.model.EventSource
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.TimelineEntry
import com.lifepilot.domain.model.schema.AiExtractionConfig
import com.lifepilot.domain.model.schema.LifecycleDefinition
import com.lifepilot.domain.model.schema.ObjectSchema
import com.lifepilot.domain.model.schema.SearchConfig
import com.lifepilot.domain.repository.EventRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.TimelineRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class CreateObjectUseCaseTest {

    private val objectRepository: ObjectRepository = mockk()
    private val eventRepository: EventRepository = mockk()
    private val timelineRepository: TimelineRepository = mockk()
    private val schemaEngine: SchemaEngine = mockk()

    private lateinit var useCase: CreateObjectUseCase

    private val testSchema = ObjectSchema(
        objectType = "passport",
        domain = "Identity",
        displayName = "Passport",
        icon = "badge",
        description = "International travel document",
        schemaVersion = 1,
        fields = emptyList(),
        lifecycle = LifecycleDefinition(emptyList(), emptyList(), "active"),
        reminderRules = emptyList(),
        searchConfig = SearchConfig(emptyList(), emptyList(), emptyList()),
        aiConfig = AiExtractionConfig(emptyList(), emptyList()),
    )

    private val testObject = LifeObject(
        objectId = "test-object-id",
        profileId = "profile-1",
        objectType = "passport",
        domain = "Identity",
        title = "My Passport",
        description = null,
        status = ObjectStatus.ACTIVE,
        metadata = emptyList(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    @Before
    fun setUp() {
        useCase = CreateObjectUseCase(
            objectRepository = objectRepository,
            eventRepository = eventRepository,
            timelineRepository = timelineRepository,
            schemaEngine = schemaEngine,
        )
    }

    @Test
    fun `invoke creates object and records event and timeline entry`() = runTest {
        coEvery { schemaEngine.getSchema("passport") } returns testSchema
        coEvery {
            objectRepository.createObject(
                profileId = "profile-1",
                objectType = "passport",
                domain = "Identity",
                title = "My Passport",
                description = null,
            )
        } returns testObject
        coEvery {
            eventRepository.recordEvent(
                objectId = "test-object-id",
                eventType = "OBJECT_CREATED",
                payload = any(),
                source = EventSource.SYSTEM,
                confidence = null,
            )
        } returns mockk()
        coEvery {
            timelineRepository.addTimelineEntry(any())
        } returns mockk()

        val result = useCase(
            profileId = "profile-1",
            objectType = "passport",
            title = "My Passport",
        )

        assertTrue(result.isSuccess)
        assertEquals(testObject, result.getOrNull())

        coVerify(exactly = 1) {
            objectRepository.createObject(
                profileId = "profile-1",
                objectType = "passport",
                domain = "Identity",
                title = "My Passport",
                description = null,
            )
        }
        coVerify(exactly = 1) {
            eventRepository.recordEvent(
                objectId = "test-object-id",
                eventType = "OBJECT_CREATED",
                payload = any(),
                source = EventSource.SYSTEM,
                confidence = null,
            )
        }
        coVerify(exactly = 1) { timelineRepository.addTimelineEntry(any()) }
    }

    @Test
    fun `invoke returns failure when schema not found`() = runTest {
        coEvery { schemaEngine.getSchema("unknown_type") } returns null

        val result = useCase(
            profileId = "profile-1",
            objectType = "unknown_type",
            title = "Test",
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Unknown object type") == true)
    }
}
