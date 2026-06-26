package com.lifepilot.domain.repository

import com.lifepilot.domain.model.SearchResult
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    suspend fun search(query: String, profileId: String): List<SearchResult>
    fun observeRecentSearches(profileId: String): Flow<List<String>>
    suspend fun saveRecentSearch(query: String, profileId: String)
    suspend fun clearRecentSearches(profileId: String)
    suspend fun indexObject(objectId: String)
    suspend fun removeFromIndex(objectId: String)
}
