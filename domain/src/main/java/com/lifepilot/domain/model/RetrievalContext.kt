package com.lifepilot.domain.model

/**
 * Everything the PromptBuilder needs to assemble a system prompt.
 *
 * Produced by RetrievalEngine. Contains:
 *  - Domain Life States: highest-priority context — accumulated understanding per domain.
 *    Objects own facts; Domains own understanding.
 *  - A scored, bounded list of relevant ObjectSnapshots (max 5) for detailed fact retrieval.
 *  - A full index of all objects for parseAction() resolution after the response.
 *  - Planner context (tasks, reminders).
 *
 * PromptBuilder receives this and nothing else — it never queries repositories.
 */
data class RetrievalContext(
    val profileId: String,
    val profileName: String?,
    val domainLifeStates: Map<String, DomainLifeState>,
    val relevantSnapshots: List<ObjectSnapshot>,
    val totalObjectCount: Int,
    /** objectId → (title, objectType) — for action parsing */
    val allObjectIndex: Map<String, Pair<String, String>>,
    /** objectId → domain — for post-approval domain life state targeting */
    val allObjectDomainIndex: Map<String, String>,
    val allObjectMetadata: Map<String, List<MetadataEntry>>,
    val pendingTasks: List<Task>,
    val upcomingReminders: List<Reminder>,
    /** Document attached to the current conversation (e.g. scanned from chat).
     *  The AI receives only [AttachedDocumentContext.extractedFields], never raw OCR. */
    val attachedDocumentContext: AttachedDocumentContext? = null,
    /** Active projects — life initiatives that club Objects, Tasks, Goals, and Documents. */
    val activeProjects: List<Project> = emptyList(),
)
