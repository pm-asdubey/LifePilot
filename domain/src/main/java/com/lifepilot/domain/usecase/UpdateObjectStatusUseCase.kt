package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.EventSource
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.TimelineEntry
import com.lifepilot.domain.model.TimelineSourceType
import com.lifepilot.domain.repository.EventRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.TimelineRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class UpdateObjectStatusUseCase @Inject constructor(
    private val objectRepository: ObjectRepository,
    private val eventRepository: EventRepository,
    private val timelineRepository: TimelineRepository,
) {
    suspend operator fun invoke(objectId: String, newStatus: ObjectStatus): Result<Unit> = runCatching {
        val obj = objectRepository.getObjectById(objectId)
            ?: error("Object not found: $objectId")

        val previousStatus = obj.status
        objectRepository.updateObjectStatus(objectId, newStatus)

        val event = eventRepository.recordEvent(
            objectId = objectId,
            eventType = "STATUS_CHANGED",
            payload = """{"from":"${previousStatus.name}","to":"${newStatus.name}"}""",
            source = EventSource.USER,
            confidence = null,
        )

        val displayStatus = newStatus.name
            .replace("_", " ")
            .lowercase()
            .replaceFirstChar { it.uppercase() }

        timelineRepository.addTimelineEntry(
            TimelineEntry(
                timelineId = UUID.randomUUID().toString(),
                sourceId = event.eventId,
                sourceType = TimelineSourceType.USER_ACTION,
                timestamp = Instant.now(),
                title = "Status updated to $displayStatus",
                summary = "${obj.title} status changed from ${previousStatus.name} to ${newStatus.name}",
                objectId = objectId,
                objectType = obj.objectType,
            )
        )
    }
}
