package com.lifepilot.features.objectdetail.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.TaskStatus
import com.lifepilot.domain.usecase.CompleteTaskUseCase
import com.lifepilot.domain.usecase.DeleteObjectUseCase
import com.lifepilot.domain.repository.DocumentRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.RelationshipRepository
import com.lifepilot.domain.repository.ReminderRepository
import com.lifepilot.domain.repository.TaskRepository
import com.lifepilot.domain.repository.TimelineRepository
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.usecase.ArchiveObjectUseCase
import com.lifepilot.domain.usecase.LinkObjectsUseCase
import com.lifepilot.domain.usecase.UpdateObjectStatusUseCase
import com.lifepilot.features.objectdetail.state.ObjectDetailTab
import com.lifepilot.features.objectdetail.state.ObjectDetailUiState
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
class ObjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val objectRepository: ObjectRepository,
    private val documentRepository: DocumentRepository,
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val timelineRepository: TimelineRepository,
    private val relationshipRepository: RelationshipRepository,
    private val preferenceManager: PreferenceManager,
    private val archiveObjectUseCase: ArchiveObjectUseCase,
    private val updateObjectStatusUseCase: UpdateObjectStatusUseCase,
    private val linkObjectsUseCase: LinkObjectsUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteObjectUseCase: DeleteObjectUseCase,
) : ViewModel() {

    private val objectId: String = checkNotNull(savedStateHandle["objectId"])

    private val _uiState = MutableStateFlow(ObjectDetailUiState())
    val uiState: StateFlow<ObjectDetailUiState> = _uiState.asStateFlow()

    private val _allProfileObjects = MutableStateFlow<List<LifeObject>>(emptyList())
    val allProfileObjects: StateFlow<List<LifeObject>> = _allProfileObjects.asStateFlow()

    init {
        observeObject()
        observeRelationships()
        observeAllObjects()
        observePendingVerification()
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
                _uiState.value.copy(
                    isLoading = false,
                    lifeObject = obj,
                    documents = docs,
                    tasks = tasks,
                    reminders = reminders,
                    timeline = timeline,
                )
            }
            .catch { e ->
                Timber.e(e, "Error loading object detail")
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
            .collect { state ->
                _uiState.update { current ->
                    state.copy(
                        selectedTab = current.selectedTab,
                        relationships = current.relationships,
                        relatedObjects = current.relatedObjects,
                        showLinkObjectSheet = current.showLinkObjectSheet,
                        pendingVerificationVersionId = current.pendingVerificationVersionId,
                    )
                }
            }
        }
    }

    private fun observePendingVerification() {
        viewModelScope.launch {
            preferenceManager.observePendingVerification(objectId)
                .catch { Timber.e(it, "Error observing pending verification") }
                .collect { versionId ->
                    _uiState.update { it.copy(pendingVerificationVersionId = versionId) }
                }
        }
    }

    fun dismissPendingVerification() {
        viewModelScope.launch {
            preferenceManager.clearPendingVerification(objectId)
        }
    }

    private fun observeRelationships() {
        viewModelScope.launch {
            relationshipRepository.observeRelationshipsByObject(objectId)
                .catch { e -> Timber.e(e, "Error observing relationships") }
                .collect { relationships ->
                    val relatedObjectIds = relationships.map { rel ->
                        if (rel.sourceObjectId == objectId) rel.targetObjectId else rel.sourceObjectId
                    }.toSet()
                    val relatedObjects = relatedObjectIds.mapNotNull { id ->
                        objectRepository.getObjectById(id)
                    }.associateBy { it.objectId }
                    _uiState.update { it.copy(relationships = relationships, relatedObjects = relatedObjects) }
                }
        }
    }

    private fun observeAllObjects() {
        viewModelScope.launch {
            val profileId = preferenceManager.getActiveProfileId() ?: return@launch
            objectRepository.observeObjectsByProfile(profileId)
                .catch { e -> Timber.e(e, "Error observing all objects") }
                .collect { objects ->
                    _allProfileObjects.value = objects.filter { it.objectId != objectId }
                }
        }
    }

    fun selectTab(tab: ObjectDetailTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun showLinkObjectSheet() {
        _uiState.update { it.copy(showLinkObjectSheet = true) }
    }

    fun hideLinkObjectSheet() {
        _uiState.update { it.copy(showLinkObjectSheet = false) }
    }

    fun linkObject(targetObjectId: String, relationshipType: String) {
        viewModelScope.launch {
            linkObjectsUseCase(objectId, targetObjectId, relationshipType)
                .onSuccess { _uiState.update { it.copy(showLinkObjectSheet = false) } }
                .onFailure { e -> Timber.e(e, "Failed to link objects") }
        }
    }

    fun unlinkObject(relationshipId: String) {
        viewModelScope.launch {
            runCatching { relationshipRepository.deleteRelationship(relationshipId) }
                .onFailure { e -> Timber.e(e, "Failed to unlink objects") }
        }
    }

    fun archiveObject(onArchived: () -> Unit = {}) {
        viewModelScope.launch {
            archiveObjectUseCase(objectId)
                .onSuccess { onArchived() }
                .onFailure { e ->
                    Timber.e(e, "Failed to archive object")
                    _uiState.update { it.copy(error = "Failed to archive: ${e.message}") }
                }
        }
    }

    fun updateStatus(status: ObjectStatus) {
        viewModelScope.launch {
            updateObjectStatusUseCase(objectId, status).onFailure { e ->
                Timber.e(e, "Failed to update status")
                _uiState.update { it.copy(error = "Failed to update status: ${e.message}") }
            }
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            completeTaskUseCase(taskId).onFailure { e ->
                Timber.e(e, "Failed to complete task $taskId")
                _uiState.update { it.copy(error = "Failed to complete task: ${e.message}") }
            }
        }
    }

    fun deleteObject(onDeleted: () -> Unit) {
        viewModelScope.launch {
            deleteObjectUseCase(objectId)
                .onSuccess { onDeleted() }
                .onFailure { e ->
                    Timber.e(e, "Failed to delete object $objectId")
                    _uiState.update { it.copy(error = "Failed to delete: ${e.message}") }
                }
        }
    }
}
