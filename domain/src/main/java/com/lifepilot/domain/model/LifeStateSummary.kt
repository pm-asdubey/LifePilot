package com.lifepilot.domain.model

/**
 * Lightweight snapshot of what exists in the life state — no content, just structure.
 * Used by [com.lifepilot.domain.engine.RetrievalPlanner] to decide which domains
 * are worth fetching in full before a main AI call.
 */
data class LifeStateSummary(
    val domainObjectCounts: Map<String, Int>,
    val totalObjects: Int,
)
