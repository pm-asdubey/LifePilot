package com.lifepilot.data.mapper

import com.lifepilot.data.database.entity.TaskEntity
import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TaskPriority
import com.lifepilot.domain.model.TaskSource
import com.lifepilot.domain.model.TaskStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun TaskEntity.toDomain(): Task = Task(
    taskId = taskId,
    objectId = objectId,
    title = title,
    description = description,
    priority = TaskPriority.valueOf(priority),
    dueDate = dueDate?.let { LocalDate.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) },
    status = TaskStatus.valueOf(status),
    source = TaskSource.valueOf(source),
    completedAt = completedAt?.let { Instant.ofEpochMilli(it) },
)

fun Task.toEntity(profileId: String): TaskEntity = TaskEntity(
    taskId = taskId,
    profileId = profileId,
    objectId = objectId,
    title = title,
    description = description,
    priority = priority.name,
    dueDate = dueDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
    status = status.name,
    source = source.name,
    completedAt = completedAt?.toEpochMilli(),
    goalId = null,
)
