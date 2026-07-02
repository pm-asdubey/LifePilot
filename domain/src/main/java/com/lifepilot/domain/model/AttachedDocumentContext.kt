package com.lifepilot.domain.model

/**
 * Context for a document attached to the current AI conversation.
 *
 * This is transient state: it lives only for the current conversation turn
 * and is cleared once the user approves or dismisses the associated proposal.
 * The AI is sent only the structured [extractedFields], never the raw OCR
 * text or document bytes.
 */
data class AttachedDocumentContext(
    val fileName: String,
    val mimeType: String,
    val objectType: String? = null,
    val domain: String? = null,
    val title: String? = null,
    val extractedFields: List<ProposedField> = emptyList(),
    val isPendingApproval: Boolean = true,
)
