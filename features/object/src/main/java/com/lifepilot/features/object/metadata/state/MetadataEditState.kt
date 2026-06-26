package com.lifepilot.features.object.metadata.state

import com.lifepilot.domain.model.schema.MetadataFieldDefinition

data class MetadataEditState(
    val isLoading: Boolean = false,
    val objectId: String = "",
    val objectType: String = "",
    val fields: List<MetadataFieldDefinition> = emptyList(),
    val values: Map<String, String> = emptyMap(),
    val errors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)
