package com.lifepilot.domain.model

import java.time.Instant
import java.time.LocalDate

data class Task(
    val taskId: String,
    val goalId: String?,
    val objectId: String?,
    val projectId: String? = null,
    val title: String,
    val description: String?,
    val priority: TaskPriority,
    val dueDate: LocalDate?,
    val status: TaskStatus,
    val source: TaskSource,
    val completedAt: Instant?,
)

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT,
}

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    DISMISSED,
}

enum class TaskSource {
    MANUAL,       // User created directly in Planner
    GOAL,         // Generated when a goal was created
    RECURRING,    // From a recurring task template
    SYSTEM,       // From Rule Engine / Life State Engine
    AI_PROPOSED,  // Suggested by AI, approved by user
    RULE_ENGINE,  // Legacy alias for SYSTEM
    AI,           // Legacy alias for AI_PROPOSED
    USER,         // Legacy alias for MANUAL
}
