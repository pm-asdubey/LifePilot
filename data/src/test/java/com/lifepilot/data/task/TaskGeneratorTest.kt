package com.lifepilot.data.task

import com.lifepilot.data.database.dao.TaskDao
import com.lifepilot.data.database.entity.TaskEntity
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.ObjectStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class TaskGeneratorTest {

    private val taskDao: TaskDao = mockk()
    private val schemaEngine: SchemaEngine = mockk()

    private lateinit var taskGenerator: TaskGenerator

    @Before
    fun setUp() {
        taskGenerator = TaskGenerator(
            taskDao = taskDao,
            schemaEngine = schemaEngine,
        )
    }

    private fun makeObject(objectType: String) = LifeObject(
        objectId = "obj-1",
        profileId = "profile-1",
        objectType = objectType,
        domain = "Identity",
        title = "Test Object",
        description = null,
        status = ObjectStatus.ACTIVE,
        metadata = emptyList(),
        archived = false,
        deleted = false,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    @Test
    fun `generateObjectCreationTasks creates tasks for passport`() = runTest {
        val obj = makeObject("passport")
        val insertedTasks = mutableListOf<TaskEntity>()

        coEvery { taskDao.getTaskTitlesByObject("obj-1") } returns emptyList()
        coEvery { taskDao.insertTask(capture(insertedTasks)) } returns Unit

        taskGenerator.generateObjectCreationTasks(obj)

        assertTrue("Should create at least one task", insertedTasks.isNotEmpty())
        assertTrue("All tasks belong to object", insertedTasks.all { it.objectId == "obj-1" })
        assertTrue("All tasks belong to profile", insertedTasks.all { it.profileId == "profile-1" })
        assertTrue("All tasks are PENDING", insertedTasks.all { it.status == "PENDING" })
        assertTrue("All tasks have RULE_ENGINE source", insertedTasks.all { it.source == "RULE_ENGINE" })
    }

    @Test
    fun `generateObjectCreationTasks creates tasks for insurance`() = runTest {
        val obj = makeObject("insurance")
        val insertedTasks = mutableListOf<TaskEntity>()

        coEvery { taskDao.getTaskTitlesByObject("obj-1") } returns emptyList()
        coEvery { taskDao.insertTask(capture(insertedTasks)) } returns Unit

        taskGenerator.generateObjectCreationTasks(obj)

        assertTrue("Insurance should have at least 1 task", insertedTasks.isNotEmpty())
        val titles = insertedTasks.map { it.title }
        assertTrue("Should include document upload task", titles.any { it.contains("certificate") || it.contains("upload") || it.contains("Upload") })
    }

    @Test
    fun `generateObjectCreationTasks does nothing for unknown type`() = runTest {
        val obj = makeObject("unknown_type")

        taskGenerator.generateObjectCreationTasks(obj)

        coVerify(exactly = 0) { taskDao.insertTask(any()) }
    }

    @Test
    fun `generateObjectCreationTasks sets due dates in future`() = runTest {
        val obj = makeObject("passport")
        val insertedTasks = mutableListOf<TaskEntity>()
        val now = System.currentTimeMillis()

        coEvery { taskDao.getTaskTitlesByObject("obj-1") } returns emptyList()
        coEvery { taskDao.insertTask(capture(insertedTasks)) } returns Unit

        taskGenerator.generateObjectCreationTasks(obj)

        assertTrue("All tasks should have future due dates", insertedTasks.all { task ->
            task.dueDate != null && task.dueDate!! > now
        })
    }

    @Test
    fun `generateObjectCreationTasks skips already-existing tasks`() = runTest {
        val obj = makeObject("passport")
        val insertedTasks = mutableListOf<TaskEntity>()

        // Pretend one task already exists
        coEvery { taskDao.getTaskTitlesByObject("obj-1") } returns listOf("Scan and upload passport document")
        coEvery { taskDao.insertTask(capture(insertedTasks)) } returns Unit

        taskGenerator.generateObjectCreationTasks(obj)

        // Should only create the remaining task (record expiry date)
        assertTrue("Should skip existing tasks", insertedTasks.all { it.title != "Scan and upload passport document" })
    }
}
