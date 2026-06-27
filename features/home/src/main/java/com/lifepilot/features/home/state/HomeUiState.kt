package com.lifepilot.features.home.state

import com.lifepilot.domain.engine.AttentionItem
import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TimelineEntry

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val profileName: String = "",
    val objectCount: Int = 0,
    val pendingTaskCount: Int = 0,
    val pendingTasks: List<Task> = emptyList(),
    val recentActivity: List<TimelineEntry> = emptyList(),
    val domainCounts: Map<String, Int> = emptyMap(),
    val attentionItems: List<AttentionItem> = emptyList(),
    val error: String? = null,
)
