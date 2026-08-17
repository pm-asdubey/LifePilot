package com.lifepilot.domain.ai

interface AiProvider {
    val name: String
    val isAvailable: Boolean
    suspend fun complete(
        systemPrompt: String,
        userMessage: String,
        conversationHistory: List<AiMessage>,
        readTimeoutSeconds: Long = 60,
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
    /**
     * @param truncated true when the model stopped because it hit the output token limit
     * (finish_reason "length" / stop_reason "max_tokens") rather than finishing naturally. The caller
     * can then request a continuation and stitch the parts together before parsing.
     */
    data class Success(val content: String, val model: String, val truncated: Boolean = false) : AiCompletionResult()
    data class Error(val message: String, val code: Int? = null) : AiCompletionResult()
    data object Unavailable : AiCompletionResult()
}
