package com.lifepilot.features.planner.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.engine.PlanningEngine
import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TaskPriority
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
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PlannerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val planningEngine: PlanningEngine,
    private val preferenceManager: PreferenceManager,
) : ViewModel() {

    private val selectedTaskId: String? = savedStateHandle["taskId"]

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    // Drives filter changes without spawning new collectors.
    private val selectedFilter = MutableStateFlow(
        if (selectedTaskId.isNullOrBlank()) TaskFilter.ALL else TaskFilter.ALL
    )

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
                selectedFilter,
            ) { goals, tasks, filter ->
                Triple(goals, tasks, filter)
            }
                .catch { e ->
                    Timber.e(e, "Error loading planner data")
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { (goals, tasks, filter) ->
                    val effectiveFilter = if (!selectedTaskId.isNullOrBlank()) TaskFilter.ALL else filter
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            activeGoals = goals,
                            tasks = filterTasks(tasks, effectiveFilter),
                            selectedTaskFilter = effectiveFilter,
                            highlightedTaskId = selectedTaskId,
                            error = null,
                        )
                    }
                }
        }
    }

    fun setTaskFilter(filter: TaskFilter) {
        selectedFilter.value = filter
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
        val title = _uiState.value.tasks.find { it.taskId == taskId }?.title
        viewModelScope.launch {
            planningEngine.completeTask(taskId)
                .onSuccess {
                    _uiState.update { it.copy(lastCompletedTaskId = taskId, lastCompletedTaskTitle = title) }
                }
                .onFailure { e -> Timber.e(e, "Failed to complete task: $taskId") }
        }
    }

    fun undoComplete() {
        val taskId = _uiState.value.lastCompletedTaskId ?: return
        _uiState.update { it.copy(lastCompletedTaskId = null, lastCompletedTaskTitle = null) }
        viewModelScope.launch {
            planningEngine.uncompleteTask(taskId)
                .onFailure { e -> Timber.e(e, "Failed to reopen task: $taskId") }
        }
    }

    fun clearUndoState() {
        _uiState.update { it.copy(lastCompletedTaskId = null, lastCompletedTaskTitle = null) }
    }

    fun reopenTask(taskId: String) {
        viewModelScope.launch {
            planningEngine.uncompleteTask(taskId)
                .onFailure { e -> Timber.e(e, "Failed to reopen task: $taskId") }
        }
    }

    fun openEditTask(task: Task) {
        _uiState.update { it.copy(editingTask = task) }
    }

    fun closeEditTask() {
        _uiState.update { it.copy(editingTask = null) }
    }

    fun saveEditedTask(
        taskId: String,
        title: String,
        description: String?,
        dueDate: LocalDate?,
        priority: TaskPriority,
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            planningEngine.updateTask(taskId, title, description, dueDate, priority)
                .onFailure { e -> Timber.e(e, "Failed to update task: $taskId") }
            _uiState.update { it.copy(editingTask = null) }
        }
    }

    fun openEditGoal(goal: com.lifepilot.domain.model.Goal) {
        _uiState.update { it.copy(editingGoal = goal) }
    }

    fun closeEditGoal() {
        _uiState.update { it.copy(editingGoal = null) }
    }

    fun saveEditedGoal(
        goalId: String,
        title: String,
        description: String?,
        deadline: LocalDate?,
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            planningEngine.updateGoal(
                goalId = goalId,
                title = title,
                description = description,
                deadline = deadline,
            ).onFailure { e -> Timber.e(e, "Failed to update goal: $goalId") }
            _uiState.update { it.copy(editingGoal = null) }
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
