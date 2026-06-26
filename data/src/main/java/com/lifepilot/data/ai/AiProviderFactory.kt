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
    private val offlineProvider: OfflineAiProvider,
) {
    suspend fun getProvider(): AiProvider {
        val configuredProvider = preferenceManager.aiProvider.firstOrNull()
        val apiKey = preferenceManager.aiApiKey.firstOrNull()

        return when {
            configuredProvider?.equals("anthropic", ignoreCase = true) == true && !apiKey.isNullOrBlank() ->
                anthropicProvider
            else -> offlineProvider
        }
    }
}
