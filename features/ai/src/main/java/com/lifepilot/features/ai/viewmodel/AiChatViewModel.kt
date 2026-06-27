package com.lifepilot.features.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.data.ai.AiProviderFactory
import com.lifepilot.domain.ai.AiCompletionResult
import com.lifepilot.domain.ai.AiMessage
import com.lifepilot.domain.ai.AiMessageRole
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.repository.ReminderRepository
import com.lifepilot.domain.repository.TaskRepository
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
    private val metadataRepository: MetadataRepository,
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val aiProviderFactory: AiProviderFactory,
) : ViewModel() {

    private val _state = MutableStateFlow(AiChatState())
    val state: StateFlow<AiChatState> = _state.asStateFlow()

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
                val systemPrompt = buildSystemPrompt()
                val history = _state.value.messages
                    .dropLast(1)
                    .map { msg ->
                        AiMessage(
                            role = when (msg.role) {
                                MessageRole.USER -> AiMessageRole.USER
                                MessageRole.ASSISTANT -> AiMessageRole.ASSISTANT
                                MessageRole.SYSTEM -> AiMessageRole.SYSTEM
                            },
                            content = msg.content,
                        )
                    }

                val provider = aiProviderFactory.getProvider()
                val result = provider.complete(
                    systemPrompt = systemPrompt,
                    userMessage = text,
                    conversationHistory = history,
                )

                val responseContent = when (result) {
                    is AiCompletionResult.Success -> result.content
                    is AiCompletionResult.Error -> "Error: ${result.message}"
                    is AiCompletionResult.Unavailable ->
                        "AI provider not configured. Go to Settings → AI Provider to set it up."
                }

                val isConfigured = result is AiCompletionResult.Success &&
                    provider.name != "offline"

                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.ASSISTANT,
                    content = responseContent,
                )
                _state.update { state ->
                    state.copy(
                        messages = state.messages + assistantMessage,
                        isLoading = false,
                        isConfigured = isConfigured,
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "AI query failed")
                _state.update { it.copy(isLoading = false, error = "Request failed: ${e.message}") }
            }
        }
    }

    private suspend fun buildSystemPrompt(): String {
        val profile = profileRepository.observeActiveProfile()
            .catch { }
            .firstOrNull()

        val objects = profile?.let {
            objectRepository.observeObjectsByProfile(it.profileId)
                .catch { }
                .firstOrNull()
                ?: emptyList()
        } ?: emptyList()

        val pendingTasks = profile?.let {
            taskRepository.observePendingTasks(it.profileId)
                .catch { }
                .firstOrNull()
                ?: emptyList()
        } ?: emptyList()

        val upcomingReminders = runCatching {
            reminderRepository.observeUpcomingReminders(
                java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS)
            ).catch { }.firstOrNull() ?: emptyList()
        }.getOrElse { emptyList() }

        return buildString {
            appendLine("You are the LifePilot AI assistant. You help users manage their administrative life.")
            appendLine("You have access to the user's structured life data below. Answer questions based ONLY on this data.")
            appendLine("Be concise, practical, and focused on actionable insights.")
            appendLine("Today's date: ${java.time.LocalDate.now()}")
            appendLine()
            appendLine("USER LIFE DATA:")
            if (profile != null) {
                appendLine("Profile: ${profile.displayName}")
            }
            appendLine()
            appendLine("Objects (${objects.size} total):")
            objects.forEach { obj ->
                appendLine("  - ${obj.title} [type=${obj.objectType}, domain=${obj.domain}, status=${obj.status}]")
                val metadata = runCatching {
                    metadataRepository.getMetadataByObject(obj.objectId)
                }.getOrElse { emptyList() }
                if (metadata.isNotEmpty()) {
                    metadata.take(5).forEach { entry ->
                        appendLine("    ${entry.fieldId}: ${entry.value}")
                    }
                }
            }
            if (objects.isEmpty()) {
                appendLine("  (No objects yet. Ask the user to add their first object.)")
            }
            appendLine()
            appendLine("Pending tasks (${pendingTasks.size}):")
            pendingTasks.take(10).forEach { task ->
                val due = task.dueDate?.toString() ?: "no due date"
                appendLine("  - ${task.title} [priority=${task.priority}, due=$due]")
            }
            appendLine()
            appendLine("Upcoming reminders (next 30 days, ${upcomingReminders.size}):")
            upcomingReminders.take(10).forEach { reminder ->
                appendLine("  - ${reminder.title} [due=${reminder.triggerDate}, priority=${reminder.priority}]")
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(messages = emptyList()) }
    }
}
