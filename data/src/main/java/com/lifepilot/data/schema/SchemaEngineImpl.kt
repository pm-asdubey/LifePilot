package com.lifepilot.data.schema

import android.content.Context
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.engine.ValidationResult
import com.lifepilot.domain.model.schema.ObjectSchema
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchemaEngineImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SchemaEngine {

    private val _registeredSchemas = MutableStateFlow<Map<String, ObjectSchema>>(emptyMap())
    override val registeredSchemas: StateFlow<Map<String, ObjectSchema>> = _registeredSchemas

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun getSchema(objectType: String): ObjectSchema? =
        _registeredSchemas.value[objectType]

    override fun getAllObjectTypes(): List<String> =
        _registeredSchemas.value.keys.sorted()

    override fun getObjectTypesByDomain(domain: String): List<String> =
        _registeredSchemas.value.values
            .filter { it.domain == domain }
            .map { it.objectType }
            .sorted()

    override fun getAllDomains(): List<String> =
        _registeredSchemas.value.values
            .map { it.domain }
            .distinct()
            .sorted()

    override suspend fun loadSchemas() {
        val schemas = mutableMapOf<String, ObjectSchema>()
        try {
            val assetManager = context.assets
            val schemaFiles = assetManager.list("schemas") ?: emptyArray()
            for (fileName in schemaFiles) {
                if (!fileName.endsWith(".json")) continue
                try {
                    val content = assetManager.open("schemas/$fileName").bufferedReader().readText()
                    val schema = json.decodeFromString<ObjectSchema>(content)
                    schemas[schema.objectType] = schema
                    Timber.d("Loaded schema: ${schema.objectType}")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load schema from $fileName")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load schemas from assets")
        }
        _registeredSchemas.value = schemas
        Timber.i("Schema engine loaded ${schemas.size} schemas")
    }

    override fun validateMetadataValue(
        objectType: String,
        fieldId: String,
        value: String,
    ): ValidationResult {
        val schema = getSchema(objectType)
            ?: return ValidationResult(isValid = false, errors = listOf("Unknown object type: $objectType"))
        val field = schema.fields.find { it.fieldId == fieldId }
            ?: return ValidationResult(isValid = false, errors = listOf("Unknown field: $fieldId"))

        val errors = mutableListOf<String>()
        for (rule in field.validationRules) {
            when (rule.ruleType) {
                "REQUIRED" -> if (value.isBlank()) errors.add(rule.message)
                "MAX_LENGTH" -> {
                    val maxLength = rule.parameter?.toIntOrNull() ?: Int.MAX_VALUE
                    if (value.length > maxLength) errors.add(rule.message)
                }
                "MIN_LENGTH" -> {
                    val minLength = rule.parameter?.toIntOrNull() ?: 0
                    if (value.length < minLength) errors.add(rule.message)
                }
                "REGEX" -> {
                    val pattern = rule.parameter ?: continue
                    runCatching {
                        if (!Regex(pattern).matches(value)) errors.add(rule.message)
                    }
                }
                "DATE_FORMAT" -> {
                    val fmt = rule.parameter ?: "yyyy-MM-dd"
                    runCatching {
                        java.time.LocalDate.parse(value, java.time.format.DateTimeFormatter.ofPattern(fmt))
                    }.onFailure { errors.add(rule.message) }
                }
                "ENUM" -> {
                    if (field.enumValues.isNotEmpty() && value !in field.enumValues) {
                        errors.add(rule.message)
                    }
                }
            }
        }
        if (field.fieldType == "ENUM" && field.enumValues.isNotEmpty() && value.isNotBlank() && value !in field.enumValues) {
            errors.add("Value must be one of: ${field.enumValues.joinToString(", ")}")
        }
        return ValidationResult(isValid = errors.isEmpty(), errors = errors)
    }
}
