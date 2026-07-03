package com.lifepilot.features.planner.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.engine.PlanningEngine
import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TaskPriority
import com.lifepilot.domain.model.TaskStatus
import com.lifepilot.domain.repository.ProjectRepository
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
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val planningEngine: PlanningEngine,
    private val preferenceManager: PreferenceManager,
) : ViewModel() {

    // Observed as a flow so re-navigation from search (launchSingleTop) propagates the new taskId.
    private val deepLinkTaskId: MutableStateFlow<String?> =
        MutableStateFlow(savedStateHandle["taskId"])

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    private val selectedFilter = MutableStateFlow(TaskFilter.ALL)

    init {
        // React to savedStateHandle updates that arrive when navigating from Search with launchSingleTop.
        viewModelScope.launch {
            savedStateHandle.getStateFlow<String>("taskId", "")
                .collect { taskId ->
                    deepLinkTaskId.value = taskId.takeIf { it.isNotBlank() }
                }
        }
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            val profileId = preferenceManager.getActiveProfileId() ?: run {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            combine(
                projectRepository.observeProjects(profileId),
                taskRepository.observeTasksByProfile(profileId),
                selectedFilter,
                deepLinkTaskId,
            ) { projects, tasks, filter, taskId ->
                object {
                    val projects = projects
                    val tasks = tasks
                    val filter = filter
                    val taskId = taskId
                }
            }
                .catch { e ->
                    Timber.e(e, "Error loading planner data")
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { data ->
                    val effectiveFilter = if (!data.taskId.isNullOrBlank()) TaskFilter.ALL else data.filter

                    // Compute task counts per project: projectId -> (openTasks, totalTasks)
                    val projectTaskCounts = data.projects.associate { project ->
                        val projectTasks = data.tasks.filter { it.projectId == project.projectId }
                        val openCount = projectTasks.count {
                            it.status != TaskStatus.COMPLETED && it.status != TaskStatus.DISMISSED
                        }
                        project.projectId to Pair(openCount, projectTasks.size)
                    }

                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            projects = data.projects,
                            projectTaskCounts = projectTaskCounts,
                            tasks = filterTasks(data.tasks, effectiveFilter),
                            selectedTaskFilter = effectiveFilter,
                            highlightedTaskId = data.taskId,
                            error = null,
                        )
                    }
                }
        }
    }

    fun setTaskFilter(filter: TaskFilter) {
        selectedFilter.value = filter
    }

    fun showCreateProjectSheet() {
        _uiState.update { it.copy(showCreateProjectSheet = true) }
    }

    fun hideCreateProjectSheet() {
        _uiState.update { it.copy(showCreateProjectSheet = false) }
    }

    fun createProject(title: String, emoji: String, description: String?, targetDate: LocalDate?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val profileId = preferenceManager.getActiveProfileId() ?: return@launch
            runCatching {
                projectRepository.createProject(
                    profileId = profileId,
                    title = title.trim(),
                    description = description?.trim()?.takeIf { it.isNotBlank() },
                    domain = null,
                    emoji = emoji,
                    targetDate = targetDate,
                    isAiProposed = false,
                )
            }.onFailure { e -> Timber.e(e, "Failed to create project") }
            _uiState.update { it.copy(showCreateProjectSheet = false) }
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
        val title = _uiState.value.tasks.find { it.taskId == taskId }?.title
        viewModelScope.launch {
            planningEngine.uncompleteTask(taskId)
                .onSuccess {
                    _uiState.update { it.copy(lastReopenedTaskTitle = title) }
                }
                .onFailure { e -> Timber.e(e, "Failed to reopen task: $taskId") }
        }
    }

    fun clearReopenState() {
        _uiState.update { it.copy(lastReopenedTaskTitle = null) }
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
