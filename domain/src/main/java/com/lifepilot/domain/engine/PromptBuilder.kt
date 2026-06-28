package com.lifepilot.domain.engine

import com.lifepilot.domain.model.RetrievalContext

/**
 * Assembles the final system prompt from already-retrieved context.
 *
 * Responsibilities:
 *  - Format ObjectSnapshots into prompt text
 *  - Include action-type examples (METADATA_UPDATE, GOAL_PROPOSAL, etc.)
 *  - Include Planner context
 *  - Include today's date and profile information
 *
 * PromptBuilder must never:
 *  - Query repositories
 *  - Score or select objects
 *  - Make decisions about what to retrieve
 *  - Modify Life State
 *
 * Input: RetrievalContext + userQuery (for date/context hints)
 * Output: System prompt string for the AI provider
 */
interface PromptBuilder {
    fun build(context: RetrievalContext, userQuery: String = ""): String
}
