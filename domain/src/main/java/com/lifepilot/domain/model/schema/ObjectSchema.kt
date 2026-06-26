package com.lifepilot.domain.model.schema

import kotlinx.serialization.Serializable

@Serializable
data class ObjectSchema(
    val objectType: String,
    val domain: String,
    val displayName: String,
    val icon: String = "FolderOpen",
    val description: String = "",
    val schemaVersion: Int = 1,
    val fields: List<MetadataFieldDefinition> = emptyList(),
    val lifecycle: LifecycleDefinition = LifecycleDefinition(
        states = listOf(LifecycleState("ACTIVE", "Active")),
        transitions = emptyList(),
        initialState = "ACTIVE",
    ),
    val reminderRules: List<ReminderRule> = emptyList(),
    val searchConfig: SearchConfig = SearchConfig(
        primaryFields = emptyList(),
        fullTextFields = emptyList(),
        filterableFields = emptyList(),
    ),
    val aiConfig: AiExtractionConfig = AiExtractionConfig(),
)

@Serializable
data class MetadataFieldDefinition(
    val fieldId: String,
    val displayName: String = "",
    val fieldType: String,
    val required: Boolean = false,
    val editable: Boolean = true,
    val searchable: Boolean = true,
    val aiExtractable: Boolean = true,
    val validationRules: List<ValidationRule> = emptyList(),
    val enumValues: List<String> = emptyList(),
    val hint: String? = null,
    val placeholder: String? = null,
)

@Serializable
data class ValidationRule(
    val ruleType: String,
    val parameter: String? = null,
    val message: String,
)

@Serializable
data class LifecycleDefinition(
    val states: List<LifecycleState>,
    val transitions: List<LifecycleTransition>,
    val initialState: String,
)

@Serializable
data class LifecycleState(
    val stateId: String,
    val displayName: String,
    val isFinal: Boolean = false,
)

@Serializable
data class LifecycleTransition(
    val fromState: String,
    val toState: String,
    val trigger: String,
    val label: String,
)

@Serializable
data class ReminderRule(
    val ruleId: String,
    val displayName: String = "",
    val triggerField: String,
    val offsetDays: Int = 0,
    val priority: String = "MEDIUM",
    val title: String = "",
    val messageTemplate: String = "",
) {
    val effectiveTitle: String get() = title.ifBlank { displayName }
}

@Serializable
data class SearchConfig(
    val primaryFields: List<String>,
    val fullTextFields: List<String>,
    val filterableFields: List<String>,
)

@Serializable
data class AiExtractionConfig(
    val extractableFields: List<String> = emptyList(),
    val classificationHints: List<String> = emptyList(),
)

typealias ObjectDefinition = ObjectSchema
