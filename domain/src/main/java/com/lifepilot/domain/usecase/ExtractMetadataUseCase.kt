package com.lifepilot.domain.usecase

import com.lifepilot.domain.ai.AiCompletionResult
import com.lifepilot.domain.ai.AiProvider
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.model.MetadataFieldType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

data class ExtractedField(
    val fieldId: String,
    val label: String,
    val suggestedValue: String,
    val confidence: Float,
    val fieldType: MetadataFieldType = MetadataFieldType.TEXT,
)

data class ExtractionResult(
    val objectType: String,
    val fields: List<ExtractedField>,
    val rawResponse: String,
)

class ExtractMetadataUseCase @Inject constructor(
    private val schemaEngine: SchemaEngine,
    private val aiProvider: AiProvider,
) {
    suspend operator fun invoke(
        objectType: String,
        ocrText: String,
    ): Result<ExtractionResult> = runCatching {
        val schema = schemaEngine.getSchema(objectType)
            ?: error("Unknown object type: $objectType")

        val extractableTypes = setOf(
            MetadataFieldType.TEXT.name,
            MetadataFieldType.DATE.name,
            MetadataFieldType.NUMBER.name,
            MetadataFieldType.ENUM.name,
        )
        val extractableFields = schema.fields.filter { field ->
            field.fieldType.uppercase() in extractableTypes && field.aiExtractable
        }

        val fieldDescriptions = extractableFields.joinToString("\n") { field ->
            val enumHint = if (field.enumValues.isNotEmpty()) {
                " (one of: ${field.enumValues.joinToString(", ")})"
            } else ""
            "- ${field.fieldId}: ${field.displayName.ifBlank { field.fieldId }}${enumHint}"
        }

        val systemPrompt = """
            You are a document analysis assistant. Extract structured metadata from OCR text.
            Return ONLY a JSON object with field IDs as keys and extracted values as strings.
            If a field cannot be found, omit it. Do not invent values.
            Respond with valid JSON only, no explanation.
        """.trimIndent()

        val userMessage = """
            Extract these fields from the document text:
            $fieldDescriptions

            Document text:
            $ocrText

            Return JSON like: {"field_id": "value", ...}
        """.trimIndent()

        val result = aiProvider.complete(
            systemPrompt = systemPrompt,
            userMessage = userMessage,
            conversationHistory = emptyList(),
        )

        when (result) {
            is AiCompletionResult.Success -> {
                val extracted = parseJsonFields(result.content)
                val fields = extractableFields.mapNotNull { field ->
                    val value = extracted[field.fieldId] ?: return@mapNotNull null
                    val domainFieldType = runCatching {
                        MetadataFieldType.valueOf(field.fieldType.uppercase())
                    }.getOrDefault(MetadataFieldType.TEXT)
                    ExtractedField(
                        fieldId = field.fieldId,
                        label = field.displayName.ifBlank { field.fieldId },
                        suggestedValue = value,
                        confidence = 0.85f,
                        fieldType = domainFieldType,
                    )
                }
                ExtractionResult(
                    objectType = objectType,
                    fields = fields,
                    rawResponse = result.content,
                )
            }
            is AiCompletionResult.Error -> error("AI extraction failed: ${result.message}")
            is AiCompletionResult.Unavailable -> error("AI provider not configured")
        }
    }

    private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun parseJsonFields(json: String): Map<String, String> {
        val cleaned = json.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        return try {
            lenientJson.parseToJsonElement(cleaned).jsonObject
                .mapValues { (_, v) -> runCatching { v.jsonPrimitive.content }.getOrElse { v.toString().trim('"') } }
        } catch (_: Exception) {
            parseJsonFieldsFallback(cleaned)
        }
    }

    private fun parseJsonFieldsFallback(cleaned: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val regex = """"([^"]+)"\s*:\s*"([^"]*)"|\s*"([^"]+)"\s*:\s*(\d+(?:\.\d+)?)""".toRegex()
        for (match in regex.findAll(cleaned)) {
            val key = match.groupValues[1].ifEmpty { match.groupValues[3] }
            val value = match.groupValues[2].ifEmpty { match.groupValues[4] }
            if (key.isNotEmpty() && value.isNotEmpty()) {
                result[key] = value
            }
        }
        return result
    }
}
