package com.lifepilot.data.ai

import com.lifepilot.domain.ai.AiCompletionResult
import com.lifepilot.domain.ai.AiMessage
import com.lifepilot.domain.ai.AiProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineAiProvider @Inject constructor() : AiProvider {

    override val name: String = "offline"
    override val isAvailable: Boolean = true

    override suspend fun complete(
        systemPrompt: String,
        userMessage: String,
        conversationHistory: List<AiMessage>,
    ): AiCompletionResult {
        val lower = userMessage.lowercase()
        val context = systemPrompt

        val response = when {
            lower.contains("how many") && (lower.contains("object") || lower.contains("item")) ->
                buildObjectCountAnswer(context)
            lower.contains("expire") || lower.contains("renewal") || lower.contains("deadline") ->
                buildExpiryAnswer(context)
            lower.contains("passport") -> buildObjectAnswer(context, "passport")
            lower.contains("insurance") -> buildObjectAnswer(context, "insurance")
            lower.contains("job") || lower.contains("work") || lower.contains("employment") ->
                buildObjectAnswer(context, "job")
            lower.contains("property") || lower.contains("home") || lower.contains("house") ->
                buildObjectAnswer(context, "property")
            lower.contains("vehicle") || lower.contains("car") || lower.contains("bike") ->
                buildObjectAnswer(context, "vehicle")
            lower.contains("what") && (lower.contains("have") || lower.contains("my")) ->
                "Based on your life data:\n\n$context\n\nConfigure an AI provider in Settings for more intelligent answers."
            else ->
                "I'm running in offline mode. Configure an AI provider in Settings to get intelligent answers.\n\nYou can ask me about: objects, reminders, deadlines, and document status."
        }

        return AiCompletionResult.Success(content = response, model = "offline")
    }

    private fun buildObjectCountAnswer(context: String): String {
        val count = context.lines().count { it.trimStart().startsWith("-") }
        return if (count > 0) "You have $count objects tracked in LifePilot."
        else "No objects found. Start by adding your first object from the Library."
    }

    private fun buildExpiryAnswer(context: String): String {
        val relevant = context.lines().filter {
            it.contains("RENEWAL_DUE", ignoreCase = true) || it.contains("EXPIRED", ignoreCase = true)
        }
        return if (relevant.isEmpty()) {
            "No objects with upcoming renewals or expirations found."
        } else {
            "Objects requiring attention:\n${relevant.joinToString("\n")}"
        }
    }

    private fun buildObjectAnswer(context: String, type: String): String {
        val lines = context.lines().filter { it.lowercase().contains(type) }
        return if (lines.isEmpty()) {
            "No ${type} records found. Add one from the Library screen."
        } else {
            "Your $type record(s):\n${lines.joinToString("\n")}"
        }
    }
}
