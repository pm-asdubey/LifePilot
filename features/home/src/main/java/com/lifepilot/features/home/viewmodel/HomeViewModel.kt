package com.lifepilot.features.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.data.ai.AiProviderFactory
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.ai.AiCompletionResult
import com.lifepilot.domain.ai.AiMessage
import com.lifepilot.domain.ai.AiMessageRole
import com.lifepilot.domain.engine.AttentionPriority
import com.lifepilot.domain.engine.LifeStateEngine
import com.lifepilot.domain.engine.PlanningEngine
import com.lifepilot.domain.engine.PromptBuilder
import com.lifepilot.domain.engine.RetrievalEngine
import com.lifepilot.domain.model.AiProposal
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.model.ProposedField
import com.lifepilot.domain.model.StoredMessage
import com.lifepilot.domain.model.TaskPriority
import com.lifepilot.domain.model.TaskSource
import com.lifepilot.domain.model.UpdateMode
import com.lifepilot.domain.repository.ConversationRepository
import com.lifepilot.domain.repository.GoalRepository
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.features.home.state.AttentionItem
import com.lifepilot.features.home.state.AttentionType
import com.lifepilot.features.home.state.AttentionUrgency
import com.lifepilot.features.home.state.HomeMode
import com.lifepilot.features.home.state.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.time.Instant
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val goalRepository: GoalRepository,
    private val conversationRepository: ConversationRepository,
    private val objectRepository: ObjectRepository,
    private val metadataRepository: MetadataRepository,
    private val planningEngine: PlanningEngine,
    private val lifeStateEngine: LifeStateEngine,
    private val retrievalEngine: RetrievalEngine,
    private val promptBuilder: PromptBuilder,
    private val aiProviderFactory: AiProviderFactory,
    private val preferenceManager: PreferenceManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Kept current after each AI request for post-response parseAction() resolution.
    // RetrievalEngine populates these; HomeViewModel reads them — never writes them.
    private var currentObjectIndex: Map<String, Pair<String, String>> = emptyMap()
    private var currentObjectMetadataIndex: Map<String, List<com.lifepilot.domain.model.MetadataEntry>> = emptyMap()

    init {
        _uiState.update { it.copy(greeting = timeBasedGreeting()) }
        checkAiConfigured()
        observeBriefData()
    }

    private fun checkAiConfigured() {
        viewModelScope.launch {
            val configured = aiProviderFactory.isConfigured()
            _uiState.update { it.copy(isAiConfigured = configured) }
        }
    }

    private fun observeBriefData() {
        viewModelScope.launch {
            profileRepository.observeActiveProfile()
                .catch { e -> Timber.e(e, "Error observing profile") }
                .flatMapLatest { profile ->
                    if (profile == null) return@flatMapLatest flowOf(null)
                    combine(
                        goalRepository.observeActiveGoals(profile.profileId)
                            .catch { e -> Timber.e(e, "Error observing goals"); emit(emptyList()) },
                        conversationRepository.observeConversations(profile.profileId)
                            .catch { e -> Timber.e(e, "Error observing conversations"); emit(emptyList()) },
                        lifeStateEngine.observeAttentionRequired(profile.profileId)
                            .catch { e -> Timber.e(e, "Error observing attention"); emit(emptyList()) },
                    ) { goals, conversations, attentionItems ->
                        BriefData(profile.displayName, profile.profileId, goals, conversations, attentionItems)
                    }
                }
                .collect { data ->
                    if (data == null) {
                        _uiState.update { it.copy(isLoadingBrief = false) }
                        return@collect
                    }
                    val mapped = data.attentionItems.take(5).map { item ->
                        AttentionItem(
                            id = item.itemId,
                            title = item.title,
                            subtitle = item.description,
                            type = mapActionType(item.actionType),
                            objectId = item.objectId,
                            urgency = when (item.priority) {
                                AttentionPriority.CRITICAL -> AttentionUrgency.CRITICAL
                                AttentionPriority.HIGH -> AttentionUrgency.HIGH
                                AttentionPriority.MEDIUM -> AttentionUrgency.MEDIUM
                                AttentionPriority.LOW -> AttentionUrgency.LOW
                            },
                        )
                    }
                    _uiState.update { state ->
                        state.copy(
                            profileName = data.profileName,
                            isLoadingBrief = false,
                            activeGoals = data.goals,
                            recentConversations = data.conversations.take(3),
                            allConversations = data.conversations,
                            attentionItems = mapped,
                        )
                    }
                }
        }
    }

    private data class BriefData(
        val profileName: String,
        val profileId: String,
        val goals: List<com.lifepilot.domain.model.Goal>,
        val conversations: List<com.lifepilot.domain.model.Conversation>,
        val attentionItems: List<com.lifepilot.domain.engine.AttentionItem>,
    )

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        if (_uiState.value.mode == HomeMode.DAILY_BRIEF) {
            _uiState.update { it.copy(mode = HomeMode.AI_WORKSPACE) }
        }

        val userMessageId = UUID.randomUUID().toString()
        val nowInstant = Instant.now()

        _uiState.update { s ->
            s.copy(
                inputText = "",
                isAiLoading = true,
                error = null,
                pendingAction = null,
                pendingContextQuestion = null,
                messages = s.messages + StoredMessage(
                    messageId = userMessageId,
                    conversationId = s.currentConversationId ?: "",
                    role = "USER",
                    content = text,
                    timestamp = nowInstant,
                ),
            )
        }

        viewModelScope.launch {
            try {
                val profileId = preferenceManager.getActiveProfileId() ?: run {
                    _uiState.update { it.copy(isAiLoading = false, error = "No active profile") }
                    return@launch
                }

                val conversation = conversationRepository.getOrCreateConversation(
                    profileId = profileId,
                    conversationId = _uiState.value.currentConversationId,
                )

                val userMsg = StoredMessage(
                    messageId = userMessageId,
                    conversationId = conversation.conversationId,
                    role = "USER",
                    content = text,
                    timestamp = nowInstant,
                )

                _uiState.update { s ->
                    val fixed = s.messages.map { if (it.messageId == userMessageId) userMsg else it }
                    s.copy(
                        currentConversationId = conversation.conversationId,
                        conversationTitle = conversation.title,
                        messages = fixed,
                    )
                }

                conversationRepository.saveMessage(userMsg)

                if (conversation.title == "New conversation") {
                    val autoTitle = text.take(50).trimEnd()
                    conversationRepository.updateTitle(conversation.conversationId, autoTitle)
                    _uiState.update { it.copy(conversationTitle = autoTitle) }
                }

                // RetrievalEngine scores and selects context; PromptBuilder formats it.
                // HomeViewModel never decides what to retrieve or how to format.
                val retrievalContext = retrievalEngine.retrieve(profileId, userQuery = text)
                currentObjectIndex = retrievalContext.allObjectIndex
                currentObjectMetadataIndex = retrievalContext.allObjectMetadata

                val systemPrompt = promptBuilder.build(retrievalContext, userQuery = text)

                val history = _uiState.value.messages
                    .dropLast(1)
                    .filter { it.conversationId == conversation.conversationId }
                    .map { msg ->
                        AiMessage(
                            role = if (msg.role == "USER") AiMessageRole.USER else AiMessageRole.ASSISTANT,
                            content = msg.content,
                        )
                    }

                val provider = aiProviderFactory.getProvider()
                val result = provider.complete(
                    systemPrompt = systemPrompt,
                    userMessage = text,
                    conversationHistory = history,
                )

                when (result) {
                    is AiCompletionResult.Success -> {
                        val (visibleContent, action, question) = parseAiResponse(result.content)
                        val assistantMsg = StoredMessage(
                            messageId = UUID.randomUUID().toString(),
                            conversationId = conversation.conversationId,
                            role = "ASSISTANT",
                            content = visibleContent,
                            timestamp = Instant.now(),
                        )
                        conversationRepository.saveMessage(assistantMsg)
                        _uiState.update { s ->
                            s.copy(
                                messages = s.messages + assistantMsg,
                                isAiLoading = false,
                                isAiConfigured = true,
                                pendingAction = action,
                                pendingContextQuestion = question,
                            )
                        }
                    }
                    is AiCompletionResult.Error -> {
                        val errorMsg = StoredMessage(
                            messageId = UUID.randomUUID().toString(),
                            conversationId = conversation.conversationId,
                            role = "ASSISTANT",
                            content = "Error: ${result.message}",
                            timestamp = Instant.now(),
                        )
                        _uiState.update { s ->
                            s.copy(messages = s.messages + errorMsg, isAiLoading = false)
                        }
                    }
                    is AiCompletionResult.Unavailable -> {
                        val unavailableMsg = StoredMessage(
                            messageId = UUID.randomUUID().toString(),
                            conversationId = conversation.conversationId,
                            role = "ASSISTANT",
                            content = "AI provider not configured. Go to Settings → AI Provider to set it up.",
                            timestamp = Instant.now(),
                        )
                        _uiState.update { s ->
                            s.copy(
                                messages = s.messages + unavailableMsg,
                                isAiLoading = false,
                                isAiConfigured = false,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "AI query failed")
                _uiState.update { it.copy(isAiLoading = false, error = "Request failed: ${e.message}") }
            }
        }
    }

    fun approveAction() {
        val proposal = _uiState.value.pendingAction ?: return
        _uiState.update { it.copy(pendingAction = null) }
        viewModelScope.launch {
            try {
                val confirmText = executeProposal(proposal)
                val convId = _uiState.value.currentConversationId ?: return@launch
                val confirmMsg = StoredMessage(
                    messageId = UUID.randomUUID().toString(),
                    conversationId = convId,
                    role = "ASSISTANT",
                    content = confirmText,
                    timestamp = Instant.now(),
                )
                conversationRepository.saveMessage(confirmMsg)
                _uiState.update { s -> s.copy(messages = s.messages + confirmMsg) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to execute AI proposal")
            }
        }
    }

    private suspend fun executeProposal(proposal: AiProposal): String {
        val profileId = preferenceManager.getActiveProfileId() ?: error("No active profile")
        return when (proposal) {
            is AiProposal.MetadataUpdate -> {
                for (field in proposal.fields) {
                    val existing = if (field.mode == UpdateMode.APPEND) {
                        metadataRepository.getMetadataByField(proposal.objectId, field.fieldId)?.value
                    } else null
                    val finalValue = if (existing != null && field.mode == UpdateMode.APPEND) {
                        "$existing\n${field.value}"
                    } else {
                        field.value
                    }
                    metadataRepository.upsertMetadata(
                        objectId = proposal.objectId,
                        fieldId = field.fieldId,
                        value = finalValue,
                        source = MetadataSource.AI_EXTRACTED,
                        confidence = 0.9f,
                    )
                }
                "Saved to ${proposal.objectTitle}."
            }
            is AiProposal.GoalProposal -> {
                planningEngine.createGoal(
                    profileId = profileId,
                    title = proposal.title,
                    description = proposal.description,
                    deadline = proposal.deadline,
                    linkedObjectId = proposal.linkedObjectId,
                    suggestedTaskTitles = proposal.suggestedTasks,
                ).getOrThrow()
                "Goal \"${proposal.title}\" added to your Planner."
            }
            is AiProposal.TaskCreation -> {
                planningEngine.createTask(
                    profileId = profileId,
                    title = proposal.title,
                    description = proposal.description,
                    dueDate = proposal.dueDate,
                    goalId = proposal.goalId,
                    objectId = proposal.objectId,
                    source = TaskSource.AI_PROPOSED,
                    priority = TaskPriority.MEDIUM,
                ).getOrThrow()
                "Task \"${proposal.title}\" added to your Planner."
            }
            is AiProposal.TaskCompletion -> {
                planningEngine.completeTask(proposal.taskId).getOrThrow()
                "Marked \"${proposal.taskTitle}\" as complete."
            }
            is AiProposal.ObjectCreation -> {
                val createdObject = objectRepository.createObject(
                    profileId = profileId,
                    objectType = proposal.objectType,
                    domain = proposal.domain,
                    title = proposal.title,
                    description = proposal.initialNotes,
                )
                val notes = proposal.initialNotes
                if (!notes.isNullOrBlank()) {
                    metadataRepository.upsertMetadata(
                        objectId = createdObject.objectId,
                        fieldId = "notes",
                        value = notes,
                        source = MetadataSource.AI_EXTRACTED,
                        confidence = 0.8f,
                    )
                }
                "\"${proposal.title}\" added to your records."
            }
        }
    }

    fun dismissAction() {
        _uiState.update { it.copy(pendingAction = null) }
    }

    fun dismissContextQuestion() {
        _uiState.update { it.copy(pendingContextQuestion = null) }
    }

    fun startNewChat() {
        _uiState.update {
            it.copy(
                mode = HomeMode.DAILY_BRIEF,
                currentConversationId = null,
                conversationTitle = "New conversation",
                messages = emptyList(),
                inputText = "",
                isAiLoading = false,
                pendingAction = null,
                pendingContextQuestion = null,
                error = null,
            )
        }
    }

    fun returnToBrief() {
        _uiState.update {
            it.copy(
                mode = HomeMode.DAILY_BRIEF,
                isAiLoading = false,
                pendingAction = null,
                pendingContextQuestion = null,
                error = null,
            )
        }
    }

    fun resumeConversation(conversationId: String) {
        _uiState.update { it.copy(showConversationHistory = false) }
        viewModelScope.launch {
            try {
                val messages = conversationRepository.getMessages(conversationId)
                val conv = _uiState.value.allConversations.firstOrNull { it.conversationId == conversationId }
                _uiState.update { state ->
                    state.copy(
                        mode = HomeMode.AI_WORKSPACE,
                        currentConversationId = conversationId,
                        conversationTitle = conv?.title ?: "Conversation",
                        messages = messages,
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to resume conversation: $conversationId")
            }
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            runCatching { conversationRepository.deleteConversation(conversationId) }
                .onFailure { e -> Timber.e(e, "Failed to delete conversation: $conversationId") }
            if (_uiState.value.currentConversationId == conversationId) startNewChat()
        }
    }

    fun showConversationHistory() {
        _uiState.update { it.copy(showConversationHistory = true) }
    }

    fun hideConversationHistory() {
        _uiState.update { it.copy(showConversationHistory = false) }
    }

    fun updateConversationTitle(title: String) {
        val convId = _uiState.value.currentConversationId ?: return
        _uiState.update { it.copy(conversationTitle = title) }
        viewModelScope.launch {
            runCatching { conversationRepository.updateTitle(convId, title) }
                .onFailure { e -> Timber.e(e, "Failed to update title") }
        }
    }

    // ── AI response parsing ───────────────────────────────────────────────────

    private data class ParsedResponse(
        val visibleContent: String,
        val action: AiProposal?,
        val question: String?,
    )

    private fun parseAiResponse(raw: String): ParsedResponse {
        var content = raw
        var action: AiProposal? = null
        var question: String? = null

        val actionMatch = ACTION_PATTERN.find(raw)
        if (actionMatch != null) {
            content = content.replace(actionMatch.value, "").trim()
            action = runCatching { parseAction(actionMatch.groupValues[1].trim()) }.getOrNull()
        }

        val askMatch = ASK_PATTERN.find(raw)
        if (askMatch != null) {
            content = content.replace(askMatch.value, "").trim()
            question = askMatch.groupValues[1].trim()
        }

        return ParsedResponse(content.trim(), action, question)
    }

    private fun parseAction(json: String): AiProposal? {
        return try {
            val obj = JSONObject(json)
            val actionType = obj.optString("actionType", "METADATA_UPDATE")
            val summary = obj.optString("summary", "")

            when (actionType.uppercase()) {
                "GOAL_PROPOSAL" -> {
                    val tasksArray = obj.optJSONArray("suggestedTasks")
                    val tasks = buildList {
                        if (tasksArray != null) for (i in 0 until tasksArray.length()) add(tasksArray.getString(i))
                    }
                    val deadlineStr = obj.optString("deadline", "").takeIf { it.isNotBlank() && it != "null" }
                    AiProposal.GoalProposal(
                        proposalId = UUID.randomUUID().toString(),
                        summary = summary,
                        title = obj.optString("title", "New goal"),
                        description = obj.optString("description", "").takeIf { it.isNotBlank() && it != "null" },
                        deadline = deadlineStr?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                        estimatedWeeks = obj.optInt("estimatedWeeks", 0).takeIf { it > 0 },
                        suggestedTasks = tasks,
                        linkedObjectId = obj.optString("linkedObjectId", "").takeIf { it.isNotBlank() && it != "null" },
                    )
                }
                "TASK_CREATION" -> {
                    val dueDateStr = obj.optString("dueDate", "").takeIf { it.isNotBlank() && it != "null" }
                    AiProposal.TaskCreation(
                        proposalId = UUID.randomUUID().toString(),
                        summary = summary,
                        title = obj.optString("title", "New task"),
                        description = obj.optString("description", "").takeIf { it.isNotBlank() && it != "null" },
                        dueDate = dueDateStr?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                        goalId = obj.optString("goalId", "").takeIf { it.isNotBlank() && it != "null" },
                        objectId = obj.optString("objectId", "").takeIf { it.isNotBlank() && it != "null" },
                    )
                }
                "TASK_COMPLETION" -> {
                    AiProposal.TaskCompletion(
                        proposalId = UUID.randomUUID().toString(),
                        summary = summary,
                        taskId = obj.optString("taskId", ""),
                        taskTitle = obj.optString("taskTitle", "task"),
                        goalId = obj.optString("goalId", "").takeIf { it.isNotBlank() && it != "null" },
                    )
                }
                "OBJECT_CREATION" -> {
                    AiProposal.ObjectCreation(
                        proposalId = UUID.randomUUID().toString(),
                        summary = summary,
                        objectType = obj.optString("objectType", "Record"),
                        domain = obj.optString("domain", "General"),
                        title = obj.optString("title", "New record"),
                        initialNotes = run {
                            val fields = obj.optJSONArray("fields")
                            if (fields != null && fields.length() > 0) {
                                fields.getJSONObject(0).optString("value", "").takeIf { it.isNotBlank() }
                            } else null
                        },
                    )
                }
                else -> parseMetadataUpdate(obj, summary)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse LIFEPILOT_ACTION block")
            null
        }
    }

    private fun parseMetadataUpdate(obj: JSONObject, summary: String): AiProposal.MetadataUpdate? {
        val objectType = obj.optString("objectType", "")
        val matchField = obj.optString("matchField", "")
        val matchValue = obj.optString("matchValue", "")

        val candidateIds = currentObjectIndex.entries
            .filter { (_, v) -> v.second.equals(objectType, ignoreCase = true) }
            .map { it.key }

        val resolvedObjectId = when {
            candidateIds.isEmpty() -> return null
            candidateIds.size == 1 -> candidateIds.first()
            matchField.isNotBlank() && matchValue.isNotBlank() -> {
                candidateIds.firstOrNull { id ->
                    val title = currentObjectIndex[id]?.first ?: ""
                    title.contains(matchValue, ignoreCase = true) ||
                        currentObjectMetadataIndex[id]?.any { entry ->
                            entry.fieldId.equals(matchField, ignoreCase = true) &&
                                entry.value.contains(matchValue, ignoreCase = true)
                        } == true
                } ?: candidateIds.first()
            }
            else -> candidateIds.first()
        }

        val resolvedTitle = currentObjectIndex[resolvedObjectId]?.first ?: objectType
        val fieldsArray = obj.optJSONArray("fields") ?: return null
        val fields = mutableListOf<ProposedField>()
        for (i in 0 until fieldsArray.length()) {
            val fieldObj = fieldsArray.getJSONObject(i)
            fields.add(
                ProposedField(
                    fieldId = fieldObj.getString("fieldId"),
                    displayName = fieldObj.optString("displayName", fieldObj.getString("fieldId")),
                    value = fieldObj.getString("value"),
                    mode = if (fieldObj.optString("mode") == "append") UpdateMode.APPEND else UpdateMode.SET,
                )
            )
        }
        return AiProposal.MetadataUpdate(
            proposalId = UUID.randomUUID().toString(),
            objectId = resolvedObjectId,
            objectTitle = resolvedTitle,
            objectType = objectType,
            summary = summary.ifBlank { "Update suggested" },
            fields = fields,
        )
    }

    private fun mapActionType(actionType: String): AttentionType = when {
        actionType.contains("expir", ignoreCase = true) -> AttentionType.EXPIRING
        actionType.contains("verif", ignoreCase = true) -> AttentionType.PENDING_VERIFICATION
        actionType.contains("goal", ignoreCase = true) -> AttentionType.GOAL
        actionType.contains("task", ignoreCase = true) -> AttentionType.TASK
        else -> AttentionType.SUGGESTION
    }

    private fun timeBasedGreeting(): String = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }

    companion object {
        private val ACTION_PATTERN = Regex(
            """\[LIFEPILOT_ACTION\](.*?)\[/LIFEPILOT_ACTION\]""",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val ASK_PATTERN = Regex(
            """\[ASK\](.*?)\[/ASK\]""",
            RegexOption.DOT_MATCHES_ALL,
        )
    }
}
