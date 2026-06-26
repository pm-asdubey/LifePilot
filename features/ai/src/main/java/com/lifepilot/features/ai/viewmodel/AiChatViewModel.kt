package com.lifepilot.features.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.repository.TimelineRepository
import com.lifepilot.features.ai.state.AiChatState
import com.lifepilot.features.ai.state.ChatMessage
import com.lifepilot.features.ai.state.MessageRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val objectRepository: ObjectRepository,
    private val schemaEngine: SchemaEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(AiChatState())
    val state: StateFlow<AiChatState> = _state.asStateFlow()

    init {
        _state.update { it.copy(isConfigured = false) }
    }

    fun onInputChange(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank()) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = text,
        )

        _state.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                inputText = "",
                isLoading = true,
                error = null,
            )
        }

        viewModelScope.launch {
            try {
                val context = buildLifeContext()
                val response = buildOfflineResponse(text, context)

                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.ASSISTANT,
                    content = response,
                )
                _state.update { state ->
                    state.copy(
                        messages = state.messages + assistantMessage,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "AI query failed")
                _state.update { it.copy(isLoading = false, error = "Unable to process request.") }
            }
        }
    }

    private suspend fun buildLifeContext(): String {
        val profile = profileRepository.observeActiveProfile()
            .catch { }
            .firstOrNull()
            ?: return "No profile loaded."

        val objects = objectRepository.observeObjectsByProfile(profile.profileId)
            .catch { }
            .firstOrNull()
            ?: emptyList()

        return buildString {
            appendLine("Profile: ${profile.displayName}")
            appendLine("Objects (${objects.size}):")
            objects.forEach { obj ->
                appendLine("  - ${obj.title} [${obj.objectType}] status=${obj.status}")
            }
        }
    }

    private fun buildOfflineResponse(query: String, context: String): String {
        val lower = query.lowercase()
        return when {
            lower.contains("how many") && lower.contains("object") ->
                extractObjectCountAnswer(context)
            lower.contains("passport") ->
                extractObjectAnswer(context, "passport")
            lower.contains("insurance") ->
                extractObjectAnswer(context, "insurance")
            lower.contains("job") || lower.contains("work") ->
                extractObjectAnswer(context, "job")
            lower.contains("property") || lower.contains("home") ->
                extractObjectAnswer(context, "property")
            lower.contains("vehicle") || lower.contains("car") ->
                extractObjectAnswer(context, "vehicle")
            lower.contains("what") && lower.contains("have") ->
                "Based on your life data:\n\n$context"
            else ->
                "I can help you find information about your objects, reminders, and life documents. " +
                    "To get real AI-powered answers, configure your AI provider in Settings.\n\n" +
                    "Current life data summary:\n$context"
        }
    }

    private fun extractObjectCountAnswer(context: String): String {
        val count = context.lines().count { it.trimStart().startsWith("-") }
        return "You have $count objects in your life database."
    }

    private fun extractObjectAnswer(context: String, type: String): String {
        val lines = context.lines().filter { it.lowercase().contains(type) }
        return if (lines.isEmpty()) {
            "I couldn't find any $type records in your data. You can add one from the Library screen."
        } else {
            "Here's what I found about your ${type}(s):\n${lines.joinToString("\n")}"
        }
    }

    fun clearMessages() {
        _state.update { it.copy(messages = emptyList()) }
    }
}
