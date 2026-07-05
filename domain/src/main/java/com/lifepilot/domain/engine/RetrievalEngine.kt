package com.lifepilot.domain.engine

import com.lifepilot.domain.model.LifeStateSummary
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
     * Lightweight structural summary of the life state — no content, just domain names
     * and object counts. Used by [RetrievalPlanner] to decide what's worth fetching.
     */
    suspend fun getSummary(profileId: String): LifeStateSummary

    /**
     * Build a [RetrievalContext] for a given user query.
     *
     * @param relevantDomains When provided (from [RetrievalPlanner]), only life states
     *   for these domains are loaded. Pass null to load all (fallback behaviour).
     */
    suspend fun retrieve(
        profileId: String,
        userQuery: String,
        relevantDomains: List<String>? = null,
    ): RetrievalContext
}
