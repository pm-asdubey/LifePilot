package com.lifepilot.evals.api

import com.lifepilot.evals.domain.AiCompletionResult
import com.lifepilot.evals.domain.AiMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class EvalAiClient(
    private val apiKey: String,
    private val model: String = "claude-haiku-4-5-20251001",
    private val judgeModel: String = model,
    private val baseUrl: String = "https://api.anthropic.com",
    private val useOpenAiFormat: Boolean = false,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun complete(
        systemPrompt: String,
        userMessage: String,
        history: List<AiMessage> = emptyList(),
    ): AiCompletionResult = if (useOpenAiFormat) {
        completeOpenAi(model, systemPrompt, userMessage, history)
    } else {
        completeAnthropic(model, systemPrompt, userMessage, history)
    }

    fun judge(rubric: String, response: String): Int {
        val systemPrompt = "You are an eval judge. Score the following response on a scale of 1-5.\n\nRubric: $rubric\n\nReply with ONLY a single integer between 1 and 5. No explanation."
        val result = if (useOpenAiFormat) {
            completeOpenAi(judgeModel, systemPrompt, "Response to score:\n\n$response", emptyList())
        } else {
            completeAnthropic(judgeModel, systemPrompt, "Response to score:\n\n$response", emptyList())
        }
        return when (result) {
            is AiCompletionResult.Success -> result.content.trim().toIntOrNull()?.coerceIn(1, 5) ?: 1
            else -> 1
        }
    }

    private fun completeAnthropic(
        modelId: String,
        systemPrompt: String,
        userMessage: String,
        history: List<AiMessage>,
    ): AiCompletionResult = try {
        val messages = JSONArray()
        for (msg in history) {
            messages.put(JSONObject().apply {
                put("role", msg.role.name.lowercase())
                put("content", msg.content)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", userMessage)
        })

        val body = JSONObject().apply {
            put("model", modelId)
            put("max_tokens", 4096)
            put("system", systemPrompt)
            put("messages", messages)
        }

        val request = Request.Builder()
            .url("$baseUrl/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        if (!response.isSuccessful) return AiCompletionResult.Error(responseBody ?: "HTTP ${response.code}", response.code)

        val json = JSONObject(responseBody ?: return AiCompletionResult.Error("Empty response"))
        val content = json.getJSONArray("content").getJSONObject(0).getString("text")
        val truncated = json.optString("stop_reason") == "max_tokens"
        AiCompletionResult.Success(content = content, model = modelId, truncated = truncated)
    } catch (e: Exception) {
        AiCompletionResult.Error(e.message ?: "Unknown error")
    }

    private fun completeOpenAi(
        modelId: String,
        systemPrompt: String,
        userMessage: String,
        history: List<AiMessage>,
    ): AiCompletionResult = try {
        val messages = JSONArray()
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })
        for (msg in history) {
            messages.put(JSONObject().apply {
                put("role", msg.role.name.lowercase())
                put("content", msg.content)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", userMessage)
        })

        val body = JSONObject().apply {
            put("model", modelId)
            put("max_tokens", 4096)
            put("messages", messages)
        }

        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        if (!response.isSuccessful) return AiCompletionResult.Error(responseBody ?: "HTTP ${response.code}", response.code)

        val json = JSONObject(responseBody ?: return AiCompletionResult.Error("Empty response"))
        val content = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        val truncated = json.getJSONArray("choices").getJSONObject(0).optString("finish_reason") == "length"
        AiCompletionResult.Success(content = content, model = modelId, truncated = truncated)
    } catch (e: Exception) {
        AiCompletionResult.Error(e.message ?: "Unknown error")
    }
}
