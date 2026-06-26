package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.Document
import com.lifepilot.domain.model.EventSource
import com.lifepilot.domain.model.TimelineEntry
import com.lifepilot.domain.model.TimelineSourceType
import com.lifepilot.domain.repository.DocumentRepository
import com.lifepilot.domain.repository.EventRepository
import com.lifepilot.domain.repository.TimelineRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class UploadDocumentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val eventRepository: EventRepository,
    private val timelineRepository: TimelineRepository,
) {
    suspend operator fun invoke(
        objectId: String,
        filePath: String,
        originalName: String,
        mimeType: String,
        documentType: String,
    ): Result<Document> = runCatching {
        val document = documentRepository.uploadDocument(
            objectId = objectId,
            filePath = filePath,
            originalName = originalName,
            mimeType = mimeType,
            documentType = documentType,
        )

        eventRepository.recordEvent(
            objectId = objectId,
            eventType = "DOCUMENT_UPLOADED",
            payload = """{"documentId":"${document.documentId}","originalName":"$originalName"}""",
            source = EventSource.USER,
            confidence = null,
        )

        timelineRepository.addTimelineEntry(
            TimelineEntry(
                timelineId = UUID.randomUUID().toString(),
                sourceId = document.documentId,
                sourceType = TimelineSourceType.DOCUMENT,
                timestamp = Instant.now(),
                title = "Document uploaded: $originalName",
                summary = null,
                objectId = objectId,
                objectType = null,
            )
        )

        document
    }
}
