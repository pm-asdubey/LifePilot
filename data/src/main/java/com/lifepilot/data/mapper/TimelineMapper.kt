package com.lifepilot.data.mapper

import com.lifepilot.data.database.entity.TimelineEntity
import com.lifepilot.domain.model.TimelineEntry
import com.lifepilot.domain.model.TimelineSourceType
import java.time.Instant

fun TimelineEntity.toDomain(): TimelineEntry = TimelineEntry(
    timelineId = timelineId,
    sourceId = sourceId,
    sourceType = TimelineSourceType.valueOf(sourceType),
    timestamp = Instant.ofEpochMilli(timestamp),
    title = title,
    summary = summary,
    objectId = objectId,
    objectType = objectType,
)

fun TimelineEntry.toEntity(profileId: String, domain: String? = null): TimelineEntity = TimelineEntity(
    timelineId = timelineId,
    profileId = profileId,
    sourceId = sourceId,
    sourceType = sourceType.name,
    timestamp = timestamp.toEpochMilli(),
    title = title,
    summary = summary,
    objectId = objectId,
    objectType = objectType,
    domain = domain,
)
