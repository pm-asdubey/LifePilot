package com.lifepilot.features.document.state

import android.net.Uri

data class DocumentUploadState(
    val isLoading: Boolean = false,
    val selectedUri: Uri? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val title: String = "",
    val error: String? = null,
    val uploaded: Boolean = false,
    val uploadedDocumentId: String? = null,
)
