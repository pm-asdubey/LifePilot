package com.lifepilot.data.di

import com.lifepilot.data.ai.AiProviderFactory
import com.lifepilot.domain.ai.AiProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideAiProvider(factory: AiProviderFactory): AiProvider {
        return object : AiProvider {
            override val name: String get() = runBlocking { factory.getProvider().name }
            override val isAvailable: Boolean get() = true
            override suspend fun complete(
                systemPrompt: String,
                userMessage: String,
                conversationHistory: List<com.lifepilot.domain.ai.AiMessage>,
            ) = factory.getProvider().complete(systemPrompt, userMessage, conversationHistory)
        }
    }
}
