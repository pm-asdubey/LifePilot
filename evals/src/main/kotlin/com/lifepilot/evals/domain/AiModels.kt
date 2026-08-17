package com.lifepilot.evals.domain

// Local copies of domain AI models — avoids Android/Timber transitive dependency
// from :domain module in a pure JVM context.

sealed class AiCompletionResult {
    data class Success(val content: String, val model: String, val truncated: Boolean = false) : AiCompletionResult()
    data class Error(val message: String, val code: Int? = null) : AiCompletionResult()
    data object Unavailable : AiCompletionResult()
}

data class AiMessage(val role: AiMessageRole, val content: String)

enum class AiMessageRole { SYSTEM, USER, ASSISTANT }
