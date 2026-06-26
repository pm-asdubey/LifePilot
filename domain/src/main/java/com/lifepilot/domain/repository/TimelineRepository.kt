package com.lifepilot.domain.repository

import com.lifepilot.domain.model.TimelineEntry
import com.lifepilot.domain.model.TimelineSourceType
import kotlinx.coroutines.flow.Flow

interface TimelineRepository {
    fun observeTimeline(profileId: String): Flow<List<TimelineEntry>>
    fun observeTimelineByObject(objectId: String): Flow<List<TimelineEntry>>
    fun observeTimelineByDomain(profileId: String, domain: String): Flow<List<TimelineEntry>>
    suspend fun addTimelineEntry(entry: TimelineEntry): TimelineEntry
    suspend fun deleteTimelineEntry(timelineId: String)
}
