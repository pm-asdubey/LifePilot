package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.EventSource
import com.lifepilot.domain.model.Relationship
import com.lifepilot.domain.model.TimelineEntry
import com.lifepilot.domain.model.TimelineSourceType
import com.lifepilot.domain.repository.EventRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.RelationshipRepository
import com.lifepilot.domain.repository.TimelineRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class LinkObjectsUseCase @Inject constructor(
    private val objectRepository: ObjectRepository,
    private val relationshipRepository: RelationshipRepository,
    private val eventRepository: EventRepository,
    private val timelineRepository: TimelineRepository,
) {
    suspend operator fun invoke(
        sourceObjectId: String,
        targetObjectId: String,
        relationshipType: String,
    ): Result<Relationship> = runCatching {
        val source = objectRepository.getObjectById(sourceObjectId)
            ?: error("Source object not found: $sourceObjectId")
        val target = objectRepository.getObjectById(targetObjectId)
            ?: error("Target object not found: $targetObjectId")

        val relationship = relationshipRepository.createRelationship(
            sourceObjectId = sourceObjectId,
            targetObjectId = targetObjectId,
            relationshipType = relationshipType,
        )

        val event = eventRepository.recordEvent(
            objectId = sourceObjectId,
            eventType = "OBJECT_LINKED",
            payload = """{"targetId":"$targetObjectId","targetTitle":"${target.title}","type":"$relationshipType"}""",
            source = EventSource.USER,
            confidence = null,
        )

        timelineRepository.addTimelineEntry(
            TimelineEntry(
                timelineId = UUID.randomUUID().toString(),
                sourceId = event.eventId,
                sourceType = TimelineSourceType.EVENT,
                timestamp = Instant.now(),
                title = "${source.title} linked to ${target.title}",
                summary = "Relationship type: $relationshipType",
                objectId = sourceObjectId,
                objectType = source.objectType,
            )
        )

        relationship
    }
}
