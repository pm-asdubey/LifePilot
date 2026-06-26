package com.lifepilot.data.repository

import com.lifepilot.data.database.dao.EventDao
import com.lifepilot.data.database.entity.EventEntity
import com.lifepilot.data.mapper.toDomain
import com.lifepilot.domain.model.Event
import com.lifepilot.domain.model.EventSource
import com.lifepilot.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
) : EventRepository {

    override fun observeEventsByObject(objectId: String): Flow<List<Event>> =
        eventDao.observeEventsByObject(objectId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun recordEvent(
        objectId: String,
        eventType: String,
        payload: String,
        source: EventSource,
        confidence: Float?,
    ): Event {
        val entity = EventEntity(
            eventId = UUID.randomUUID().toString(),
            objectId = objectId,
            eventType = eventType,
            payload = payload,
            timestamp = Instant.now().toEpochMilli(),
            source = source.name,
            confidence = confidence,
        )
        eventDao.insertEvent(entity)
        return entity.toDomain()
    }

    override suspend fun getEventsByObject(objectId: String): List<Event> =
        eventDao.getEventsByObject(objectId).map { it.toDomain() }
}

fun com.lifepilot.data.database.entity.EventEntity.toDomain(): com.lifepilot.domain.model.Event =
    com.lifepilot.domain.model.Event(
        eventId = eventId,
        objectId = objectId,
        eventType = eventType,
        payload = payload,
        timestamp = java.time.Instant.ofEpochMilli(timestamp),
        source = com.lifepilot.domain.model.EventSource.valueOf(source),
        confidence = confidence,
    )
