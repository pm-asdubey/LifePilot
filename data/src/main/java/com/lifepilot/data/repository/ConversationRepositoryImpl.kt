package com.lifepilot.data.repository

import com.lifepilot.data.database.dao.ConversationDao
import com.lifepilot.data.database.entity.ChatMessageEntity
import com.lifepilot.data.database.entity.ConversationEntity
import com.lifepilot.domain.model.Conversation
import com.lifepilot.domain.model.StoredMessage
import com.lifepilot.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
) : ConversationRepository {

    override fun observeConversations(profileId: String): Flow<List<Conversation>> =
        conversationDao.observeConversationsByProfile(profileId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getOrCreateConversation(
        profileId: String,
        conversationId: String?,
    ): Conversation {
        if (conversationId != null) {
            val existing = conversationDao.getConversationById(conversationId)
            if (existing != null) return existing.toDomain()
        }
        val now = Instant.now().toEpochMilli()
        val newConversation = ConversationEntity(
            conversationId = conversationId ?: UUID.randomUUID().toString(),
            profileId = profileId,
            title = "New conversation",
            createdAt = now,
            updatedAt = now,
        )
        conversationDao.upsertConversation(newConversation)
        return newConversation.toDomain()
    }

    override suspend fun updateTitle(conversationId: String, title: String) {
        val existing = conversationDao.getConversationById(conversationId) ?: return
        conversationDao.upsertConversation(
            existing.copy(title = title, updatedAt = Instant.now().toEpochMilli())
        )
    }

    override suspend fun deleteConversation(conversationId: String) {
        conversationDao.deleteConversation(conversationId)
    }

    override suspend fun saveMessage(message: StoredMessage) {
        conversationDao.insertMessage(message.toEntity())
        // Use a targeted UPDATE (not INSERT OR REPLACE) — upsertConversation triggers
        // a CASCADE DELETE on chat_messages because SQLite's INSERT OR REPLACE deletes
        // the old row before inserting the new one, which cascades to child rows.
        conversationDao.touchConversation(message.conversationId, message.timestamp.toEpochMilli())
    }

    override suspend fun getMessages(conversationId: String): List<StoredMessage> =
        conversationDao.getMessagesByConversation(conversationId).map { it.toDomain() }

    override fun observeMessages(conversationId: String): Flow<List<StoredMessage>> =
        conversationDao.observeMessagesByConversation(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }

    private fun ConversationEntity.toDomain(): Conversation = Conversation(
        conversationId = conversationId,
        profileId = profileId,
        title = title,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

    private fun ChatMessageEntity.toDomain(): StoredMessage = StoredMessage(
        messageId = messageId,
        conversationId = conversationId,
        role = role,
        content = content,
        timestamp = Instant.ofEpochMilli(timestamp),
    )

    private fun StoredMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
        messageId = messageId,
        conversationId = conversationId,
        role = role,
        content = content,
        timestamp = timestamp.toEpochMilli(),
    )
}
