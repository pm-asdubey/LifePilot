package com.lifepilot.features.object.metadata.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.MetadataFieldType
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.features.object.metadata.state.MetadataEditState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MetadataEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val objectRepository: ObjectRepository,
    private val metadataRepository: MetadataRepository,
    private val schemaEngine: SchemaEngine,
) : ViewModel() {

    private val objectId: String = checkNotNull(savedStateHandle["objectId"])

    private val _state = MutableStateFlow(MetadataEditState(objectId = objectId))
    val state: StateFlow<MetadataEditState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        _state.update { it.copy(isLoading = true) }
        combine(
            objectRepository.observeObjectById(objectId),
            metadataRepository.observeMetadataByObject(objectId),
        ) { obj, metadata ->
            if (obj == null) {
                _state.update { it.copy(isLoading = false) }
                return@combine
            }
            val schema = schemaEngine.getSchema(obj.objectType)
            val fields = schema?.fields ?: emptyList()
            val existingValues = metadata.associate { it.fieldId to it.value }
            _state.update {
                it.copy(
                    isLoading = false,
                    objectType = obj.objectType,
                    fields = fields,
                    values = existingValues,
                )
            }
        }
            .catch { e -> Timber.e(e, "Error loading metadata edit form") }
            .launchIn(viewModelScope)
    }

    fun onFieldValueChange(fieldId: String, value: String) {
        _state.update { state ->
            state.copy(
                values = state.values + (fieldId to value),
                errors = state.errors - fieldId,
            )
        }
    }

    fun save() {
        val state = _state.value
        val validationErrors = mutableMapOf<String, String>()

        for (field in state.fields) {
            if (field.required) {
                val value = state.values[field.fieldId]
                if (value.isNullOrBlank()) {
                    validationErrors[field.fieldId] = "${field.displayName} is required"
                }
            }
            val value = state.values[field.fieldId]
            if (!value.isNullOrBlank()) {
                val result = schemaEngine.validateMetadataValue(
                    state.objectType,
                    field.fieldId,
                    value,
                )
                if (!result.isValid) {
                    validationErrors[field.fieldId] = result.errors.firstOrNull() ?: "Invalid"
                }
            }
        }

        if (validationErrors.isNotEmpty()) {
            _state.update { it.copy(errors = validationErrors) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                val entries = state.values.entries
                    .filter { it.value.isNotBlank() }
                    .map { (fieldId, value) ->
                        MetadataEntry(
                            metadataId = UUID.randomUUID().toString(),
                            objectId = objectId,
                            fieldId = fieldId,
                            value = value,
                            fieldType = getFieldType(fieldId, state),
                            source = MetadataSource.USER,
                            confidence = null,
                            version = 1,
                            updatedAt = Instant.now(),
                        )
                    }

                metadataRepository.upsertMetadataBatch(entries)
                _state.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to save metadata")
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    private fun getFieldType(fieldId: String, state: MetadataEditState): MetadataFieldType {
        val field = state.fields.find { it.fieldId == fieldId } ?: return MetadataFieldType.TEXT
        return try {
            MetadataFieldType.valueOf(field.fieldType)
        } catch (_: Exception) {
            MetadataFieldType.TEXT
        }
    }
}
