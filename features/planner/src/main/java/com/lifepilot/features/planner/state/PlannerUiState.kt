package com.lifepilot.features.planner.state

import com.lifepilot.domain.model.Goal
import com.lifepilot.domain.model.Task

data class PlannerUiState(
    val isLoading: Boolean = true,
    val activeGoals: List<Goal> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val selectedTaskFilter: TaskFilter = TaskFilter.TODAY,
    val highlightedTaskId: String? = null,
    val error: String? = null,
    val showCreateGoalSheet: Boolean = false,
    val editingTask: Task? = null,
    val editingGoal: Goal? = null,
    val lastCompletedTaskId: String? = null,
    val lastCompletedTaskTitle: String? = null,
)

enum class TaskFilter {
    TODAY,
    THIS_WEEK,
    ALL,
    COMPLETED,
}
