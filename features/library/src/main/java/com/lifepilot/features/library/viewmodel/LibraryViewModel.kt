package com.lifepilot.features.library.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.features.library.state.DomainItem
import com.lifepilot.features.library.state.LibraryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val objectRepository: ObjectRepository,
    private val schemaEngine: SchemaEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val selectedDomain = MutableStateFlow<String?>(null)

    init {
        observeLibrary()
    }

    private fun observeLibrary() {
        viewModelScope.launch {
            profileRepository.observeActiveProfile()
                .flatMapLatest { profile ->
                    if (profile == null) return@flatMapLatest flowOf(null)
                    combine(
                        objectRepository.observeObjectsByProfile(profile.profileId),
                        objectRepository.observeObjectCountByDomain(profile.profileId),
                        selectedDomain,
                    ) { objects, domainCounts, domain ->
                        Triple(objects, domainCounts, domain)
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
                    val (objects, domainCounts, domain) = result
                    val domains = domainCounts.entries.map { (d, count) ->
                        DomainItem(
                            domain = d,
                            objectCount = count,
                            displayName = d,
                        )
                    }.sortedBy { it.domain }

                    val filteredObjects = if (domain != null) {
                        objects.filter { it.domain == domain }
                    } else {
                        objects
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            domains = domains,
                            objects = filteredObjects,
                            selectedDomain = domain,
                            error = null,
                        )
                    }
                }
        }
    }

    fun selectDomain(domain: String?) {
        selectedDomain.value = domain
    }
}
