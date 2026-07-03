package com.lifepilot.features.planner.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.repository.ProjectRepository
import com.lifepilot.domain.repository.TaskRepository
import com.lifepilot.features.planner.state.ProjectWorkspaceUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProjectWorkspaceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val preferenceManager: PreferenceManager,
) : ViewModel() {

    private val projectId: String = checkNotNull(savedStateHandle["projectId"])

    private val _uiState = MutableStateFlow(ProjectWorkspaceUiState())
    val uiState: StateFlow<ProjectWorkspaceUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                projectRepository.observeObjectsForProject(projectId),
                projectRepository.observeTasksForProject(projectId),
            ) { objects, tasks ->
                object {
                    val linkedObjects = objects
                    val tasks = tasks
                }
            }
                .catch { e ->
                    Timber.e(e, "Error loading project workspace data for $projectId")
                    _uiState.update { it.copy(isLoading = false) }
                }
                .collect { data ->
                    val project = projectRepository.getProject(projectId)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            project = project,
                            tasks = data.tasks,
                            linkedObjects = data.linkedObjects,
                        )
                    }
                }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }
}
