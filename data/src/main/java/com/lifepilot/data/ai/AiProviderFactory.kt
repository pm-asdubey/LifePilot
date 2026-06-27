package com.lifepilot.data.ai

import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.ai.AiProvider
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiProviderFactory @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val anthropicProvider: AnthropicAiProvider,
    private val nvidiaProvider: NvidiaAiProvider,
    private val offlineProvider: OfflineAiProvider,
) {
    suspend fun getProvider(): AiProvider {
        val configuredProvider = preferenceManager.aiProvider.firstOrNull()?.lowercase()
        val apiKey = preferenceManager.aiApiKey.firstOrNull()

        if (apiKey.isNullOrBlank()) return offlineProvider

        return when (configuredProvider) {
            "anthropic" -> anthropicProvider
            "nvidia", "nvapi" -> nvidiaProvider
            else -> offlineProvider
        }
    }

    suspend fun isConfigured(): Boolean {
        val apiKey = preferenceManager.aiApiKey.firstOrNull()
        return !apiKey.isNullOrBlank()
    }
}
