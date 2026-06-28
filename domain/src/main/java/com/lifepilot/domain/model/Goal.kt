package com.lifepilot.domain.model

import java.time.Instant
import java.time.LocalDate

data class Goal(
    val goalId: String,
    val profileId: String,
    val title: String,
    val description: String?,
    val deadline: LocalDate?,
    val status: GoalStatus,
    val progress: Int,
    val objectId: String?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class GoalStatus { ACTIVE, COMPLETED, DISMISSED }
