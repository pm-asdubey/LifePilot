package com.lifepilot.features.ai.state

import com.lifepilot.domain.model.AiProposal

data class AiChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConfigured: Boolean = false,
    val pendingAction: AiProposal? = null,
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
