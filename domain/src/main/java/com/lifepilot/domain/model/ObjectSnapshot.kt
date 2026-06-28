package com.lifepilot.domain.model

/**
 * Canonical AI snapshot of a single Object.
 *
 * Built by ObjectReasoner. Contains everything needed to represent this
 * Object in a prompt without the LLM needing to reconstruct state each time.
 *
 * ObjectReasoner is responsible for deciding what to include.
 * PromptBuilder is responsible for formatting this into text.
 */
data class ObjectSnapshot(
    val objectId: String,
    val title: String,
    val objectType: String,
    val domain: String,
    val status: ObjectStatus,
    val metadata: List<MetadataEntry>,
    val aiContext: AiObjectContext?,
    val pendingTaskCount: Int,
    val documentCount: Int,
    val relevanceScore: Float = 0f,
)
