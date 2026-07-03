package com.lifepilot.features.library.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.repository.DomainRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.usecase.ArchiveObjectUseCase
import com.lifepilot.domain.usecase.DeleteObjectUseCase
import com.lifepilot.features.library.state.DomainItem
import com.lifepilot.features.library.state.LibraryUiState
import com.lifepilot.features.library.state.LibrarySortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val objectRepository: ObjectRepository,
    private val domainRepository: DomainRepository,
    private val schemaEngine: SchemaEngine,
    private val archiveObjectUseCase: ArchiveObjectUseCase,
    private val deleteObjectUseCase: DeleteObjectUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val selectedDomain = MutableStateFlow<String?>(null)
    private val sortOrder = MutableStateFlow(LibrarySortOrder.UPDATED_RECENT)

    init {
        observeLibrary()
    }

    private fun observeLibrary() {
        viewModelScope.launch {
            profileRepository.observeActiveProfile()
                .flatMapLatest { profile ->
                    if (profile == null) return@flatMapLatest flowOf(null)
                    val recordsFlow = combine(
                        objectRepository.observeObjectsByProfile(profile.profileId)
                            .onStart { emit(emptyList()) },
                        objectRepository.observeObjectCountByDomain(profile.profileId)
                            .onStart { emit(emptyMap()) },
                        domainRepository.observeAllDomainLifeStates(profile.profileId)
                            .onStart { emit(emptyList()) },
                    ) { objects, domainCounts, lifeStates ->
                        RecordsData(objects, domainCounts, lifeStates.associateBy { it.domain })
                    }
                    combine(
                        recordsFlow,
                        selectedDomain,
                        sortOrder,
                    ) { records, domain, sort ->
                        LibraryData(records, domain, sort)
                    }
                }
                .catch { e ->
                    Timber.e(e, "Error in library")
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { result ->
                    if (result == null) {
                        _uiState.update { it.copy(isLoading = false) }
                        return@collect
                    }
                    val allKnownDomains =
                        (schemaEngine.getAllDomains() + result.records.domainCounts.keys).distinct().sorted()
                    val domains = allKnownDomains.map { d ->
                        DomainItem(
                            domain = d,
                            objectCount = result.records.domainCounts[d] ?: 0,
                            displayName = d,
                            lifeState = result.records.lifeStateMap[d],
                        )
                    }
                    val filteredObjects = (if (result.domain != null) {
                        result.records.objects.filter { it.domain == result.domain }
                    } else {
                        result.records.objects
                    }).let { list ->
                        when (result.sort) {
                            LibrarySortOrder.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
                            LibrarySortOrder.TITLE_DESC -> list.sortedByDescending { it.title.lowercase() }
                            LibrarySortOrder.UPDATED_RECENT -> list.sortedByDescending { it.updatedAt }
                            LibrarySortOrder.STATUS -> list.sortedBy { it.status.ordinal }
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            domains = domains,
                            objects = filteredObjects,
                            selectedDomain = result.domain,
                            sortOrder = result.sort,
                            error = null,
                        )
                    }
                }
        }
    }

    private data class RecordsData(
        val objects: List<com.lifepilot.domain.model.LifeObject>,
        val domainCounts: Map<String, Int>,
        val lifeStateMap: Map<String, com.lifepilot.domain.model.DomainLifeState>,
    )

    private data class LibraryData(
        val records: RecordsData,
        val domain: String?,
        val sort: LibrarySortOrder,
    )

    fun selectDomain(domain: String?) {
        selectedDomain.value = domain
    }

    fun setSortOrder(order: LibrarySortOrder) {
        sortOrder.value = order
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = false) }
    }

    fun enterSelectionMode(objectId: String) {
        _uiState.update {
            it.copy(isSelecting = true, selectedObjectIds = setOf(objectId))
        }
    }

    fun toggleObjectSelection(objectId: String) {
        _uiState.update { state ->
            val updated = if (objectId in state.selectedObjectIds) {
                state.selectedObjectIds - objectId
            } else {
                state.selectedObjectIds + objectId
            }
            state.copy(
                selectedObjectIds = updated,
                isSelecting = updated.isNotEmpty(),
            )
        }
    }

    fun exitSelectionMode() {
        _uiState.update { it.copy(isSelecting = false, selectedObjectIds = emptySet()) }
    }

    fun archiveSelected() {
        val ids = _uiState.value.selectedObjectIds.toList()
        exitSelectionMode()
        viewModelScope.launch {
            ids.forEach { id ->
                archiveObjectUseCase(id)
                    .onFailure { Timber.e(it, "Failed to archive $id") }
            }
        }
    }

    fun deleteSelected() {
        val ids = _uiState.value.selectedObjectIds.toList()
        exitSelectionMode()
        viewModelScope.launch {
            ids.forEach { id ->
                deleteObjectUseCase(id)
                    .onFailure { Timber.e(it, "Failed to delete $id") }
            }
        }
    }
}
