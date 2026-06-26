package com.lifepilot.features.object.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.domain.repository.DocumentRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ReminderRepository
import com.lifepilot.domain.repository.TaskRepository
import com.lifepilot.domain.repository.TimelineRepository
import com.lifepilot.features.object.state.ObjectDetailTab
import com.lifepilot.features.object.state.ObjectDetailUiState
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
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class ObjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val objectRepository: ObjectRepository,
    private val documentRepository: DocumentRepository,
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val timelineRepository: TimelineRepository,
) : ViewModel() {

    private val objectId: String = checkNotNull(savedStateHandle["objectId"])

    private val _uiState = MutableStateFlow(ObjectDetailUiState())
    val uiState: StateFlow<ObjectDetailUiState> = _uiState.asStateFlow()

    init {
        observeObject()
    }

    private fun observeObject() {
        viewModelScope.launch {
            combine(
                objectRepository.observeObjectById(objectId),
                documentRepository.observeDocumentsByObject(objectId),
                taskRepository.observeTasksByObject(objectId),
                reminderRepository.observeRemindersByObject(objectId),
                timelineRepository.observeTimelineByObject(objectId),
            ) { obj, docs, tasks, reminders, timeline ->
                ObjectDetailUiState(
                    isLoading = false,
                    lifeObject = obj,
                    documents = docs,
                    tasks = tasks,
                    reminders = reminders,
                    timeline = timeline,
                    selectedTab = _uiState.value.selectedTab,
                )
            }
            .catch { e ->
                Timber.e(e, "Error loading object detail")
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
            .collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectTab(tab: ObjectDetailTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}
