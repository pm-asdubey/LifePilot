package com.lifepilot.data.engine

import com.lifepilot.data.task.TaskGenerator
import com.lifepilot.domain.engine.RuleEngine
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.MetadataFieldType
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.model.ObjectStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class LifeStateEngineTest {

    private val objectRepository = mockk<com.lifepilot.domain.repository.ObjectRepository>(relaxed = true)
    private val metadataRepository = mockk<com.lifepilot.domain.repository.MetadataRepository>(relaxed = true)
    private val timelineRepository = mockk<com.lifepilot.domain.repository.TimelineRepository>(relaxed = true)
    private val reminderRepository = mockk<com.lifepilot.domain.repository.ReminderRepository>(relaxed = true)
    private val ruleEngine = mockk<RuleEngine>(relaxed = true)
    private val taskGenerator = mockk<TaskGenerator>(relaxed = true)

    private lateinit var engine: LifeStateEngineImpl

    private val testObject = LifeObject(
        objectId = "obj-1",
        profileId = "profile-1",
        objectType = "Passport",
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

    @Before
    fun setUp() {
        engine = LifeStateEngineImpl(
            objectRepository = objectRepository,
            metadataRepository = metadataRepository,
            timelineRepository = timelineRepository,
            reminderRepository = reminderRepository,
            ruleEngine = ruleEngine,
            taskGenerator = taskGenerator,
        )
    }

    @Test
    fun `processObjectEvent OBJECT_CREATED generates tasks and evaluates rules`() = runTest {
        coEvery { objectRepository.getObjectById("obj-1") } returns testObject

        engine.processObjectEvent("obj-1", "OBJECT_CREATED", "{}")

        coVerify { taskGenerator.generateObjectCreationTasks(testObject) }
        coVerify { ruleEngine.evaluateRemindersForObject("obj-1") }
    }

    @Test
    fun `processObjectEvent non-creation event only evaluates rules`() = runTest {
        engine.processObjectEvent("obj-1", "STATUS_CHANGED", "{}")

        coVerify(exactly = 0) { taskGenerator.generateObjectCreationTasks(any()) }
        coVerify { ruleEngine.evaluateRemindersForObject("obj-1") }
    }

    @Test
    fun `processMetadataUpdate evaluates rules`() = runTest {
        val entries = listOf(
            MetadataEntry(
                metadataId = "meta-1",
                objectId = "obj-1",
                fieldId = "expiry_date",
                value = "2030-01-01",
                fieldType = MetadataFieldType.DATE,
                source = MetadataSource.USER,
                confidence = null,
                version = 1,
                updatedAt = Instant.now(),
            )
        )

        engine.processMetadataUpdate("obj-1", entries)

        coVerify { ruleEngine.evaluateRemindersForObject("obj-1") }
    }

    @Test
    fun `processDocumentIngestion evaluates rules`() = runTest {
        engine.processDocumentIngestion("obj-1", "doc-1", "Scanned OCR text")

        coVerify { ruleEngine.evaluateRemindersForObject("obj-1") }
    }

    @Test
    fun `processObjectEvent OBJECT_CREATED when object not found still evaluates rules`() = runTest {
        coEvery { objectRepository.getObjectById("obj-missing") } returns null

        engine.processObjectEvent("obj-missing", "OBJECT_CREATED", "{}")

        coVerify(exactly = 0) { taskGenerator.generateObjectCreationTasks(any()) }
        coVerify { ruleEngine.evaluateRemindersForObject("obj-missing") }
    }
}
