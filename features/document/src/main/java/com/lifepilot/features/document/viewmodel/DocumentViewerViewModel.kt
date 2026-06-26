package com.lifepilot.features.document.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.domain.model.Document
import com.lifepilot.domain.model.DocumentVersion
import com.lifepilot.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class DocumentViewerState(
    val isLoading: Boolean = true,
    val document: Document? = null,
    val currentVersion: DocumentVersion? = null,
    val ocrText: String? = null,
    val showOcrText: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DocumentViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val documentRepository: DocumentRepository,
) : ViewModel() {

    private val documentId: String = checkNotNull(savedStateHandle["documentId"])

    private val _state = MutableStateFlow(DocumentViewerState())
    val state: StateFlow<DocumentViewerState> = _state.asStateFlow()

    init {
        loadDocument()
    }

    private fun loadDocument() {
        viewModelScope.launch {
            documentRepository.observeDocumentById(documentId)
                .catch { e ->
                    Timber.e(e, "Failed to load document")
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { document ->
                    if (document == null) {
                        _state.update { it.copy(isLoading = false, error = "Document not found") }
                        return@collect
                    }
                    val currentVersion = document.versions.find { it.versionId == document.currentVersionId }
                        ?: document.versions.lastOrNull()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            document = document,
                            currentVersion = currentVersion,
                            ocrText = currentVersion?.ocrText,
                        )
                    }
                }
        }
    }

    fun toggleOcrText() {
        _state.update { it.copy(showOcrText = !it.showOcrText) }
    }
}
