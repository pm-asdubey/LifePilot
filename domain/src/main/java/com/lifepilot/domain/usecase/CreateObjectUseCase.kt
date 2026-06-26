package com.lifepilot.domain.usecase

import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.repository.EventRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.TimelineRepository
import com.lifepilot.domain.model.EventSource
import com.lifepilot.domain.model.TimelineEntry
import com.lifepilot.domain.model.TimelineSourceType
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class CreateObjectUseCase @Inject constructor(
    private val objectRepository: ObjectRepository,
    private val eventRepository: EventRepository,
    private val timelineRepository: TimelineRepository,
    private val schemaEngine: SchemaEngine,
) {
    suspend operator fun invoke(
        profileId: String,
        objectType: String,
        title: String,
        description: String? = null,
    ): Result<LifeObject> = runCatching {
        val schema = schemaEngine.getSchema(objectType)
            ?: error("Unknown object type: $objectType")

        val lifeObject = objectRepository.createObject(
            profileId = profileId,
            objectType = objectType,
            domain = schema.domain,
            title = title,
            description = description,
        )

        eventRepository.recordEvent(
            objectId = lifeObject.objectId,
            eventType = "OBJECT_CREATED",
            payload = """{"objectType":"$objectType","title":"$title"}""",
            source = EventSource.SYSTEM,
            confidence = null,
        )

        timelineRepository.addTimelineEntry(
            TimelineEntry(
                timelineId = UUID.randomUUID().toString(),
                sourceId = lifeObject.objectId,
                sourceType = TimelineSourceType.EVENT,
                timestamp = Instant.now(),
                title = "Created $title",
                summary = "New ${schema.displayName} added",
                objectId = lifeObject.objectId,
                objectType = objectType,
            )
        )

        lifeObject
    }
}
