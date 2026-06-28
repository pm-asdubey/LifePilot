package com.lifepilot.domain.model

/**
 * Everything the PromptBuilder needs to assemble a system prompt.
 *
 * Produced by RetrievalEngine. Contains:
 *  - A scored, bounded list of relevant ObjectSnapshots (max 5)
 *  - A full index of all objects for parseAction() resolution after the response
 *  - Planner context (tasks, reminders)
 *
 * PromptBuilder receives this and nothing else — it never queries repositories.
 */
data class RetrievalContext(
    val profileId: String,
    val profileName: String?,
    val relevantSnapshots: List<ObjectSnapshot>,
    val totalObjectCount: Int,
    val allObjectIndex: Map<String, Pair<String, String>>,
    val allObjectMetadata: Map<String, List<MetadataEntry>>,
    val pendingTasks: List<Task>,
    val upcomingReminders: List<Reminder>,
)
