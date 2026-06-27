package com.lifepilot.features.objectdetail.verification.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.domain.engine.LifeStateEngine
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.repository.DocumentRepository
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.usecase.ExtractMetadataUseCase
import com.lifepilot.features.objectdetail.verification.state.FieldSuggestion
import com.lifepilot.features.objectdetail.verification.state.MetadataVerificationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MetadataVerificationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val objectRepository: ObjectRepository,
    private val documentRepository: DocumentRepository,
    private val metadataRepository: MetadataRepository,
    private val extractMetadataUseCase: ExtractMetadataUseCase,
    private val lifeStateEngine: LifeStateEngine,
) : ViewModel() {

    private val objectId: String = savedStateHandle["objectId"] ?: ""
    private val versionId: String = savedStateHandle["versionId"] ?: ""

    private val _state = MutableStateFlow(MetadataVerificationState(objectId = objectId))
    val state: StateFlow<MetadataVerificationState> = _state.asStateFlow()

    init {
        if (objectId.isNotBlank()) {
            extractSuggestions()
        }
    }

    private fun extractSuggestions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val obj = objectRepository.getObjectById(objectId) ?: return@launch

                _state.update { it.copy(objectTitle = obj.title, objectType = obj.objectType) }

                val ocrText = if (versionId.isNotBlank()) {
                    documentRepository.getDocumentVersionById(versionId)?.ocrText
                } else null

                if (ocrText.isNullOrBlank()) {
                    _state.update {
                        it.copy(isLoading = false, error = "No OCR text available. Document may still be processing.")
                    }
                    return@launch
                }

                extractMetadataUseCase(obj.objectType, ocrText)
                    .onSuccess { result ->
                        val suggestions = result.fields.map { field ->
                            FieldSuggestion(
                                fieldId = field.fieldId,
                                label = field.label,
                                suggestedValue = field.suggestedValue,
                                editedValue = field.suggestedValue,
                                confidence = field.confidence,
                                fieldType = field.fieldType,
                                isAccepted = true,
                            )
                        }
                        _state.update { it.copy(isLoading = false, suggestions = suggestions) }
                    }
                    .onFailure { error ->
                        Timber.e(error, "Extraction failed")
                        _state.update {
                            it.copy(isLoading = false, error = "Could not extract metadata: ${error.message}")
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Extraction error")
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onFieldValueChange(fieldId: String, newValue: String) {
        _state.update { state ->
            state.copy(
                suggestions = state.suggestions.map { suggestion ->
                    if (suggestion.fieldId == fieldId) suggestion.copy(editedValue = newValue)
                    else suggestion
                }
            )
        }
    }

    fun onAcceptanceChange(fieldId: String, accepted: Boolean) {
        _state.update { state ->
            state.copy(
                suggestions = state.suggestions.map { suggestion ->
                    if (suggestion.fieldId == fieldId) suggestion.copy(isAccepted = accepted)
                    else suggestion
                }
            )
        }
    }

    fun save() {
        viewModelScope.launch {
            val acceptedFields = _state.value.suggestions.filter { it.isAccepted && it.editedValue.isNotBlank() }
            if (acceptedFields.isEmpty()) {
                _state.update { it.copy(saved = true) }
                return@launch
            }

            _state.update { it.copy(isSaving = true) }
            try {
                val entries = acceptedFields.map { suggestion ->
                    MetadataEntry(
                        metadataId = UUID.randomUUID().toString(),
                        objectId = objectId,
                        fieldId = suggestion.fieldId,
                        value = suggestion.editedValue,
                        fieldType = suggestion.fieldType,
                        source = MetadataSource.AI_EXTRACTED,
                        confidence = suggestion.confidence,
                        version = 1,
                        updatedAt = Instant.now(),
                    )
                }
                metadataRepository.upsertMetadataBatch(entries)
                lifeStateEngine.processMetadataUpdate(objectId, entries)
                _state.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to save verified metadata")
                _state.update { it.copy(isSaving = false, error = "Failed to save: ${e.message}") }
            }
        }
    }

    fun dismiss() {
        _state.update { it.copy(saved = true) }
    }
}
