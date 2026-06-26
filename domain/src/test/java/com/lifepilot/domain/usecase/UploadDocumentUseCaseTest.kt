package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.Document
import com.lifepilot.domain.model.EventSource
import com.lifepilot.domain.repository.DocumentRepository
import com.lifepilot.domain.repository.EventRepository
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

class UploadDocumentUseCaseTest {

    private val documentRepository: DocumentRepository = mockk()
    private val eventRepository: EventRepository = mockk()
    private val timelineRepository: TimelineRepository = mockk()

    private lateinit var useCase: UploadDocumentUseCase

    private val testDocument = Document(
        documentId = "doc-1",
        objectId = "obj-1",
        documentType = "IMAGE",
        currentVersionId = "ver-1",
        versions = emptyList(),
        createdAt = Instant.now(),
    )

    @Before
    fun setUp() {
        useCase = UploadDocumentUseCase(
            documentRepository = documentRepository,
            eventRepository = eventRepository,
            timelineRepository = timelineRepository,
        )
    }

    @Test
    fun `invoke uploads document and records event and timeline entry`() = runTest {
        coEvery {
            documentRepository.uploadDocument(
                objectId = "obj-1",
                filePath = "content://test/file.jpg",
                originalName = "file.jpg",
                mimeType = "image/jpeg",
                documentType = "IMAGE",
            )
        } returns testDocument
        coEvery {
            eventRepository.recordEvent(
                objectId = "obj-1",
                eventType = "DOCUMENT_UPLOADED",
                payload = any(),
                source = EventSource.USER,
                confidence = null,
            )
        } returns mockk()
        coEvery { timelineRepository.addTimelineEntry(any()) } returns mockk()

        val result = useCase(
            objectId = "obj-1",
            filePath = "content://test/file.jpg",
            originalName = "file.jpg",
            mimeType = "image/jpeg",
            documentType = "IMAGE",
        )

        assertTrue(result.isSuccess)
        assertEquals(testDocument, result.getOrNull())

        coVerify(exactly = 1) {
            documentRepository.uploadDocument(any(), any(), any(), any(), any())
        }
        coVerify(exactly = 1) {
            eventRepository.recordEvent(
                objectId = "obj-1",
                eventType = "DOCUMENT_UPLOADED",
                payload = any(),
                source = EventSource.USER,
                confidence = null,
            )
        }
        coVerify(exactly = 1) { timelineRepository.addTimelineEntry(any()) }
    }

    @Test
    fun `invoke returns failure when repository throws`() = runTest {
        coEvery {
            documentRepository.uploadDocument(any(), any(), any(), any(), any())
        } throws RuntimeException("Storage full")

        val result = useCase(
            objectId = "obj-1",
            filePath = "content://test/file.jpg",
            originalName = "file.jpg",
            mimeType = "image/jpeg",
            documentType = "IMAGE",
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Storage full") == true)
    }
}
