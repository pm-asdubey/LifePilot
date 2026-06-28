package com.lifepilot.domain.model

import java.time.Instant

data class Conversation(
    val conversationId: String,
    val profileId: String,
    val title: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class StoredMessage(
    val messageId: String,
    val conversationId: String,
    val role: String, // USER or ASSISTANT
    val content: String,
    val timestamp: Instant,
)
