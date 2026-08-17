package com.lifepilot.evals.api

import com.lifepilot.evals.domain.AiCompletionResult
import com.lifepilot.evals.domain.AiMessage

/**
 * Unified interface so EvalRunner and AssertionRunner don't care
 * whether they're talking to Anthropic, NVIDIA, or local Ollama.
 */
interface AiClientAdapter {
    fun complete(
        systemPrompt: String,
        userMessage: String,
        history: List<AiMessage> = emptyList(),
    ): AiCompletionResult

    fun judge(rubric: String, response: String): Int
}

class AnthropicAdapter(private val client: EvalAiClient) : AiClientAdapter {
    override fun complete(systemPrompt: String, userMessage: String, history: List<AiMessage>) =
        client.complete(systemPrompt, userMessage, history)

    override fun judge(rubric: String, response: String) =
        client.judge(rubric, response)
}

class OllamaAdapter(
    private val client: OllamaAiClient,
    private val judgeClient: EvalAiClient? = null,
) : AiClientAdapter {
    override fun complete(systemPrompt: String, userMessage: String, history: List<AiMessage>) =
        client.complete(systemPrompt, userMessage, history)

    override fun judge(rubric: String, response: String): Int {
        // If a cloud judge client is provided use it — more reliable than local model judging itself.
        // If not, fall back to local model as judge.
        return judgeClient?.judge(rubric, response) ?: run {
            val systemPrompt = "You are an eval judge. Score the following response on a scale of 1-5.\n\nRubric: $rubric\n\nReply with ONLY a single integer between 1 and 5. No explanation."
            val result = client.complete(systemPrompt, "Response to score:\n\n$response")
            when (result) {
                is AiCompletionResult.Success -> result.content.trim().toIntOrNull()?.coerceIn(1, 5) ?: 1
                else -> 1
            }
        }
    }
}
