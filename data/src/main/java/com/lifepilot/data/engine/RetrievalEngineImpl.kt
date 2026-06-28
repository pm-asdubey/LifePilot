package com.lifepilot.data.engine

import com.lifepilot.domain.engine.ObjectReasoner
import com.lifepilot.domain.engine.RetrievalEngine
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.RetrievalContext
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.repository.ReminderRepository
import com.lifepilot.domain.repository.TaskRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrievalEngineImpl @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val objectRepository: ObjectRepository,
    private val metadataRepository: MetadataRepository,
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val objectReasoner: ObjectReasoner,
) : RetrievalEngine {

    companion object {
        private const val MAX_RELEVANT_OBJECTS = 5
        private const val REMINDER_HORIZON_DAYS = 30L
    }

    override suspend fun retrieve(profileId: String, userQuery: String): RetrievalContext {
        val profile = profileRepository.observeActiveProfile()
            .catch { Timber.e(it, "RetrievalEngine: error observing profile") }
            .firstOrNull()

        val resolvedProfileId = profile?.profileId ?: profileId

        val allObjects = runCatching {
            objectRepository.observeObjectsByProfile(resolvedProfileId)
                .catch { }
                .firstOrNull() ?: emptyList()
        }.getOrElse { emptyList() }

        val allMetadata = runCatching {
            metadataRepository.getMetadataForObjects(allObjects.map { it.objectId })
        }.getOrElse { emptyMap() }

        // Build the full index used by parseAction() after the AI responds
        val allObjectIndex = allObjects.associate { it.objectId to (it.title to it.objectType) }

        // Score and select the most relevant objects for the prompt
        val scoredIds = scoreObjects(userQuery, allObjects, allMetadata)
            .take(MAX_RELEVANT_OBJECTS)
            .map { it.first }

        // Build ObjectSnapshots for selected objects only
        val relevantSnapshots = scoredIds.mapNotNull { objectId ->
            val rawScore = scoreObjects(userQuery, allObjects.filter { it.objectId == objectId }, allMetadata)
                .firstOrNull()?.second ?: 0f
            objectReasoner.buildSnapshot(resolvedProfileId, objectId)?.copy(relevanceScore = rawScore)
        }

        val pendingTasks = runCatching {
            taskRepository.observePendingTasks(resolvedProfileId)
                .catch { }
                .firstOrNull() ?: emptyList()
        }.getOrElse { emptyList() }

        val upcomingReminders = runCatching {
            reminderRepository.observeUpcomingReminders(
                Instant.now().plus(REMINDER_HORIZON_DAYS, ChronoUnit.DAYS)
            ).catch { }.firstOrNull() ?: emptyList()
        }.getOrElse { emptyList() }

        return RetrievalContext(
            profileId = resolvedProfileId,
            profileName = profile?.displayName,
            relevantSnapshots = relevantSnapshots,
            totalObjectCount = allObjects.size,
            allObjectIndex = allObjectIndex,
            allObjectMetadata = allMetadata,
            pendingTasks = pendingTasks,
            upcomingReminders = upcomingReminders,
        )
    }

    /**
     * Keyword-based relevance scoring.
     * Returns (objectId, score) pairs sorted descending by score, falling back to all objects
     * when no query tokens match anything.
     *
     * Future: swap this function body for embedding-based cosine similarity
     * without changing any caller.
     */
    private fun scoreObjects(
        query: String,
        objects: List<com.lifepilot.domain.model.LifeObject>,
        metadata: Map<String, List<MetadataEntry>>,
    ): List<Pair<String, Float>> {
        val tokens = query.lowercase()
            .split(" ", ",", ".", "?", "!")
            .filter { it.length > 2 }

        if (tokens.isEmpty()) {
            return objects.take(MAX_RELEVANT_OBJECTS).map { it.objectId to 0f }
        }

        val scored = objects.map { obj ->
            var score = 0f
            val titleLower = obj.title.lowercase()
            val typeLower = obj.objectType.lowercase()
            val domainLower = obj.domain.lowercase()

            tokens.forEach { token ->
                if (titleLower.contains(token)) score += 3f
                if (typeLower.contains(token)) score += 2f
                if (domainLower.contains(token)) score += 1.5f
            }

            metadata[obj.objectId]?.take(10)?.forEach { entry ->
                tokens.forEach { token ->
                    if (entry.value.lowercase().contains(token)) score += 0.5f
                    if (entry.fieldId.lowercase().contains(token)) score += 0.3f
                }
            }

            obj.objectId to score
        }.filter { (_, score) -> score > 0f }
            .sortedByDescending { (_, score) -> score }

        return scored.ifEmpty { objects.take(MAX_RELEVANT_OBJECTS).map { it.objectId to 0f } }
    }
}
