package com.lifepilot.data.repository

import com.lifepilot.data.database.dao.ConversationDao
import com.lifepilot.data.database.dao.DocumentDao
import com.lifepilot.data.database.dao.GoalDao
import com.lifepilot.data.database.dao.MetadataDao
import com.lifepilot.data.database.dao.ObjectDao
import com.lifepilot.data.database.dao.TaskDao
import com.lifepilot.domain.model.SearchEntityType
import com.lifepilot.domain.model.SearchResult
import com.lifepilot.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val objectDao: ObjectDao,
    private val metadataDao: MetadataDao,
    private val documentDao: DocumentDao,
    private val goalDao: GoalDao,
    private val taskDao: TaskDao,
    private val conversationDao: ConversationDao,
    private val preferenceManager: PreferenceManager,
) : SearchRepository {

    private val recentSearchesMap = mutableMapOf<String, MutableList<String>>()
    private val recentSearchesFlow = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    override suspend fun search(query: String, profileId: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        // Object title/description/type search
        val objectResults = objectDao.searchObjects(profileId, query).map { entity ->
            SearchResult(
                entityId = entity.objectId,
                entityType = SearchEntityType.OBJECT,
                title = entity.title,
                subtitle = entity.description,
                objectType = entity.objectType,
                domain = entity.domain,
                relevanceScore = computeScore(query, entity.title, entity.description),
            )
        }

        // Metadata value search — find objects by their metadata content
        val metadataMatches = metadataDao.searchMetadataValues(profileId, query)
        val metadataObjectIds = metadataMatches.map { it.objectId }.toSet()
        val existingObjectIds = objectResults.map { it.entityId }.toSet()

        // Load objects that matched via metadata but not already in object results
        val metadataOnlyIds = (metadataObjectIds - existingObjectIds).toList()
        val metadataOnlyEntities = if (metadataOnlyIds.isEmpty()) {
            emptyMap()
        } else {
            objectDao.getObjectsByIds(metadataOnlyIds).associateBy { it.objectId }
        }
        val additionalObjects = metadataOnlyIds.mapNotNull { objectId ->
            val entity = metadataOnlyEntities[objectId] ?: return@mapNotNull null
            val matchingField = metadataMatches.first { it.objectId == objectId }
            SearchResult(
                entityId = entity.objectId,
                entityType = SearchEntityType.OBJECT,
                title = entity.title,
                subtitle = "Matched: ${matchingField.fieldId.replace("_", " ")} = ${matchingField.value}",
                objectType = entity.objectType,
                domain = entity.domain,
                relevanceScore = 0.6f,
            )
        }

        // Document name/type search — surfaces the parent object
        val documentMatches = documentDao.searchDocuments(profileId, query)
        val docObjectIds = documentMatches.map { it.objectId }.toSet()
        val existingObjectIds2 = (objectResults.map { it.entityId } + additionalObjects.map { it.entityId }).toSet()

        val docOnlyIds = (docObjectIds - existingObjectIds2).toList()
        val docEntities = if (docOnlyIds.isEmpty()) {
            emptyMap()
        } else {
            objectDao.getObjectsByIds(docOnlyIds).associateBy { it.objectId }
        }
        val docObjects = docOnlyIds.mapNotNull { objectId ->
            val entity = docEntities[objectId] ?: return@mapNotNull null
            val matchingDoc = documentMatches.first { it.objectId == objectId }
            SearchResult(
                entityId = entity.objectId,
                entityType = SearchEntityType.OBJECT,
                title = entity.title,
                subtitle = "Document: ${matchingDoc.documentType.replace("_", " ")}",
                objectType = entity.objectType,
                domain = entity.domain,
                relevanceScore = 0.55f,
            )
        }

        // Boost scores for objects that match both title and metadata
        val boostedObjectResults = objectResults.map { result ->
            if (result.entityId in metadataObjectIds) {
                result.copy(relevanceScore = minOf(1.0f, result.relevanceScore + 0.1f))
            } else {
                result
            }
        }

        // Goal search
        val goalResults = goalDao.searchGoals(profileId, query).map { entity ->
            SearchResult(
                entityId = entity.goalId,
                entityType = SearchEntityType.GOAL,
                title = entity.title,
                subtitle = entity.description,
                objectType = null,
                domain = null,
                relevanceScore = computeScore(query, entity.title, entity.description),
            )
        }

        // Task search (pending/in-progress only to keep results actionable)
        val taskResults = taskDao.searchTasks(profileId, query)
            .filter { it.status in listOf("PENDING", "IN_PROGRESS") }
            .map { entity ->
                SearchResult(
                    entityId = entity.taskId,
                    entityType = SearchEntityType.TASK,
                    title = entity.title,
                    subtitle = entity.description,
                    objectType = null,
                    domain = null,
                    relevanceScore = computeScore(query, entity.title, entity.description) * 0.85f,
                )
            }

        // Conversation search (title + message content)
        val conversationResults = conversationDao.searchConversations(profileId, query)
            .map { entity ->
                SearchResult(
                    entityId = entity.conversationId,
                    entityType = SearchEntityType.CONVERSATION,
                    title = entity.title,
                    subtitle = "Past conversation",
                    objectType = null,
                    domain = null,
                    relevanceScore = computeScore(query, entity.title, null) * 0.8f,
                )
            }

        return (boostedObjectResults + additionalObjects + docObjects + goalResults + taskResults + conversationResults)
            .sortedByDescending { it.relevanceScore }
    }

    private fun computeScore(query: String, title: String, description: String?): Float {
        val q = query.lowercase()
        val t = title.lowercase()
        val d = description?.lowercase() ?: ""
        return when {
            t == q -> 1.0f
            t.startsWith(q) -> 0.9f
            t.contains(q) -> 0.7f
            d.contains(q) -> 0.5f
            else -> 0.3f
        }
    }

    override fun observeRecentSearches(profileId: String): Flow<List<String>> =
        recentSearchesFlow.map { it[profileId] ?: emptyList() }

    override suspend fun saveRecentSearch(query: String, profileId: String) {
        val list = recentSearchesMap.getOrPut(profileId) { mutableListOf() }
        list.remove(query)
        list.add(0, query)
        if (list.size > 20) list.removeLastOrNull()
        recentSearchesFlow.value = recentSearchesMap.toMap()
    }

    override suspend fun clearRecentSearches(profileId: String) {
        recentSearchesMap.remove(profileId)
        recentSearchesFlow.value = recentSearchesMap.toMap()
    }


}
