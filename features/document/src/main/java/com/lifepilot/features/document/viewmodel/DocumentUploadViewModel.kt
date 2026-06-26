package com.lifepilot.features.document.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.domain.usecase.UploadDocumentUseCase
import com.lifepilot.features.document.state.DocumentUploadState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DocumentUploadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uploadDocumentUseCase: UploadDocumentUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DocumentUploadState())
    val state: StateFlow<DocumentUploadState> = _state.asStateFlow()

    fun onFileSelected(uri: Uri) {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        val fileName = resolveFileName(uri)
        val suggestedTitle = fileName
            ?.substringBeforeLast(".")
            ?.replace("_", " ")
            ?.replace("-", " ")
            ?: ""

        _state.update {
            it.copy(
                selectedUri = uri,
                fileName = fileName,
                mimeType = mimeType,
                title = it.title.ifBlank { suggestedTitle },
            )
        }
    }

    fun onTitleChange(title: String) {
        _state.update { it.copy(title = title) }
    }

    fun upload(objectId: String) {
        val uri = _state.value.selectedUri ?: return
        val title = _state.value.title.trim()
        if (title.isBlank()) {
            _state.update { it.copy(error = "Title is required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                uploadDocumentUseCase(
                    objectId = objectId,
                    filePath = uri.toString(),
                    originalName = _state.value.fileName ?: title,
                    mimeType = _state.value.mimeType ?: "application/octet-stream",
                    documentType = deriveDocumentType(_state.value.mimeType),
                ).onSuccess { document ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            uploaded = true,
                            uploadedDocumentId = document.documentId,
                        )
                    }
                }.onFailure { e ->
                    Timber.e(e, "Upload failed")
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Upload error")
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun deriveDocumentType(mimeType: String?): String = when {
        mimeType == null -> "OTHER"
        mimeType.contains("pdf") -> "PDF"
        mimeType.contains("image") -> "IMAGE"
        mimeType.contains("word") || mimeType.contains("document") -> "DOCUMENT"
        mimeType.contains("spreadsheet") || mimeType.contains("excel") -> "SPREADSHEET"
        else -> "OTHER"
    }

    private fun resolveFileName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
