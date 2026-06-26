package com.lifepilot.features.library.state

import com.lifepilot.domain.model.LifeObject

data class LibraryUiState(
    val isLoading: Boolean = true,
    val selectedDomain: String? = null,
    val domains: List<DomainItem> = emptyList(),
    val objects: List<LifeObject> = emptyList(),
    val error: String? = null,
)

data class DomainItem(
    val domain: String,
    val objectCount: Int,
    val displayName: String,
)
