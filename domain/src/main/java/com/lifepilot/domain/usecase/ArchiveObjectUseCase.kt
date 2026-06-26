package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.EventSource
import com.lifepilot.domain.repository.EventRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.TimelineRepository
import java.time.Instant
import javax.inject.Inject

class ArchiveObjectUseCase @Inject constructor(
    private val objectRepository: ObjectRepository,
    private val eventRepository: EventRepository,
    private val timelineRepository: TimelineRepository,
) {
    suspend operator fun invoke(objectId: String): Result<Unit> = runCatching {
        val obj = objectRepository.getObjectById(objectId)
            ?: error("Object not found: $objectId")

        objectRepository.archiveObject(objectId)

        eventRepository.recordEvent(
            objectId = objectId,
            eventType = "OBJECT_ARCHIVED",
            payload = mapOf("objectType" to obj.objectType, "title" to obj.title),
            source = EventSource.USER,
            confidence = null,
        )

        timelineRepository.addTimelineEntry(
            com.lifepilot.domain.model.TimelineEntry(
                timelineId = java.util.UUID.randomUUID().toString(),
                objectId = objectId,
                eventId = null,
                title = "${obj.title} archived",
                summary = "Object moved to archive",
                timestamp = Instant.now(),
                sourceType = com.lifepilot.domain.model.TimelineSourceType.USER_ACTION,
            )
        )
    }
}
