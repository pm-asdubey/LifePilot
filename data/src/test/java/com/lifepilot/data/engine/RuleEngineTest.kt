package com.lifepilot.data.engine

import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.Reminder
import com.lifepilot.domain.model.ReminderPriority
import com.lifepilot.domain.model.ReminderStatus
import com.lifepilot.domain.model.schema.AiExtractionConfig
import com.lifepilot.domain.model.schema.LifecycleDefinition
import com.lifepilot.domain.model.schema.ObjectSchema
import com.lifepilot.domain.model.schema.ReminderRule
import com.lifepilot.domain.model.schema.SearchConfig
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ReminderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RuleEngineTest {

    private val objectRepository: ObjectRepository = mockk()
    private val metadataRepository: MetadataRepository = mockk()
    private val reminderRepository: ReminderRepository = mockk()
    private val schemaEngine: SchemaEngine = mockk()

    private lateinit var ruleEngine: RuleEngineImpl

    private val testObjectId = "obj-1"
    private val testObject = LifeObject(
        objectId = testObjectId,
        profileId = "profile-1",
        objectType = "passport",
        domain = "Identity",
        title = "My Passport",
        description = null,
        status = ObjectStatus.ACTIVE,
        metadata = emptyList(),
        archived = false,
        deleted = false,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    private val futureDate = LocalDate.now().plusDays(90)
    private val futureDateStr = futureDate.toString()

    private val testSchema = ObjectSchema(
        objectType = "passport",
        domain = "Identity",
        displayName = "Passport",
        icon = "badge",
        description = "Travel document",
        schemaVersion = 1,
        fields = emptyList(),
        lifecycle = LifecycleDefinition(emptyList(), emptyList(), "active"),
        reminderRules = listOf(
            ReminderRule(
                ruleId = "passport_expiry",
                title = "Passport Expiry",
                triggerField = "expiry_date",
                offsetDays = -60,
                priority = "HIGH",
                messageTemplate = "Passport expires soon",
            )
        ),
        searchConfig = SearchConfig(emptyList(), emptyList(), emptyList()),
        aiConfig = AiExtractionConfig(emptyList(), emptyList()),
    )

    @Before
    fun setUp() {
        ruleEngine = RuleEngineImpl(
            objectRepository = objectRepository,
            metadataRepository = metadataRepository,
            reminderRepository = reminderRepository,
            schemaEngine = schemaEngine,
        )
    }

    @Test
    fun `evaluateRemindersForObject creates reminder when date is in future and no existing reminder`() = runTest {
        coEvery { objectRepository.getObjectById(testObjectId) } returns testObject
        coEvery { schemaEngine.getSchema("passport") } returns testSchema
        coEvery { metadataRepository.getMetadataByObject(testObjectId) } returns listOf(
            MetadataEntry(
                metadataId = "m1",
                objectId = testObjectId,
                fieldId = "expiry_date",
                value = futureDateStr,
                version = 1,
                updatedAt = Instant.now(),
            )
        )
        coEvery { reminderRepository.getRemindersForObject(testObjectId) } returns emptyList()
        val capturedReminder = slot<Reminder>()
        coEvery { reminderRepository.createReminder(capture(capturedReminder)) } returns mockk()

        ruleEngine.evaluateRemindersForObject(testObjectId)

        coVerify(exactly = 1) { reminderRepository.createReminder(any()) }
        assertEquals("passport_expiry", capturedReminder.captured.reminderType)
        assertEquals(ReminderPriority.HIGH, capturedReminder.captured.priority)
        assertEquals(ReminderStatus.SCHEDULED, capturedReminder.captured.status)

        val expectedTriggerDate = futureDate.plusDays(-60).atStartOfDay(ZoneId.systemDefault()).toInstant()
        assertEquals(expectedTriggerDate, capturedReminder.captured.triggerDate)
    }

    @Test
    fun `evaluateRemindersForObject does not create duplicate reminder`() = runTest {
        coEvery { objectRepository.getObjectById(testObjectId) } returns testObject
        coEvery { schemaEngine.getSchema("passport") } returns testSchema
        coEvery { metadataRepository.getMetadataByObject(testObjectId) } returns listOf(
            MetadataEntry(
                metadataId = "m1",
                objectId = testObjectId,
                fieldId = "expiry_date",
                value = futureDateStr,
                version = 1,
                updatedAt = Instant.now(),
            )
        )
        coEvery { reminderRepository.getRemindersForObject(testObjectId) } returns listOf(
            Reminder(
                reminderId = "r1",
                objectId = testObjectId,
                reminderType = "passport_expiry",
                triggerDate = Instant.now(),
                priority = ReminderPriority.HIGH,
                status = ReminderStatus.SCHEDULED,
                title = "Passport Expiry",
                message = null,
            )
        )

        ruleEngine.evaluateRemindersForObject(testObjectId)

        coVerify(exactly = 0) { reminderRepository.createReminder(any()) }
    }

    @Test
    fun `evaluateRemindersForObject skips when metadata field missing`() = runTest {
        coEvery { objectRepository.getObjectById(testObjectId) } returns testObject
        coEvery { schemaEngine.getSchema("passport") } returns testSchema
        coEvery { metadataRepository.getMetadataByObject(testObjectId) } returns emptyList()

        ruleEngine.evaluateRemindersForObject(testObjectId)

        coVerify(exactly = 0) { reminderRepository.createReminder(any()) }
    }

    @Test
    fun `evaluateRemindersForObject skips when trigger date is past`() = runTest {
        val pastDate = LocalDate.now().minusDays(90).toString()
        coEvery { objectRepository.getObjectById(testObjectId) } returns testObject
        coEvery { schemaEngine.getSchema("passport") } returns testSchema
        coEvery { metadataRepository.getMetadataByObject(testObjectId) } returns listOf(
            MetadataEntry(
                metadataId = "m1",
                objectId = testObjectId,
                fieldId = "expiry_date",
                value = pastDate,
                version = 1,
                updatedAt = Instant.now(),
            )
        )
        coEvery { reminderRepository.getRemindersForObject(testObjectId) } returns emptyList()

        ruleEngine.evaluateRemindersForObject(testObjectId)

        coVerify(exactly = 0) { reminderRepository.createReminder(any()) }
    }
}
