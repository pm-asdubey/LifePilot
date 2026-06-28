package com.lifepilot.features.home.state

import com.lifepilot.domain.model.Conversation
import com.lifepilot.domain.model.Goal
import com.lifepilot.domain.model.ProposedAction
import com.lifepilot.domain.model.StoredMessage

data class HomeUiState(
    val mode: HomeMode = HomeMode.DAILY_BRIEF,
    val profileName: String = "",
    val greeting: String = "",
    val attentionItems: List<AttentionItem> = emptyList(),
    val activeGoals: List<Goal> = emptyList(),
    val recentConversations: List<Conversation> = emptyList(),
    val isLoadingBrief: Boolean = true,
    // AI Workspace state
    val currentConversationId: String? = null,
    val conversationTitle: String = "New conversation",
    val messages: List<StoredMessage> = emptyList(),
    val inputText: String = "",
    val isAiLoading: Boolean = false,
    val isAiConfigured: Boolean = false,
    val error: String? = null,
    val pendingAction: ProposedAction? = null,
    val pendingContextQuestion: String? = null,
    val showConversationHistory: Boolean = false,
    val allConversations: List<Conversation> = emptyList(),
)

enum class HomeMode { DAILY_BRIEF, AI_WORKSPACE }

data class AttentionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: AttentionType,
    val objectId: String?,
    val urgency: AttentionUrgency,
)

enum class AttentionType { EXPIRING, PENDING_VERIFICATION, GOAL, TASK, SUGGESTION }
enum class AttentionUrgency { CRITICAL, HIGH, MEDIUM, LOW }
