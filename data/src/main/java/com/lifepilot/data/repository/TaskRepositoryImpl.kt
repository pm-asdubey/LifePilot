package com.lifepilot.data.repository

import com.lifepilot.data.database.dao.TaskDao
import com.lifepilot.data.mapper.toDomain
import com.lifepilot.data.mapper.toEntity
import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TaskStatus
import com.lifepilot.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val preferenceManager: PreferenceManager,
) : TaskRepository {

    override fun observeTasksByProfile(profileId: String): Flow<List<Task>> =
        taskDao.observeTasksByProfile(profileId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeTasksByObject(objectId: String): Flow<List<Task>> =
        taskDao.observeTasksByObject(objectId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observePendingTasks(profileId: String): Flow<List<Task>> =
        taskDao.observePendingTasks(profileId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun createTask(task: Task): Task {
        val profileId = preferenceManager.getActiveProfileId() ?: error("No active profile")
        taskDao.insertTask(task.toEntity(profileId))
        return task
    }

    override suspend fun updateTask(task: Task) {
        val profileId = preferenceManager.getActiveProfileId() ?: error("No active profile")
        taskDao.updateTask(task.toEntity(profileId))
    }

    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus) {
        val completedAt = if (status == TaskStatus.COMPLETED) Instant.now().toEpochMilli() else null
        taskDao.updateTaskStatus(taskId, status.name, completedAt)
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.deleteTask(taskId)
    }

    override suspend fun getTaskById(taskId: String): Task? =
        taskDao.getTaskById(taskId)?.toDomain()

    override suspend fun getPendingTaskCountForObject(objectId: String): Int =
        taskDao.getPendingTaskCount(objectId)
}
