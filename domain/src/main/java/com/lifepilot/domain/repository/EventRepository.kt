package com.lifepilot.domain.repository

import com.lifepilot.domain.model.Event
import com.lifepilot.domain.model.EventSource
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun observeEventsByObject(objectId: String): Flow<List<Event>>
    suspend fun recordEvent(
        objectId: String,
        eventType: String,
        payload: String,
        source: EventSource,
        confidence: Float?,
    ): Event
    suspend fun getEventsByObject(objectId: String): List<Event>
}
