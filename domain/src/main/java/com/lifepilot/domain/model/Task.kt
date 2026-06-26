package com.lifepilot.domain.model

import java.time.Instant
import java.time.LocalDate

data class Task(
    val taskId: String,
    val objectId: String?,
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
    USER,
    RULE_ENGINE,
    AI,
}
