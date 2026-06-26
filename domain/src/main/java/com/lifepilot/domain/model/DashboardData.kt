package com.lifepilot.domain.model

data class DashboardData(
    val objectCount: Int,
    val pendingTaskCount: Int,
    val pendingTasks: List<Task>,
    val upcomingReminderCount: Int,
    val recentActivity: List<TimelineEntry>,
    val domainCounts: Map<String, Int>,
)
