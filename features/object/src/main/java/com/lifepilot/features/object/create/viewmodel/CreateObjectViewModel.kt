package com.lifepilot.features.objectdetail.create.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.usecase.CreateObjectUseCase
import com.lifepilot.features.objectdetail.create.state.CreateObjectState
import com.lifepilot.features.objectdetail.create.state.CreateObjectStep
import com.lifepilot.features.objectdetail.create.state.ObjectTypeItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CreateObjectViewModel @Inject constructor(
    private val schemaEngine: SchemaEngine,
    private val profileRepository: ProfileRepository,
    private val createObjectUseCase: CreateObjectUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateObjectState())
    val state: StateFlow<CreateObjectState> = _state.asStateFlow()

    init {
        // Observe schema changes reactively so types populate correctly even if schemas load after ViewModel creation.
        viewModelScope.launch {
            schemaEngine.registeredSchemas
                .map { schemas ->
                    schemas.values.map { schema ->
                        ObjectTypeItem(
                            objectType = schema.objectType,
                            displayName = schema.displayName,
                            domain = schema.domain,
                            icon = schema.icon,
                            description = schema.description,
                        )
                    }.sortedBy { it.domain }
                }
                .distinctUntilChanged()
                .collect { types -> _state.update { it.copy(availableTypes = types) } }
        }
    }

    /**
     * Scope the type picker to a single life domain. Called when the sheet is opened from a
     * Library domain section so "add to <domain>" only offers that domain's record types.
     * Passing null (or a domain with no registered types) shows every type.
     */
    fun setDomainFilter(domain: String?) {
        _state.update { it.copy(domainFilter = domain?.takeIf { d -> d.isNotBlank() }) }
    }

    fun selectType(objectType: String) {
        _state.update { it.copy(selectedType = objectType, step = CreateObjectStep.FILL_DETAILS) }
    }

    fun onTitleChange(title: String) {
        _state.update { it.copy(title = title) }
    }

    fun onDescriptionChange(description: String) {
        _state.update { it.copy(description = description) }
    }

    fun goBack() {
        _state.update { it.copy(step = CreateObjectStep.SELECT_TYPE, selectedType = null) }
    }

    fun createObject() {
        val objectType = _state.value.selectedType ?: return
        val title = _state.value.title.trim()
        if (title.isBlank()) {
            _state.update { it.copy(error = "Title is required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val profile = profileRepository.observeActiveProfile()
                    .catch { }
                    .firstOrNull()
                    ?: run {
                        _state.update { it.copy(isLoading = false, error = "No active profile") }
                        return@launch
                    }

                createObjectUseCase(
                    profileId = profile.profileId,
                    objectType = objectType,
                    title = title,
                    description = _state.value.description.takeIf { it.isNotBlank() },
                ).onSuccess { obj ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            created = true,
                            createdObjectId = obj.objectId,
                        )
                    }
                }.onFailure { e ->
                    Timber.e(e, "Failed to create object")
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error creating object")
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
