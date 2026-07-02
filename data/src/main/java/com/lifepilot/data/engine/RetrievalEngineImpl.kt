package com.lifepilot.data.engine

import com.lifepilot.domain.engine.ObjectReasoner
import com.lifepilot.domain.engine.RetrievalEngine
import com.lifepilot.domain.model.DomainLifeState
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.RetrievalContext
import com.lifepilot.domain.repository.DomainRepository
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.repository.ReminderRepository
import com.lifepilot.domain.repository.TaskRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    private val domainRepository: DomainRepository,
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

        // Batch-load metadata once; allObjects Flow already batches this but we
        // need the raw map for scoring as well.
        val allMetadata = runCatching {
            metadataRepository.getMetadataForObjects(allObjects.map { it.objectId })
        }.getOrElse { emptyMap() }

        val allObjectIndex = allObjects.associate { it.objectId to (it.title to it.objectType) }
        val allObjectDomainIndex = allObjects.associate { it.objectId to it.domain }

        // Load domain life states — highest priority context for prompt construction.
        val domainLifeStates: Map<String, DomainLifeState> = runCatching {
            domainRepository.observeAllDomainLifeStates(resolvedProfileId)
                .catch { }
                .firstOrNull() ?: emptyList()
        }.getOrElse { emptyList() }
            .filter { it.currentSituation.isNotBlank() }
            .associateBy { it.domain }

        // Score once — keep (objectId, score) pairs so we don't re-score below.
        val topScored = scoreObjects(userQuery, allObjects, allMetadata).take(MAX_RELEVANT_OBJECTS)

        // Build ObjectSnapshots in parallel — each buildSnapshot() fires DB queries
        // independently, so async/await cuts latency from (N × queries) to ~(1 × queries).
        val relevantSnapshots = coroutineScope {
            topScored.map { (objectId, score) ->
                async {
                    runCatching {
                        objectReasoner.buildSnapshot(resolvedProfileId, objectId)
                            ?.copy(relevanceScore = score)
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull()
        }

        // Remaining context loaded concurrently with snapshot building above when
        // called from a coroutineScope — structured concurrency ensures cancellation.
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
            domainLifeStates = domainLifeStates,
            relevantSnapshots = relevantSnapshots,
            totalObjectCount = allObjects.size,
            allObjectIndex = allObjectIndex,
            allObjectDomainIndex = allObjectDomainIndex,
            allObjectMetadata = allMetadata,
            pendingTasks = pendingTasks,
            upcomingReminders = upcomingReminders,
        )
    }

    /**
     * Keyword-based relevance scoring.
     * Returns (objectId, score) pairs sorted descending by score, falling back to
     * the most recently updated objects when no tokens match.
     *
     * Future: replace this body with embedding-based cosine similarity
     * without changing any caller.
     */
    private fun scoreObjects(
        query: String,
        objects: List<LifeObject>,
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

            for (token in tokens) {
                if (titleLower.contains(token)) score += 3f
                if (typeLower.contains(token)) score += 2f
                if (domainLower.contains(token)) score += 1.5f
            }

            metadata[obj.objectId]?.take(10)?.forEach { entry ->
                val valueLower = entry.value.lowercase()
                val fieldLower = entry.fieldId.lowercase()
                for (token in tokens) {
                    if (valueLower.contains(token)) score += 0.5f
                    if (fieldLower.contains(token)) score += 0.3f
                }
            }

            obj.objectId to score
        }.filter { (_, score) -> score > 0f }
            .sortedByDescending { (_, score) -> score }

        return scored.ifEmpty { objects.take(MAX_RELEVANT_OBJECTS).map { it.objectId to 0f } }
    }
}
