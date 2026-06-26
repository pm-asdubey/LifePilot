package com.lifepilot.data.repository

import com.lifepilot.data.database.dao.ObjectDao
import com.lifepilot.data.database.dao.TimelineDao
import com.lifepilot.data.mapper.toDomain
import com.lifepilot.data.mapper.toEntity
import com.lifepilot.domain.model.TimelineEntry
import com.lifepilot.domain.repository.TimelineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TimelineRepositoryImpl @Inject constructor(
    private val timelineDao: TimelineDao,
    private val objectDao: ObjectDao,
    private val preferenceManager: PreferenceManager,
) : TimelineRepository {

    override fun observeTimeline(profileId: String): Flow<List<TimelineEntry>> =
        timelineDao.observeTimeline(profileId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeTimelineByObject(objectId: String): Flow<List<TimelineEntry>> =
        timelineDao.observeTimelineByObject(objectId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeTimelineByDomain(profileId: String, domain: String): Flow<List<TimelineEntry>> =
        timelineDao.observeTimelineByDomain(profileId, domain).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun addTimelineEntry(entry: TimelineEntry): TimelineEntry {
        val profileId = preferenceManager.getActiveProfileId() ?: return entry
        val domain = entry.objectId?.let { objectDao.getObjectById(it)?.domain }
        timelineDao.insertTimelineEntry(entry.toEntity(profileId, domain))
        return entry
    }

    override suspend fun deleteTimelineEntry(timelineId: String) {
        timelineDao.deleteTimelineEntry(timelineId)
    }
}
