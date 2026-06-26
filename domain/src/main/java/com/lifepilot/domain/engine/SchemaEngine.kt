package com.lifepilot.domain.engine

import com.lifepilot.domain.model.schema.ObjectDefinition
import com.lifepilot.domain.model.schema.ObjectSchema
import kotlinx.coroutines.flow.StateFlow

interface SchemaEngine {
    val registeredSchemas: StateFlow<Map<String, ObjectSchema>>
    fun getSchema(objectType: String): ObjectSchema?
    fun getAllObjectTypes(): List<String>
    fun getObjectTypesByDomain(domain: String): List<String>
    fun getAllDomains(): List<String>
    suspend fun loadSchemas()
    fun validateMetadataValue(
        objectType: String,
        fieldId: String,
        value: String,
    ): ValidationResult
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
)
