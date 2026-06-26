package com.lifepilot.features.object.verification.state

data class MetadataVerificationState(
    val isLoading: Boolean = false,
    val objectId: String = "",
    val objectTitle: String = "",
    val objectType: String = "",
    val suggestions: List<FieldSuggestion> = emptyList(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

data class FieldSuggestion(
    val fieldId: String,
    val label: String,
    val suggestedValue: String,
    val editedValue: String,
    val confidence: Float,
    val isAccepted: Boolean = true,
)
