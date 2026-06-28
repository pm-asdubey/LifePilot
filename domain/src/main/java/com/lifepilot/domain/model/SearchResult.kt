package com.lifepilot.domain.model

data class SearchResult(
    val entityId: String,
    val entityType: SearchEntityType,
    val title: String,
    val subtitle: String?,
    val objectType: String?,
    val domain: String?,
    val relevanceScore: Float,
)

enum class SearchEntityType {
    OBJECT,
    DOCUMENT,
    TASK,
    EVENT,
    GOAL,
    CONVERSATION,
}
