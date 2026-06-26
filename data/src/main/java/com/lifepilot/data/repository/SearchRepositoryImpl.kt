package com.lifepilot.data.repository

import com.lifepilot.data.database.dao.ObjectDao
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
    private val preferenceManager: PreferenceManager,
) : SearchRepository {

    private val recentSearchesMap = mutableMapOf<String, MutableList<String>>()
    private val recentSearchesFlow = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    override suspend fun search(query: String, profileId: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val objectResults = objectDao.searchObjects(profileId, query).map { entity ->
            SearchResult(
                resultId = entity.objectId,
                entityType = SearchEntityType.OBJECT,
                title = entity.title,
                subtitle = entity.description,
                objectType = entity.objectType,
                domain = entity.domain,
                score = computeScore(query, entity.title, entity.description),
            )
        }

        return objectResults.sortedByDescending { it.score }
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

    override suspend fun indexObject(objectId: String) {}

    override suspend fun removeFromIndex(objectId: String) {}
}
