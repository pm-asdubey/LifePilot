package com.lifepilot.data.ai

import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.ai.AiCompletionResult
import com.lifepilot.domain.ai.AiMessage
import com.lifepilot.domain.ai.AiMessageRole
import com.lifepilot.domain.ai.AiProvider
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
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
    ): AiCompletionResult {
        val apiKey = preferenceManager.aiApiKey.firstOrNull()
        // Default to meta/llama-3.1-70b-instruct for high-quality responses
        val model = preferenceManager.aiModel.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: "meta/llama-3.1-70b-instruct"

        if (apiKey.isNullOrBlank()) {
            return AiCompletionResult.Unavailable
        }

        return try {
            val messages = JSONArray()

            // System message
            val sysMsg = JSONObject()
            sysMsg.put("role", "system")
            sysMsg.put("content", systemPrompt)
            messages.put(sysMsg)

            // Conversation history
            for (msg in conversationHistory) {
                if (msg.role != AiMessageRole.SYSTEM) {
                    val obj = JSONObject()
                    obj.put("role", msg.role.name.lowercase())
                    obj.put("content", msg.content)
                    messages.put(obj)
                }
            }

            // Current user message
            val userMsg = JSONObject()
            userMsg.put("role", "user")
            userMsg.put("content", userMessage)
            messages.put(userMsg)

            val body = JSONObject()
            body.put("model", model)
            body.put("messages", messages)
            body.put("max_tokens", 1024)
            body.put("temperature", 0.7)
            body.put("stream", false)

            val request = Request.Builder()
                .url("https://integrate.api.nvidia.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
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
