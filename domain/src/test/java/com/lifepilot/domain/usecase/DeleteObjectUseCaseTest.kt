package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.Document
import com.lifepilot.domain.model.EventSource
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.repository.DocumentRepository
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

class DeleteObjectUseCaseTest {

    private val objectRepository: ObjectRepository = mockk()
    private val documentRepository: DocumentRepository = mockk()
    private val eventRepository: EventRepository = mockk()
    private val timelineRepository: TimelineRepository = mockk()

    private lateinit var useCase: DeleteObjectUseCase

    private val testObject = LifeObject(
        objectId = "obj-1",
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
        useCase = DeleteObjectUseCase(
            objectRepository = objectRepository,
            documentRepository = documentRepository,
            eventRepository = eventRepository,
            timelineRepository = timelineRepository,
        )
    }

    @Test
    fun `invoke deletes object and all associated documents`() = runTest {
        val doc1 = mockk<Document> { coEvery { documentId } returns "doc-1" }
        val doc2 = mockk<Document> { coEvery { documentId } returns "doc-2" }

        coEvery { objectRepository.getObjectById("obj-1") } returns testObject
        coEvery { documentRepository.getDocumentsByObject("obj-1") } returns listOf(doc1, doc2)
        coEvery { documentRepository.deleteDocument(any()) } returns Unit
        coEvery {
            eventRepository.recordEvent(any(), any(), any(), any(), any())
        } returns mockk { every { eventId } returns "event-1" }
        coEvery { timelineRepository.addTimelineEntry(any()) } answers { firstArg() }
        coEvery { objectRepository.deleteObject("obj-1") } returns Unit

        val result = useCase("obj-1")

        assertTrue(result.isSuccess)
        coVerify { documentRepository.deleteDocument("doc-1") }
        coVerify { documentRepository.deleteDocument("doc-2") }
        coVerify {
            eventRepository.recordEvent(
                objectId = "obj-1",
                eventType = "OBJECT_DELETED",
                payload = any(),
                source = EventSource.USER,
                confidence = null,
            )
        }
        coVerify { timelineRepository.addTimelineEntry(any()) }
        coVerify { objectRepository.deleteObject("obj-1") }
    }

    @Test
    fun `invoke succeeds with no documents`() = runTest {
        coEvery { objectRepository.getObjectById("obj-1") } returns testObject
        coEvery { documentRepository.getDocumentsByObject("obj-1") } returns emptyList()
        coEvery {
            eventRepository.recordEvent(any(), any(), any(), any(), any())
        } returns mockk { every { eventId } returns "event-1" }
        coEvery { timelineRepository.addTimelineEntry(any()) } answers { firstArg() }
        coEvery { objectRepository.deleteObject("obj-1") } returns Unit

        val result = useCase("obj-1")

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { documentRepository.deleteDocument(any()) }
    }

    @Test
    fun `invoke returns failure when object not found`() = runTest {
        coEvery { objectRepository.getObjectById("obj-999") } returns null

        val result = useCase("obj-999")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { objectRepository.deleteObject(any()) }
    }
}
