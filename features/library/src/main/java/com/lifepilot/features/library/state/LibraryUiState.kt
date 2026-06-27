package com.lifepilot.features.library.state

import com.lifepilot.domain.model.LifeObject

enum class LibrarySortOrder(val label: String) {
    TITLE_ASC("A – Z"),
    TITLE_DESC("Z – A"),
    UPDATED_RECENT("Recent"),
    STATUS("Status"),
}

data class LibraryUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedDomain: String? = null,
    val sortOrder: LibrarySortOrder = LibrarySortOrder.UPDATED_RECENT,
    val domains: List<DomainItem> = emptyList(),
    val objects: List<LifeObject> = emptyList(),
    val error: String? = null,
)

data class DomainItem(
    val domain: String,
    val objectCount: Int,
    val displayName: String,
)
