package com.lifepilot.domain.engine

/**
 * Evaluates whether a conversation event has changed domain-level understanding
 * and, if so, updates the relevant DomainLifeState records.
 *
 * This engine runs in the background — it never blocks the conversation UI.
 * It determines which domains are affected and calls the AI with a focused prompt
 * to update only the sections that changed.
 */
interface DomainLifeStateEngine {

    /**
     * @param profileId      Active user profile.
     * @param userMessage    What the user said (may imply life changes).
     * @param aiResponse     What the AI replied (may reference objects/domains).
     * @param affectedDomains Domains that were active in the retrieval context.
     *                       The engine evaluates each and updates those that changed.
     */
    suspend fun evaluateAndUpdate(
        profileId: String,
        userMessage: String,
        aiResponse: String,
        affectedDomains: List<String>,
    )
}
