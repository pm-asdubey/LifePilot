package com.lifepilot.features.objectdetail.create.state

data class CreateObjectState(
    val isLoading: Boolean = false,
    val availableTypes: List<ObjectTypeItem> = emptyList(),
    val selectedType: String? = null,
    val title: String = "",
    val description: String = "",
    val step: CreateObjectStep = CreateObjectStep.SELECT_TYPE,
    val error: String? = null,
    val created: Boolean = false,
    val createdObjectId: String? = null,
)

data class ObjectTypeItem(
    val objectType: String,
    val displayName: String,
    val domain: String,
    val icon: String,
    val description: String,
)

enum class CreateObjectStep {
    SELECT_TYPE,
    FILL_DETAILS,
}
