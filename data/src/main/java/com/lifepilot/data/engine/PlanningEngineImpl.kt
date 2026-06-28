package com.lifepilot.data.engine

import com.lifepilot.domain.engine.PlanningEngine
import com.lifepilot.domain.model.Goal
import com.lifepilot.domain.model.GoalStatus
import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TaskPriority
import com.lifepilot.domain.model.TaskSource
import com.lifepilot.domain.model.TaskStatus
import com.lifepilot.domain.repository.GoalRepository
import com.lifepilot.domain.repository.TaskRepository
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanningEngineImpl @Inject constructor(
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
) : PlanningEngine {

    override suspend fun createGoal(
        profileId: String,
        title: String,
        description: String?,
        deadline: LocalDate?,
        linkedObjectId: String?,
        suggestedTaskTitles: List<String>,
    ): Result<Goal> = runCatching {
        val now = Instant.now()
        val goal = Goal(
            goalId = UUID.randomUUID().toString(),
            profileId = profileId,
            title = title,
            description = description,
            deadline = deadline,
            status = GoalStatus.ACTIVE,
            progress = 0,
            objectId = linkedObjectId,
            notes = null,
            createdAt = now,
            updatedAt = now,
        )
        goalRepository.upsertGoal(goal)

        // Generate tasks from AI-suggested titles
        for (taskTitle in suggestedTaskTitles) {
            val task = Task(
                taskId = UUID.randomUUID().toString(),
                goalId = goal.goalId,
                objectId = linkedObjectId,
                title = taskTitle,
                description = null,
                priority = TaskPriority.MEDIUM,
                dueDate = deadline,
                status = TaskStatus.PENDING,
                source = TaskSource.GOAL,
                completedAt = null,
            )
            taskRepository.createTask(task)
        }

        Timber.d("PlanningEngine: created goal '${goal.title}' with ${suggestedTaskTitles.size} tasks")
        goal
    }

    override suspend fun updateGoal(
        goalId: String,
        title: String?,
        description: String?,
        deadline: LocalDate?,
        notes: String?,
    ): Result<Goal> = runCatching {
        val existing = goalRepository.getGoalById(goalId)
            ?: error("Goal $goalId not found")
        val updated = existing.copy(
            title = title ?: existing.title,
            description = description ?: existing.description,
            deadline = deadline ?: existing.deadline,
            notes = notes ?: existing.notes,
            updatedAt = Instant.now(),
        )
        goalRepository.upsertGoal(updated)
        updated
    }

    override suspend fun completeGoal(goalId: String): Result<Unit> = runCatching {
        goalRepository.completeGoal(goalId)
        Timber.d("PlanningEngine: completed goal $goalId")
    }

    override suspend fun archiveGoal(goalId: String): Result<Unit> = runCatching {
        val existing = goalRepository.getGoalById(goalId) ?: error("Goal $goalId not found")
        goalRepository.upsertGoal(existing.copy(status = GoalStatus.ARCHIVED, updatedAt = Instant.now()))
        Timber.d("PlanningEngine: archived goal $goalId")
    }

    override suspend fun cancelGoal(goalId: String): Result<Unit> = runCatching {
        val existing = goalRepository.getGoalById(goalId) ?: error("Goal $goalId not found")
        goalRepository.upsertGoal(existing.copy(status = GoalStatus.CANCELLED, updatedAt = Instant.now()))
        Timber.d("PlanningEngine: cancelled goal $goalId")
    }

    override suspend fun updateGoalProgress(goalId: String, progress: Int): Result<Unit> = runCatching {
        val clamped = progress.coerceIn(0, 100)
        goalRepository.updateProgress(goalId, clamped)
        if (clamped == 100) {
            goalRepository.completeGoal(goalId)
            Timber.d("PlanningEngine: goal $goalId auto-completed at 100%")
        }
    }

    override suspend fun createTask(
        profileId: String,
        title: String,
        description: String?,
        dueDate: LocalDate?,
        goalId: String?,
        objectId: String?,
        source: TaskSource,
        priority: TaskPriority,
    ): Result<Task> = runCatching {
        val task = Task(
            taskId = UUID.randomUUID().toString(),
            goalId = goalId,
            objectId = objectId,
            title = title,
            description = description,
            priority = priority,
            dueDate = dueDate,
            status = TaskStatus.PENDING,
            source = source,
            completedAt = null,
        )
        taskRepository.createTask(task)
        Timber.d("PlanningEngine: created task '${task.title}' source=$source")
        task
    }

    override suspend fun completeTask(taskId: String): Result<Unit> = runCatching {
        taskRepository.updateTaskStatus(taskId, TaskStatus.COMPLETED)
        // Recalculate parent goal progress
        val task = taskRepository.getTaskById(taskId)
        val goalId = task?.goalId
        if (goalId != null) {
            recalculateGoalProgress(goalId)
        }
        Timber.d("PlanningEngine: completed task $taskId")
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
        taskRepository.deleteTask(taskId)
    }

    override suspend fun rescheduleTask(taskId: String, newDueDate: LocalDate): Result<Unit> = runCatching {
        // TaskRepository doesn't have updateDueDate yet — this is a best-effort no-op for now.
        // Will be wired once TaskRepository.updateDueDate is added.
        Timber.w("PlanningEngine: rescheduleTask not yet fully implemented")
    }

    override suspend fun generateTasksForGoal(goalId: String): Result<List<Task>> = runCatching {
        // Placeholder — future: use SchemaEngine + goal type to generate standard tasks.
        Timber.d("PlanningEngine: generateTasksForGoal called for $goalId (no-op)")
        emptyList()
    }

    private suspend fun recalculateGoalProgress(goalId: String) {
        // Lightweight: count completed vs total tasks for this goal.
        // Full implementation requires a TaskDao query by goalId — deferred to next iteration.
        Timber.d("PlanningEngine: recalculating progress for goal $goalId")
    }
}
