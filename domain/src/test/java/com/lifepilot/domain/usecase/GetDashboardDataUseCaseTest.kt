package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.DashboardData
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.Reminder
import com.lifepilot.domain.model.ReminderPriority
import com.lifepilot.domain.model.ReminderStatus
import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TaskPriority
import com.lifepilot.domain.model.TaskSource
import com.lifepilot.domain.model.TaskStatus
import com.lifepilot.domain.model.TimelineEntry
import com.lifepilot.domain.model.TimelineSourceType
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ReminderRepository
import com.lifepilot.domain.repository.TaskRepository
import com.lifepilot.domain.repository.TimelineRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

class GetDashboardDataUseCaseTest {

    private val objectRepository: ObjectRepository = mockk()
    private val taskRepository: TaskRepository = mockk()
    private val reminderRepository: ReminderRepository = mockk()
    private val timelineRepository: TimelineRepository = mockk()

    private lateinit var useCase: GetDashboardDataUseCase

    @Before
    fun setUp() {
        useCase = GetDashboardDataUseCase(
            objectRepository = objectRepository,
            taskRepository = taskRepository,
            reminderRepository = reminderRepository,
            timelineRepository = timelineRepository,
        )
    }

    @Test
    fun `returns dashboard with correct counts`() = runTest {
        val profileId = "profile-1"
        val objects = listOf(
            makeObject("obj-1", "passport", "Identity"),
            makeObject("obj-2", "job", "Career"),
        )
        val tasks = listOf(
            makeTask("task-1"),
            makeTask("task-2"),
            makeTask("task-3"),
        )
        val reminders = listOf(
            makeReminder("rem-1"),
        )
        val timeline = listOf(
            makeTimeline("tl-1"),
            makeTimeline("tl-2"),
        )

        every { objectRepository.observeObjectsByProfile(profileId) } returns flowOf(objects)
        every { taskRepository.observePendingTasks(profileId) } returns flowOf(tasks)
        every { reminderRepository.observeUpcomingReminders(any()) } returns flowOf(reminders)
        every { timelineRepository.observeTimeline(profileId) } returns flowOf(timeline)

        val result = useCase(profileId).first()

        assertEquals(2, result.objectCount)
        assertEquals(3, result.pendingTaskCount)
        assertEquals(1, result.upcomingReminderCount)
        assertEquals(2, result.recentActivity.size)
    }

    @Test
    fun `pendingTasks is limited to 5`() = runTest {
        val profileId = "profile-1"
        val tasks = (1..10).map { makeTask("task-$it") }

        every { objectRepository.observeObjectsByProfile(profileId) } returns flowOf(emptyList())
        every { taskRepository.observePendingTasks(profileId) } returns flowOf(tasks)
        every { reminderRepository.observeUpcomingReminders(any()) } returns flowOf(emptyList())
        every { timelineRepository.observeTimeline(profileId) } returns flowOf(emptyList())

        val result = useCase(profileId).first()

        assertEquals(10, result.pendingTaskCount)
        assertEquals(5, result.pendingTasks.size)
    }

    @Test
    fun `domainCounts groups objects by domain`() = runTest {
        val profileId = "profile-1"
        val objects = listOf(
            makeObject("obj-1", "passport", "Identity"),
            makeObject("obj-2", "job", "Career"),
            makeObject("obj-3", "insurance", "Finance"),
            makeObject("obj-4", "bank_account", "Finance"),
        )

        every { objectRepository.observeObjectsByProfile(profileId) } returns flowOf(objects)
        every { taskRepository.observePendingTasks(profileId) } returns flowOf(emptyList())
        every { reminderRepository.observeUpcomingReminders(any()) } returns flowOf(emptyList())
        every { timelineRepository.observeTimeline(profileId) } returns flowOf(emptyList())

        val result = useCase(profileId).first()

        assertEquals(1, result.domainCounts["Identity"])
        assertEquals(1, result.domainCounts["Career"])
        assertEquals(2, result.domainCounts["Finance"])
    }

    private fun makeObject(id: String, type: String, domain: String) = LifeObject(
        objectId = id,
        profileId = "profile-1",
        objectType = type,
        domain = domain,
        title = "Test $type",
        description = null,
        status = ObjectStatus.ACTIVE,
        metadata = emptyList(),
        archived = false,
        deleted = false,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    private fun makeTask(id: String) = Task(
        taskId = id,
        goalId = null,
        objectId = "obj-1",
        title = "Task $id",
        description = null,
        priority = TaskPriority.MEDIUM,
        dueDate = null,
        status = TaskStatus.PENDING,
        source = TaskSource.RULE_ENGINE,
        completedAt = null,
    )

    private fun makeReminder(id: String) = Reminder(
        reminderId = id,
        objectId = "obj-1",
        reminderType = "test",
        triggerDate = Instant.now(),
        priority = ReminderPriority.MEDIUM,
        status = ReminderStatus.SCHEDULED,
        title = "Test reminder",
        message = "",
    )

    private fun makeTimeline(id: String) = TimelineEntry(
        timelineId = id,
        sourceId = "src-1",
        sourceType = TimelineSourceType.USER_ACTION,
        timestamp = Instant.now(),
        title = "Event $id",
        summary = null,
        objectId = "obj-1",
        objectType = "passport",
    )
}
