package com.lifepilot.domain.model

import java.time.Instant

/**
 * Structured AI context for a single Object.
 *
 * Stored as JSON in the metadata field with fieldId = "ai_context".
 * Rendered as formatted text on Object Detail; reasoned over structurally by the engine.
 */
data class AiObjectContext(
    val summary: String,
    val importantFacts: List<String>,
    val currentSituation: String,
    val suggestions: List<String>,
    val lastUpdated: Instant,
    val confidence: Float,
)
