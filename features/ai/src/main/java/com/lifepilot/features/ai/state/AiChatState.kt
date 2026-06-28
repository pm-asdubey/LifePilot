package com.lifepilot.features.ai.state

data class AiChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConfigured: Boolean = false,
    val pendingAction: ProposedAction? = null,
    val pendingContextQuestion: String? = null,
)

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
}

data class ProposedAction(
    val id: String,
    val objectId: String,
    val objectTitle: String,
    val objectType: String,
    val summary: String,
    val fields: List<ProposedField>,
)

data class ProposedField(
    val fieldId: String,
    val displayName: String,
    val value: String,
    val mode: UpdateMode = UpdateMode.SET,
)

enum class UpdateMode { SET, APPEND }
