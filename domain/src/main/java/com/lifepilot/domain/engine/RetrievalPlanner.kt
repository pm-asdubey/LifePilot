package com.lifepilot.domain.engine

import com.lifepilot.domain.model.LifeStateSummary

/**
 * Determines which life domains are relevant to a user query before the main retrieval.
 *
 * Sends a lightweight AI call with only domain names + counts, avoiding full context bloat.
 * The returned list is used by [RetrievalEngine] to load life states selectively.
 *
 * Falls back to all available domains on failure so the main call always proceeds.
 */
interface RetrievalPlanner {

    /**
     * @param userQuery    Raw user message.
     * @param documentType Object type of any attached document (e.g. "PASSPORT"), or null.
     * @param summary      Lightweight structural summary of the life state.
     * @return Domain names to load in full. Empty list means fallback to all domains.
     */
    suspend fun planDomains(
        userQuery: String,
        documentType: String?,
        summary: LifeStateSummary,
    ): List<String>
}
