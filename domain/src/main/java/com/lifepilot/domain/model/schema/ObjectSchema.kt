package com.lifepilot.domain.model.schema

import kotlinx.serialization.Serializable

@Serializable
data class ObjectSchema(
    val objectType: String,
    val domain: String,
    val displayName: String,
    val icon: String,
    val description: String,
    val schemaVersion: Int,
    val fields: List<MetadataFieldDefinition>,
    val lifecycle: LifecycleDefinition,
    val reminderRules: List<ReminderRule>,
    val searchConfig: SearchConfig,
    val aiConfig: AiExtractionConfig,
)

@Serializable
data class MetadataFieldDefinition(
    val fieldId: String,
    val displayName: String,
    val fieldType: String,
    val required: Boolean,
    val editable: Boolean,
    val searchable: Boolean,
    val aiExtractable: Boolean,
    val validationRules: List<ValidationRule>,
    val enumValues: List<String> = emptyList(),
    val hint: String? = null,
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
    val triggerField: String,
    val offsetDays: Int,
    val priority: String,
    val title: String,
    val messageTemplate: String,
)

@Serializable
data class SearchConfig(
    val primaryFields: List<String>,
    val fullTextFields: List<String>,
    val filterableFields: List<String>,
)

@Serializable
data class AiExtractionConfig(
    val extractableFields: List<String>,
    val classificationHints: List<String>,
)

typealias ObjectDefinition = ObjectSchema
