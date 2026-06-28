package com.lifepilot.features.planner.state

import com.lifepilot.domain.model.Goal
import com.lifepilot.domain.model.Task

data class PlannerUiState(
    val isLoading: Boolean = true,
    val activeGoals: List<Goal> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val selectedTaskFilter: TaskFilter = TaskFilter.TODAY,
    val error: String? = null,
    val showCreateGoalSheet: Boolean = false,
)

enum class TaskFilter {
    TODAY,
    THIS_WEEK,
    ALL,
    COMPLETED,
}
