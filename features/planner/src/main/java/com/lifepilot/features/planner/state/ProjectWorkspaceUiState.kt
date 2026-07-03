package com.lifepilot.features.planner.state

import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.Project
import com.lifepilot.domain.model.Task

data class ProjectWorkspaceUiState(
    val isLoading: Boolean = true,
    val project: Project? = null,
    val tasks: List<Task> = emptyList(),
    val linkedObjects: List<LifeObject> = emptyList(),
    val selectedTab: Int = 0,
)
