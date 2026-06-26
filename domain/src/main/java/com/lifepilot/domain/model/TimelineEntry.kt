package com.lifepilot.domain.model

import java.time.Instant

data class TimelineEntry(
    val timelineId: String,
    val sourceId: String,
    val sourceType: TimelineSourceType,
    val timestamp: Instant,
    val title: String,
    val summary: String?,
    val objectId: String?,
    val objectType: String?,
)

enum class TimelineSourceType {
    EVENT,
    DOCUMENT,
    TASK,
    REMINDER,
    METADATA_CHANGE,
}
