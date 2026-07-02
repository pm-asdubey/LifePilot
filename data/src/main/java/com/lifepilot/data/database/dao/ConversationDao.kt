package com.lifepilot.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifepilot.data.database.entity.ChatMessageEntity
import com.lifepilot.data.database.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations WHERE profile_id = :profileId ORDER BY updated_at DESC")
    fun observeConversationsByProfile(profileId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE conversation_id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET updated_at = :timestamp WHERE conversation_id = :conversationId")
    suspend fun touchConversation(conversationId: String, timestamp: Long)

    @Query("DELETE FROM conversations WHERE conversation_id = :id")
    suspend fun deleteConversation(id: String)

    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY timestamp ASC")
    fun observeMessagesByConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesByConversation(conversationId: String): List<ChatMessageEntity>

    @Query(
        "SELECT DISTINCT c.conversation_id, c.profile_id, c.title, c.created_at, c.updated_at " +
            "FROM conversations c " +
            "LEFT JOIN chat_messages m ON c.conversation_id = m.conversation_id " +
            "WHERE c.profile_id = :profileId " +
            "AND (c.title LIKE '%' || :query || '%' OR m.content LIKE '%' || :query || '%') " +
            "ORDER BY c.updated_at DESC"
    )
    suspend fun searchConversations(profileId: String, query: String): List<ConversationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE conversation_id = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: String)
}
