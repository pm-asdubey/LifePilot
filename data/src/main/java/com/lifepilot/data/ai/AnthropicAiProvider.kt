package com.lifepilot.data.ai

import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.ai.AiCompletionResult
import com.lifepilot.domain.ai.AiMessage
import com.lifepilot.domain.ai.AiMessageRole
import com.lifepilot.domain.ai.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnthropicAiProvider @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val httpClient: OkHttpClient,
) : AiProvider {

    override val name: String = "anthropic"

    override val isAvailable: Boolean
        get() = true

    override suspend fun complete(
        systemPrompt: String,
        userMessage: String,
        conversationHistory: List<AiMessage>,
        readTimeoutSeconds: Long,
    ): AiCompletionResult {
        val apiKey = preferenceManager.aiApiKey.firstOrNull()
        val model = preferenceManager.aiModel.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: "claude-haiku-4-5-20251001"

        if (apiKey.isNullOrBlank()) {
            return AiCompletionResult.Unavailable
        }

        return withContext(Dispatchers.IO) {
            executeRequest(apiKey, model, systemPrompt, userMessage, conversationHistory, readTimeoutSeconds)
        }
    }

    private fun executeRequest(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userMessage: String,
        conversationHistory: List<AiMessage>,
        readTimeoutSeconds: Long,
    ): AiCompletionResult {
        return try {
            val messages = JSONArray()
            for (msg in conversationHistory) {
                if (msg.role != AiMessageRole.SYSTEM) {
                    val obj = JSONObject().apply {
                        put("role", msg.role.name.lowercase())
                        put("content", msg.content)
                    }
                    messages.put(obj)
                }
            }
            val userMsg = JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            }
            messages.put(userMsg)

            val body = JSONObject().apply {
                put("model", model)
                put("max_tokens", 1024)
                put("system", systemPrompt)
                put("messages", messages)
            }

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val client = httpClient.newBuilder()
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                return AiCompletionResult.Error(
                    message = responseBody ?: "HTTP ${response.code}",
                    code = response.code,
                )
            }

            val json = JSONObject(responseBody ?: return AiCompletionResult.Error("Empty response"))
            val content = json
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")

            AiCompletionResult.Success(content = content, model = model)
        } catch (e: Exception) {
            Timber.e(e, "Anthropic API call failed")
            AiCompletionResult.Error(message = e.message ?: "Unknown error")
        }
    }
}
