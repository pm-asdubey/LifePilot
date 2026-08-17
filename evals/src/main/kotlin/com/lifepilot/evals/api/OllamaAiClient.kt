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

/**
 * Hits a local Ollama server at localhost:11434.
 * Ollama exposes an OpenAI-compatible /v1/chat/completions endpoint.
 *
 * Start Ollama before running evals: `ollama serve`
 * Pull a model first: `ollama pull gemma2:2b`
 */
class OllamaAiClient(
    private val model: String = "gemma2:2b",
    private val baseUrl: String = "http://localhost:11434",
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        // Local inference on CPU is slow — generous timeout
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    fun complete(
        systemPrompt: String,
        userMessage: String,
        history: List<AiMessage> = emptyList(),
    ): AiCompletionResult {
        return try {
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
                put("model", model)
                put("messages", messages)
                put("stream", false)
                // Keep responses focused — small models hallucinate more with high max_tokens
                put("max_tokens", 2048)
                put("options", JSONObject().apply {
                    put("temperature", 0.1)   // low temp for structured JSON output
                })
            }

            val request = Request.Builder()
                .url("$baseUrl/v1/chat/completions")
                .addHeader("content-type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                return AiCompletionResult.Error(
                    message = responseBody ?: "HTTP ${response.code} — is Ollama running? Try: ollama serve",
                    code = response.code,
                )
            }

            val json = JSONObject(responseBody ?: return AiCompletionResult.Error("Empty response"))
            val content = json
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            val finishReason = json
                .getJSONArray("choices")
                .getJSONObject(0)
                .optString("finish_reason", "stop")

            AiCompletionResult.Success(
                content = content,
                model = model,
                truncated = finishReason == "length",
            )
        } catch (e: java.net.ConnectException) {
            AiCompletionResult.Error("Cannot connect to Ollama. Run: ollama serve")
        } catch (e: Exception) {
            AiCompletionResult.Error(e.message ?: "Unknown error")
        }
    }

    fun isAvailable(): Boolean {
        return try {
            val request = Request.Builder().url("$baseUrl/api/tags").get().build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    fun listModels(): List<String> {
        return try {
            val request = Request.Builder().url("$baseUrl/api/tags").get().build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: return emptyList())
            val models = json.getJSONArray("models")
            (0 until models.length()).map { models.getJSONObject(it).getString("name") }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
