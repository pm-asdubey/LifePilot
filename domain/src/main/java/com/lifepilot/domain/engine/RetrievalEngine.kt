package com.lifepilot.domain.engine

import com.lifepilot.domain.model.RetrievalContext

/**
 * Assembles the context passed to PromptBuilder before every AI request.
 *
 * Responsibilities:
 *  - Intent detection from the user query
 *  - Scoring and selecting relevant Objects (max 5)
 *  - Building ObjectSnapshots via ObjectReasoner
 *  - Retrieving relevant Planner context (tasks, reminders)
 *  - Populating the full object index for post-response parseAction() resolution
 *
 * PromptBuilder must never decide what to retrieve.
 * RetrievalEngine must never decide how to format context.
 *
 * Future retrieval strategies (embeddings, vector search, semantic ranking)
 * can be swapped in here without changing PromptBuilder or HomeViewModel.
 */
interface RetrievalEngine {

    /**
     * Build a [RetrievalContext] for a given user query.
     * Pass an empty [userQuery] to retrieve general context (e.g. daily brief).
     */
    suspend fun retrieve(profileId: String, userQuery: String): RetrievalContext
}
