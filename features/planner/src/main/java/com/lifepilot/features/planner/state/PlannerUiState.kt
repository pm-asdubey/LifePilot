package com.lifepilot.features.planner.state

import com.lifepilot.domain.model.Project
import com.lifepilot.domain.model.Task

data class PlannerUiState(
    val isLoading: Boolean = true,
    val projects: List<Project> = emptyList(),
    val projectTaskCounts: Map<String, Pair<Int, Int>> = emptyMap(), // projectId -> (openTasks, totalTasks)
    val tasks: List<Task> = emptyList(),
    val selectedTaskFilter: TaskFilter = TaskFilter.TODAY,
    val highlightedTaskId: String? = null,
    val error: String? = null,
    val showCreateProjectSheet: Boolean = false,
    val editingTask: Task? = null,
    val lastCompletedTaskId: String? = null,
    val lastCompletedTaskTitle: String? = null,
    val lastReopenedTaskTitle: String? = null,
)

enum class TaskFilter {
    TODAY,
    THIS_WEEK,
    ALL,
    COMPLETED,
}
