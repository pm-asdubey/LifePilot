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
class NvidiaAiProvider @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val httpClient: OkHttpClient,
) : AiProvider {

    override val name: String = "nvidia"

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
            ?: "meta/llama-3.1-70b-instruct"

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

            val sysMsg = JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            }
            messages.put(sysMsg)

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
                put("messages", messages)
                put("max_tokens", 1024)
                put("temperature", 0.7)
                put("stream", false)
            }

            val request = Request.Builder()
                .url("https://integrate.api.nvidia.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
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
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            AiCompletionResult.Success(content = content.trim(), model = model)
        } catch (e: Exception) {
            Timber.e(e, "NVIDIA NIM API call failed")
            AiCompletionResult.Error(message = e.message ?: "Unknown error")
        }
    }
}
