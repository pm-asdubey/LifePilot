package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.EventSource
import com.lifepilot.domain.model.TimelineEntry
import com.lifepilot.domain.model.TimelineSourceType
import com.lifepilot.domain.repository.DocumentRepository
import com.lifepilot.domain.repository.EventRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.TimelineRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class DeleteObjectUseCase @Inject constructor(
    private val objectRepository: ObjectRepository,
    private val documentRepository: DocumentRepository,
    private val eventRepository: EventRepository,
    private val timelineRepository: TimelineRepository,
) {
    suspend operator fun invoke(objectId: String): Result<Unit> = runCatching {
        val obj = objectRepository.getObjectById(objectId)
            ?: error("Object not found: $objectId")

        val event = eventRepository.recordEvent(
            objectId = objectId,
            eventType = "OBJECT_DELETED",
            payload = """{"objectType":"${obj.objectType}","title":"${obj.title}"}""",
            source = EventSource.USER,
            confidence = null,
        )

        timelineRepository.addTimelineEntry(
            TimelineEntry(
                timelineId = UUID.randomUUID().toString(),
                sourceId = event.eventId,
                sourceType = TimelineSourceType.USER_ACTION,
                timestamp = Instant.now(),
                title = "Deleted ${obj.title}",
                summary = "${obj.objectType} permanently removed",
                objectId = null,
                objectType = obj.objectType,
            )
        )

        objectRepository.deleteObject(objectId)
    }
}
