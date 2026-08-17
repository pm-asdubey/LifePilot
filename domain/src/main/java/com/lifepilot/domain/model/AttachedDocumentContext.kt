package com.lifepilot.domain.model

/**
 * Context for a document attached to the current AI conversation.
 *
 * Lives for the duration of the conversation so the user can ask follow-up
 * questions about the document. Cleared when the conversation ends or a new
 * chat starts.
 *
 * The AI receives [extractedFields] (structured) and [ocrText] (raw) so it
 * can answer freeform questions about the document content.
 */
data class AttachedDocumentContext(
    val fileName: String,
    val mimeType: String,
    val objectType: String? = null,
    val domain: String? = null,
    val title: String? = null,
    val extractedFields: List<ProposedField> = emptyList(),
    val isPendingApproval: Boolean = true,
    /** Raw OCR text — included in the AI prompt so questions about document content work. */
    val ocrText: String? = null,
)
