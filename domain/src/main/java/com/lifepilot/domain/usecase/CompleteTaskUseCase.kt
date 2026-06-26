package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.TaskStatus
import com.lifepilot.domain.repository.TaskRepository
import javax.inject.Inject

class CompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String): Result<Unit> = runCatching {
        taskRepository.updateTaskStatus(taskId, TaskStatus.COMPLETED)
    }
}
