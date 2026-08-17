package com.lifepilot.data.engine

import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TaskPriority
import com.lifepilot.domain.model.TaskSource
import com.lifepilot.domain.model.TaskStatus
import com.lifepilot.domain.repository.GoalRepository
import com.lifepilot.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Guards the goal-progress recalculation that [PlanningEngineImpl.completeTask]/`uncompleteTask`
 * perform — previously a silent no-op (the engine claimed to recalc but did nothing).
 */
class PlanningEngineImplTest {

    private val goalRepository = mockk<GoalRepository>(relaxed = true)
    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private val engine = PlanningEngineImpl(goalRepository, taskRepository)

    private fun task(id: String, goalId: String?, status: TaskStatus) = Task(
        taskId = id,
        goalId = goalId,
        objectId = null,
        title = "T $id",
        description = null,
        priority = TaskPriority.MEDIUM,
        dueDate = null,
        status = status,
        source = TaskSource.USER,
        completedAt = null,
    )

    @Test
    fun `completeTask writes goal progress as completed over total`() = runTest {
        val goalId = "g1"
        coEvery { taskRepository.getTaskById("t1") } returns task("t1", goalId, TaskStatus.COMPLETED)
        coEvery { taskRepository.getTasksByGoal(goalId) } returns listOf(
            task("t1", goalId, TaskStatus.COMPLETED),
            task("t2", goalId, TaskStatus.COMPLETED),
            task("t3", goalId, TaskStatus.PENDING),
            task("t4", goalId, TaskStatus.PENDING),
        )

        engine.completeTask("t1")

        coVerify { goalRepository.updateProgress(goalId, 50) }
    }

    @Test
    fun `task with no goal does not touch goal progress`() = runTest {
        coEvery { taskRepository.getTaskById("t1") } returns task("t1", goalId = null, TaskStatus.COMPLETED)

        engine.completeTask("t1")

        coVerify(exactly = 0) { goalRepository.updateProgress(any(), any()) }
    }

    @Test
    fun `all tasks complete sets progress to 100`() = runTest {
        val goalId = "g2"
        coEvery { taskRepository.getTaskById("t1") } returns task("t1", goalId, TaskStatus.COMPLETED)
        coEvery { taskRepository.getTasksByGoal(goalId) } returns listOf(
            task("t1", goalId, TaskStatus.COMPLETED),
            task("t2", goalId, TaskStatus.COMPLETED),
        )

        engine.completeTask("t1")

        coVerify { goalRepository.updateProgress(goalId, 100) }
    }
}
