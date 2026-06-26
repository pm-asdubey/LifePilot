package com.lifepilot.domain.ai

interface AiProvider {
    val name: String
    val isAvailable: Boolean
    suspend fun complete(
        systemPrompt: String,
        userMessage: String,
        conversationHistory: List<AiMessage>,
    ): AiCompletionResult
}

data class AiMessage(
    val role: AiMessageRole,
    val content: String,
)

enum class AiMessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
}

sealed class AiCompletionResult {
    data class Success(val content: String, val model: String) : AiCompletionResult()
    data class Error(val message: String, val code: Int? = null) : AiCompletionResult()
    data object Unavailable : AiCompletionResult()
}
