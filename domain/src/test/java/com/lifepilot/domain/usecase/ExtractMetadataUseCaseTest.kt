package com.lifepilot.domain.usecase

import com.lifepilot.domain.ai.AiCompletionResult
import com.lifepilot.domain.ai.AiProvider
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.model.schema.AiExtractionConfig
import com.lifepilot.domain.model.schema.LifecycleDefinition
import com.lifepilot.domain.model.schema.LifecycleState
import com.lifepilot.domain.model.schema.MetadataFieldDefinition
import com.lifepilot.domain.model.schema.ObjectSchema
import com.lifepilot.domain.model.schema.SearchConfig
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExtractMetadataUseCaseTest {

    private val schemaEngine: SchemaEngine = mockk()
    private val aiProvider: AiProvider = mockk()

    private lateinit var useCase: ExtractMetadataUseCase

    private val passportSchema = ObjectSchema(
        objectType = "passport",
        domain = "Identity",
        displayName = "Passport",
        fields = listOf(
            MetadataFieldDefinition(
                fieldId = "passport_number",
                displayName = "Passport Number",
                fieldType = "TEXT",
                required = true,
                aiExtractable = true,
            ),
            MetadataFieldDefinition(
                fieldId = "expiry_date",
                displayName = "Expiry Date",
                fieldType = "DATE",
                required = true,
                aiExtractable = true,
            ),
        ),
        lifecycle = LifecycleDefinition(
            states = listOf(LifecycleState("ACTIVE", "Active")),
            transitions = emptyList(),
            initialState = "ACTIVE",
        ),
        reminderRules = emptyList(),
        searchConfig = SearchConfig(
            primaryFields = emptyList(),
            fullTextFields = emptyList(),
            filterableFields = emptyList(),
        ),
    )

    @Before
    fun setUp() {
        useCase = ExtractMetadataUseCase(schemaEngine, aiProvider)
    }

    @Test
    fun `returns extracted fields from AI response`() = runTest {
        coEvery { schemaEngine.getSchema("passport") } returns passportSchema
        coEvery { aiProvider.complete(any(), any(), any()) } returns AiCompletionResult.Success(
            content = """{"passport_number": "AB123456", "expiry_date": "2029-01-15"}""",
        )

        val result = useCase("passport", "Sample OCR text with passport info")

        assertTrue(result.isSuccess)
        val extraction = result.getOrThrow()
        assertEquals(2, extraction.fields.size)
        val numberField = extraction.fields.find { it.fieldId == "passport_number" }!!
        assertEquals("AB123456", numberField.suggestedValue)
        assertEquals("Passport Number", numberField.label)
    }

    @Test
    fun `omits fields not found in AI response`() = runTest {
        coEvery { schemaEngine.getSchema("passport") } returns passportSchema
        coEvery { aiProvider.complete(any(), any(), any()) } returns AiCompletionResult.Success(
            content = """{"passport_number": "XY987654"}""",
        )

        val result = useCase("passport", "Partial OCR text")

        assertTrue(result.isSuccess)
        val extraction = result.getOrThrow()
        assertEquals(1, extraction.fields.size)
        assertEquals("passport_number", extraction.fields[0].fieldId)
    }

    @Test
    fun `fails when schema not found`() = runTest {
        coEvery { schemaEngine.getSchema("unknown_type") } returns null

        val result = useCase("unknown_type", "Any text")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Unknown object type") == true)
    }

    @Test
    fun `fails when AI returns error`() = runTest {
        coEvery { schemaEngine.getSchema("passport") } returns passportSchema
        coEvery { aiProvider.complete(any(), any(), any()) } returns AiCompletionResult.Error(
            message = "Rate limit exceeded",
        )

        val result = useCase("passport", "Some OCR text")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("AI extraction failed") == true)
    }

    @Test
    fun `fails when AI is unavailable`() = runTest {
        coEvery { schemaEngine.getSchema("passport") } returns passportSchema
        coEvery { aiProvider.complete(any(), any(), any()) } returns AiCompletionResult.Unavailable

        val result = useCase("passport", "Some OCR text")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not configured") == true)
    }

    @Test
    fun `handles AI response wrapped in code block`() = runTest {
        coEvery { schemaEngine.getSchema("passport") } returns passportSchema
        coEvery { aiProvider.complete(any(), any(), any()) } returns AiCompletionResult.Success(
            content = "```json\n{\"passport_number\": \"CD567890\"}\n```",
        )

        val result = useCase("passport", "OCR text")

        assertTrue(result.isSuccess)
        val extraction = result.getOrThrow()
        assertEquals(1, extraction.fields.size)
        assertEquals("CD567890", extraction.fields[0].suggestedValue)
    }

    @Test
    fun `handles AI response with numeric values`() = runTest {
        val schemaWithNumber = passportSchema.copy(
            fields = passportSchema.fields + MetadataFieldDefinition(
                fieldId = "page_count",
                displayName = "Page Count",
                fieldType = "NUMBER",
                aiExtractable = true,
            )
        )
        coEvery { schemaEngine.getSchema("passport") } returns schemaWithNumber
        coEvery { aiProvider.complete(any(), any(), any()) } returns AiCompletionResult.Success(
            content = """{"passport_number": "GH445566", "page_count": 32}""",
        )

        val result = useCase("passport", "OCR text")

        assertTrue(result.isSuccess)
        val pageCount = result.getOrThrow().fields.find { it.fieldId == "page_count" }
        assertEquals("32", pageCount?.suggestedValue)
    }

    @Test
    fun `skips fields with aiExtractable=false`() = runTest {
        val schemaWithNonExtractable = passportSchema.copy(
            fields = passportSchema.fields + MetadataFieldDefinition(
                fieldId = "internal_note",
                displayName = "Internal Note",
                fieldType = "TEXT",
                aiExtractable = false,
            )
        )
        coEvery { schemaEngine.getSchema("passport") } returns schemaWithNonExtractable
        coEvery { aiProvider.complete(any(), any(), any()) } returns AiCompletionResult.Success(
            content = """{"passport_number": "EF111222", "internal_note": "ignored"}""",
        )

        val result = useCase("passport", "OCR text")

        assertTrue(result.isSuccess)
        val extraction = result.getOrThrow()
        // internal_note is not extractable so even if AI returns it, it won't be in results
        // because extractableFields filter excludes it
        assertTrue(extraction.fields.none { it.fieldId == "internal_note" })
    }
}
