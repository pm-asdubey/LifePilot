package com.lifepilot.domain.repository

import com.lifepilot.domain.model.Conversation
import com.lifepilot.domain.model.StoredMessage
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun observeConversations(profileId: String): Flow<List<Conversation>>
    suspend fun getOrCreateConversation(profileId: String, conversationId: String? = null): Conversation
    suspend fun updateTitle(conversationId: String, title: String)
    suspend fun deleteConversation(conversationId: String)
    suspend fun saveMessage(message: StoredMessage)
    suspend fun getMessages(conversationId: String): List<StoredMessage>
    fun observeMessages(conversationId: String): Flow<List<StoredMessage>>
}
