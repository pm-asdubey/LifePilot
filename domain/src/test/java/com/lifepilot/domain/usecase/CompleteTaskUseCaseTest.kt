package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.TaskStatus
import com.lifepilot.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CompleteTaskUseCaseTest {

    private val taskRepository: TaskRepository = mockk()
    private lateinit var useCase: CompleteTaskUseCase

    @Before
    fun setUp() {
        useCase = CompleteTaskUseCase(taskRepository = taskRepository)
    }

    @Test
    fun `invoke marks task as completed`() = runTest {
        coEvery { taskRepository.updateTaskStatus("task-1", TaskStatus.COMPLETED) } returns Unit

        val result = useCase("task-1")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { taskRepository.updateTaskStatus("task-1", TaskStatus.COMPLETED) }
    }

    @Test
    fun `invoke returns failure when repository throws`() = runTest {
        coEvery {
            taskRepository.updateTaskStatus("task-1", TaskStatus.COMPLETED)
        } throws RuntimeException("DB error")

        val result = useCase("task-1")

        assertTrue(result.isFailure)
    }
}
