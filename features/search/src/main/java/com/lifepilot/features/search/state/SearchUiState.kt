package com.lifepilot.features.search.state

import com.lifepilot.domain.model.SearchResult

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val error: String? = null,
)
