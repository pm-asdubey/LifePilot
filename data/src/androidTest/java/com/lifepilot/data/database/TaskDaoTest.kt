package com.lifepilot.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifepilot.data.database.dao.TaskDao
import com.lifepilot.data.database.entity.ProfileEntity
import com.lifepilot.data.database.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var db: LifePilotDatabase
    private lateinit var dao: TaskDao

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LifePilotDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.taskDao()
        db.profileDao().insertProfile(
            ProfileEntity(
                profileId = "profile1",
                displayName = "Test User",
                avatarPath = null,
                isPrimary = true,
                createdAt = 1_000_000L,
                updatedAt = 1_000_000L,
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetTask() = runTest {
        val task = testTask("task1")
        dao.insertTask(task)

        val retrieved = dao.getTaskById("task1")
        assertNotNull(retrieved)
        assertEquals("task1", retrieved?.taskId)
        assertEquals("Review document", retrieved?.title)
    }

    @Test
    fun observePendingTasks_excludesCompletedAndCancelled() = runTest {
        dao.insertTask(testTask("t1", status = "PENDING"))
        dao.insertTask(testTask("t2", status = "IN_PROGRESS"))
        dao.insertTask(testTask("t3", status = "COMPLETED"))
        dao.insertTask(testTask("t4", status = "CANCELLED"))

        val pending = dao.observePendingTasks("profile1").first()
        assertEquals(2, pending.size)
        assertTrue(pending.all { it.status in setOf("PENDING", "IN_PROGRESS") })
    }

    @Test
    fun updateTaskStatus_marksComplete() = runTest {
        dao.insertTask(testTask("task1", status = "PENDING"))
        val completedAt = System.currentTimeMillis()
        dao.updateTaskStatus("task1", "COMPLETED", completedAt)

        val task = dao.getTaskById("task1")
        assertEquals("COMPLETED", task?.status)
        assertEquals(completedAt, task?.completedAt)
    }

    @Test
    fun deleteTask_removesFromDb() = runTest {
        dao.insertTask(testTask("task1"))
        dao.deleteTask("task1")

        val retrieved = dao.getTaskById("task1")
        assertNull(retrieved)
    }

    @Test
    fun observeTasksByObject_filtersCorrectly() = runTest {
        dao.insertTask(testTask("t1", objectId = "obj1"))
        dao.insertTask(testTask("t2", objectId = "obj1"))
        dao.insertTask(testTask("t3", objectId = "obj2"))

        val obj1Tasks = dao.observeTasksByObject("obj1").first()
        assertEquals(2, obj1Tasks.size)
        assertTrue(obj1Tasks.all { it.objectId == "obj1" })
    }

    @Test
    fun observeTasksByProfile_orderedByDueDateAsc() = runTest {
        val later = 2_000_000L
        val earlier = 1_000_000L
        dao.insertTask(testTask("t1", dueDate = later))
        dao.insertTask(testTask("t2", dueDate = earlier))

        val tasks = dao.observeTasksByProfile("profile1").first()
        assertEquals("t2", tasks[0].taskId)
        assertEquals("t1", tasks[1].taskId)
    }

    private fun testTask(
        id: String,
        objectId: String? = null,
        status: String = "PENDING",
        priority: String = "MEDIUM",
        dueDate: Long? = null,
    ) = TaskEntity(
        taskId = id,
        profileId = "profile1",
        objectId = objectId,
        title = "Review document",
        description = null,
        priority = priority,
        dueDate = dueDate,
        status = status,
        source = "manual",
        completedAt = null,
    )
}
