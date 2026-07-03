package com.lifepilot.domain.model

import java.time.Instant
import java.time.LocalDate

data class Project(
    val projectId: String,
    val profileId: String,
    val title: String,
    val description: String?,
    val domain: String?,
    val status: ProjectStatus,
    val emoji: String = "🎯",
    val targetDate: LocalDate? = null,
    val isAiProposed: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class ProjectStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED,
}
