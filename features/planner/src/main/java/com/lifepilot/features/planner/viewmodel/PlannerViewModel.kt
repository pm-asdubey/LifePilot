package com.lifepilot.features.planner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.engine.PlanningEngine
import com.lifepilot.domain.model.Goal
import com.lifepilot.domain.model.GoalStatus
import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TaskStatus
import com.lifepilot.domain.repository.GoalRepository
import com.lifepilot.domain.repository.TaskRepository
import com.lifepilot.features.planner.state.PlannerUiState
import com.lifepilot.features.planner.state.TaskFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val planningEngine: PlanningEngine,
    private val preferenceManager: PreferenceManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            val profileId = preferenceManager.getActiveProfileId() ?: run {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            combine(
                goalRepository.observeActiveGoals(profileId),
                taskRepository.observeTasksByProfile(profileId),
            ) { goals, tasks -> Pair(goals, tasks) }
                .catch { e ->
                    Timber.e(e, "Error loading planner data")
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { (goals, tasks) ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            activeGoals = goals,
                            tasks = filterTasks(tasks, state.selectedTaskFilter),
                            error = null,
                        )
                    }
                }
        }
    }

    fun setTaskFilter(filter: TaskFilter) {
        val allTasks = _uiState.value.tasks
        _uiState.update { state ->
            state.copy(
                selectedTaskFilter = filter,
            )
        }
        viewModelScope.launch {
            val profileId = preferenceManager.getActiveProfileId() ?: return@launch
            taskRepository.observeTasksByProfile(profileId)
                .catch { }
                .collect { tasks ->
                    _uiState.update { state ->
                        state.copy(tasks = filterTasks(tasks, filter))
                    }
                }
        }
    }

    fun showCreateGoalSheet() {
        _uiState.update { it.copy(showCreateGoalSheet = true) }
    }

    fun hideCreateGoalSheet() {
        _uiState.update { it.copy(showCreateGoalSheet = false) }
    }

    fun createGoal(title: String, description: String?, deadline: LocalDate?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val profileId = preferenceManager.getActiveProfileId() ?: return@launch
            planningEngine.createGoal(
                profileId = profileId,
                title = title.trim(),
                description = description?.trim()?.takeIf { it.isNotBlank() },
                deadline = deadline,
                linkedObjectId = null,
                suggestedTaskTitles = emptyList(),
            ).onFailure { e -> Timber.e(e, "Failed to create goal") }
            _uiState.update { it.copy(showCreateGoalSheet = false) }
        }
    }

    fun completeGoal(goalId: String) {
        viewModelScope.launch {
            planningEngine.completeGoal(goalId)
                .onFailure { e -> Timber.e(e, "Failed to complete goal: $goalId") }
        }
    }

    fun dismissGoal(goalId: String) {
        viewModelScope.launch {
            planningEngine.cancelGoal(goalId)
                .onFailure { e -> Timber.e(e, "Failed to cancel goal: $goalId") }
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            planningEngine.completeTask(taskId)
                .onFailure { e -> Timber.e(e, "Failed to complete task: $taskId") }
        }
    }

    private fun filterTasks(tasks: List<Task>, filter: TaskFilter): List<Task> {
        val today = LocalDate.now()
        val endOfWeek = today.plusDays(7)
        return when (filter) {
            TaskFilter.TODAY -> tasks.filter { task ->
                val dueDate = task.dueDate
                task.status != TaskStatus.COMPLETED &&
                    task.status != TaskStatus.DISMISSED &&
                    (dueDate == null || !dueDate.isAfter(today))
            }
            TaskFilter.THIS_WEEK -> tasks.filter { task ->
                val dueDate = task.dueDate
                task.status != TaskStatus.COMPLETED &&
                    task.status != TaskStatus.DISMISSED &&
                    (dueDate == null || !dueDate.isAfter(endOfWeek))
            }
            TaskFilter.ALL -> tasks.filter { task ->
                task.status != TaskStatus.COMPLETED &&
                    task.status != TaskStatus.DISMISSED
            }
            TaskFilter.COMPLETED -> tasks.filter { task ->
                task.status == TaskStatus.COMPLETED
            }
        }
    }
}
