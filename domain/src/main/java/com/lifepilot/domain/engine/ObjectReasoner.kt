package com.lifepilot.domain.engine

import com.lifepilot.domain.model.AiObjectContext
import com.lifepilot.domain.model.ObjectSnapshot

/**
 * Builds a canonical AI snapshot for a single Object.
 *
 * ObjectReasoner decides what metadata, tasks, and context to include in the snapshot.
 * The snapshot is what RetrievalEngine passes to PromptBuilder — the LLM never
 * needs to reconstruct Object state from raw fields.
 */
interface ObjectReasoner {

    /**
     * Build a snapshot for the given object, scoped to [profileId].
     * Returns null if the object does not exist.
     */
    suspend fun buildSnapshot(profileId: String, objectId: String): ObjectSnapshot?

    /**
     * Parse a structured [AiObjectContext] from its JSON representation stored in metadata.
     * Returns null if the metadata is absent or malformed.
     */
    fun parseAiContext(json: String): AiObjectContext?

    /**
     * Serialize an [AiObjectContext] to its JSON storage representation.
     */
    fun serializeAiContext(context: AiObjectContext): String
}
