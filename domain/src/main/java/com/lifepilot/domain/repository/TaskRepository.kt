package com.lifepilot.domain.repository

import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasksByProfile(profileId: String): Flow<List<Task>>
    fun observeTasksByObject(objectId: String): Flow<List<Task>>
    fun observePendingTasks(profileId: String): Flow<List<Task>>
    suspend fun createTask(task: Task): Task
    suspend fun updateTask(task: Task)
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus)
    suspend fun deleteTask(taskId: String)
    suspend fun getTaskById(taskId: String): Task?
    suspend fun getPendingTaskCountForObject(objectId: String): Int
}
