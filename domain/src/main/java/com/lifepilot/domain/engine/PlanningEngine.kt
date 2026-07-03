package com.lifepilot.domain.engine

import com.lifepilot.domain.model.Goal
import com.lifepilot.domain.model.Task
import java.time.LocalDate

/**
 * The Planning Engine is the canonical executor for all commitment mutations.
 *
 * AI never writes directly to GoalRepository or TaskRepository.
 * AI requests actions. PlanningEngine executes them.
 *
 * This ensures:
 * - A single audit trail for all commitment changes
 * - Future AI providers use the same pipeline
 * - Planner remains the canonical owner of Goals and Tasks
 */
interface PlanningEngine {
    /** Create a goal from a user-approved AI proposal. Status will be ACTIVE. */
    suspend fun createGoal(
        profileId: String,
        title: String,
        description: String?,
        deadline: LocalDate?,
        linkedObjectId: String?,
        suggestedTaskTitles: List<String>,
    ): Result<Goal>

    /** Update goal details (title, description, deadline, notes). */
    suspend fun updateGoal(
        goalId: String,
        title: String? = null,
        description: String? = null,
        deadline: LocalDate? = null,
        notes: String? = null,
    ): Result<Goal>

    /** Mark a goal complete and update its linked tasks accordingly. */
    suspend fun completeGoal(goalId: String): Result<Unit>

    /** Archive a goal (intentionally paused). */
    suspend fun archiveGoal(goalId: String): Result<Unit>

    /** Cancel a goal (abandoned). */
    suspend fun cancelGoal(goalId: String): Result<Unit>

    /** Update goal progress (0-100). Automatically completes goal at 100. */
    suspend fun updateGoalProgress(goalId: String, progress: Int): Result<Unit>

    /** Create a standalone task from an AI proposal or manual entry. */
    suspend fun createTask(
        profileId: String,
        title: String,
        description: String?,
        dueDate: LocalDate?,
        goalId: String?,
        objectId: String?,
        projectId: String? = null,
        source: com.lifepilot.domain.model.TaskSource,
        priority: com.lifepilot.domain.model.TaskPriority,
    ): Result<Task>

    /** Update task fields. All values replace the current state. */
    suspend fun updateTask(
        taskId: String,
        title: String,
        description: String?,
        dueDate: LocalDate?,
        priority: com.lifepilot.domain.model.TaskPriority,
    ): Result<Task>

    /** Mark a task as completed. Updates parent goal progress automatically. */
    suspend fun completeTask(taskId: String): Result<Unit>

    /** Reopen a completed task — sets status back to PENDING and clears completedAt. */
    suspend fun uncompleteTask(taskId: String): Result<Unit>

    /** Delete a task. Does not affect Life State. */
    suspend fun deleteTask(taskId: String): Result<Unit>

    /** Reschedule a task's due date. */
    suspend fun rescheduleTask(taskId: String, newDueDate: LocalDate): Result<Unit>

    /** Generate standard tasks for a new goal based on its type and linked object. */
    suspend fun generateTasksForGoal(goalId: String): Result<List<Task>>
}
