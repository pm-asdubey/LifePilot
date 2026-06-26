package com.lifepilot.domain.model

import java.time.Instant

data class Event(
    val eventId: String,
    val objectId: String,
    val eventType: String,
    val payload: String,
    val timestamp: Instant,
    val source: EventSource,
    val confidence: Float?,
)

enum class EventSource {
    USER,
    SYSTEM,
    AI,
    OCR,
    RULE_ENGINE,
}
