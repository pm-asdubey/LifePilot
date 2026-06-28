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
import com.lifepilot.domain.repository.ReminderRepository
import com.lifepilot.domain.repository.TaskRepository
import com.lifepilot.features.home.state.AttentionItem
import com.lifepilot.features.home.state.AttentionType
import com.lifepilot.features.home.state.AttentionUrgency
import com.lifepilot.features.home.state.HomeMode
import com.lifepilot.features.home.state.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.time.Instant
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val goalRepository: GoalRepository,
    private val conversationRepository: ConversationRepository,
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val objectRepository: ObjectRepository,
    private val metadataRepository: MetadataRepository,
    private val planningEngine: PlanningEngine,
    private val lifeStateEngine: LifeStateEngine,
    private val aiProviderFactory: AiProviderFactory,
    private val preferenceManager: PreferenceManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Updated per-message with intelligent retrieval; always rebuilt from live data
    private var objectIndex: Map<String, Pair<String, String>> = emptyMap()
    private var objectMetadataIndex: Map<String, List<com.lifepilot.domain.model.MetadataEntry>> = emptyMap()

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
                .collect { profile ->
                    if (profile == null) {
                        _uiState.update { it.copy(isLoadingBrief = false) }
                        return@collect
                    }
                    _uiState.update { it.copy(profileName = profile.displayName) }
                    observeGoals(profile.profileId)
                    observeConversations(profile.profileId)
                    observeAttentionItems(profile.profileId)
                }
        }
    }

    private fun observeGoals(profileId: String) {
        viewModelScope.launch {
            goalRepository.observeActiveGoals(profileId)
                .catch { e -> Timber.e(e, "Error observing goals") }
                .collect { goals ->
                    _uiState.update { it.copy(activeGoals = goals, isLoadingBrief = false) }
                }
        }
    }

    private fun observeConversations(profileId: String) {
        viewModelScope.launch {
            conversationRepository.observeConversations(profileId)
                .catch { e -> Timber.e(e, "Error observing conversations") }
                .collect { conversations ->
                    _uiState.update { state ->
                        state.copy(
                            recentConversations = conversations.take(3),
                            allConversations = conversations,
                        )
                    }
                }
        }
    }

    private fun observeAttentionItems(profileId: String) {
        viewModelScope.launch {
            lifeStateEngine.observeAttentionRequired(profileId)
                .catch { e -> Timber.e(e, "Error observing attention items") }
                .collect { engineItems ->
                    val mapped = engineItems.take(5).map { item ->
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
                    _uiState.update { it.copy(attentionItems = mapped) }
                }
        }
    }

    // Input
    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    // Send from either mode — transitions to AI_WORKSPACE
    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        val state = _uiState.value

        // Transition to AI_WORKSPACE if needed
        if (state.mode == HomeMode.DAILY_BRIEF) {
            _uiState.update { it.copy(mode = HomeMode.AI_WORKSPACE) }
        }

        // Optimistic user message
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
                // Ensure we have a conversation
                val profileId = preferenceManager.getActiveProfileId() ?: run {
                    _uiState.update { it.copy(isAiLoading = false, error = "No active profile") }
                    return@launch
                }

                val conversation = conversationRepository.getOrCreateConversation(
                    profileId = profileId,
                    conversationId = _uiState.value.currentConversationId,
                )

                // Fix up the message conversationId if it was blank
                val userMsg = StoredMessage(
                    messageId = userMessageId,
                    conversationId = conversation.conversationId,
                    role = "USER",
                    content = text,
                    timestamp = nowInstant,
                )

                _uiState.update { s ->
                    val fixed = s.messages.map { msg ->
                        if (msg.messageId == userMessageId) userMsg else msg
                    }
                    s.copy(
                        currentConversationId = conversation.conversationId,
                        conversationTitle = conversation.title,
                        messages = fixed,
                    )
                }

                // Persist user message
                conversationRepository.saveMessage(userMsg)

                // Auto-generate title from first message
                if (conversation.title == "New conversation") {
                    val autoTitle = text.take(50).trimEnd()
                    conversationRepository.updateTitle(conversation.conversationId, autoTitle)
                    _uiState.update { it.copy(conversationTitle = autoTitle) }
                }

                // Build AI context with intelligent retrieval scoped to user's query intent
                val systemPrompt = buildSystemPrompt(userQuery = text)
                val history = _uiState.value.messages
                    .dropLast(1) // exclude current user message
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
                val conversations = _uiState.value.allConversations
                val conv = conversations.firstOrNull { it.conversationId == conversationId }
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
            // If deleting current, return to brief
            if (_uiState.value.currentConversationId == conversationId) {
                startNewChat()
            }
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

    // ---- AI parsing (adapted from AiChatViewModel) ----

    private data class ParsedResponse(
        val visibleContent: String,
        val action: AiProposal?,
        val question: String?,
    )

    private fun parseAiResponse(raw: String): ParsedResponse {
        var content = raw
        var action: AiProposal? = null
        var question: String? = null

        val actionPattern = Regex("""\[LIFEPILOT_ACTION\](.*?)\[/LIFEPILOT_ACTION\]""", RegexOption.DOT_MATCHES_ALL)
        val actionMatch = actionPattern.find(raw)
        if (actionMatch != null) {
            content = content.replace(actionMatch.value, "").trim()
            action = runCatching { parseAction(actionMatch.groupValues[1].trim()) }.getOrNull()
        }

        val askPattern = Regex("""\[ASK\](.*?)\[/ASK\]""", RegexOption.DOT_MATCHES_ALL)
        val askMatch = askPattern.find(raw)
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
                        if (tasksArray != null) {
                            for (i in 0 until tasksArray.length()) add(tasksArray.getString(i))
                        }
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
                else -> {
                    // METADATA_UPDATE (default)
                    val objectType = obj.optString("objectType", "")
                    val matchField = obj.optString("matchField", "")
                    val matchValue = obj.optString("matchValue", "")

                    val candidateIds = objectIndex.entries
                        .filter { (_, v) -> v.second.equals(objectType, ignoreCase = true) }
                        .map { it.key }

                    val resolvedObjectId = when {
                        candidateIds.isEmpty() -> return null
                        candidateIds.size == 1 -> candidateIds.first()
                        matchField.isNotBlank() && matchValue.isNotBlank() -> {
                            candidateIds.firstOrNull { id ->
                                val title = objectIndex[id]?.first ?: ""
                                title.contains(matchValue, ignoreCase = true) ||
                                    objectMetadataIndex[id]?.any { entry ->
                                        entry.fieldId.equals(matchField, ignoreCase = true) &&
                                            entry.value.contains(matchValue, ignoreCase = true)
                                    } == true
                            } ?: candidateIds.first()
                        }
                        else -> candidateIds.first()
                    }
                    val resolvedTitle = objectIndex[resolvedObjectId]?.first ?: objectType

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
                    AiProposal.MetadataUpdate(
                        proposalId = UUID.randomUUID().toString(),
                        objectId = resolvedObjectId,
                        objectTitle = resolvedTitle,
                        objectType = objectType,
                        summary = summary.ifBlank { "Update suggested" },
                        fields = fields,
                    )
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse LIFEPILOT_ACTION block")
            null
        }
    }

    private suspend fun buildSystemPrompt(userQuery: String = ""): String {
        val profile = profileRepository.observeActiveProfile()
            .catch { }
            .firstOrNull()

        val allObjects = profile?.let {
            objectRepository.observeObjectsByProfile(it.profileId)
                .catch { }
                .firstOrNull()
                ?: emptyList()
        } ?: emptyList()

        // Always keep objectIndex current for parseAction() matching
        val allMetadata = runCatching {
            metadataRepository.getMetadataForObjects(allObjects.map { it.objectId })
        }.getOrElse { emptyMap() }
        objectIndex = allObjects.associate { it.objectId to (it.title to it.objectType) }
        objectMetadataIndex = allMetadata

        // Intelligent retrieval: score objects by relevance to user query
        val objects = if (userQuery.isBlank()) {
            allObjects.take(5) // General context: show most recently active
        } else {
            selectRelevantObjects(userQuery, allObjects, allMetadata, maxObjects = 5)
        }

        val metadataByObject = allMetadata.filterKeys { it in objects.map { o -> o.objectId }.toSet() }

        val pendingTasks = profile?.let {
            taskRepository.observePendingTasks(it.profileId)
                .catch { }
                .firstOrNull()
                ?: emptyList()
        } ?: emptyList()

        val upcomingReminders = runCatching {
            reminderRepository.observeUpcomingReminders(
                Instant.now().plus(30, ChronoUnit.DAYS)
            ).catch { }.firstOrNull() ?: emptyList()
        }.getOrElse { emptyList() }

        return buildString {
            appendLine("You are the LifePilot AI assistant. You help users manage their administrative life.")
            appendLine("You have access to the user's structured life data below. Answer questions based ONLY on this data.")
            appendLine("Be concise, practical, and focused on actionable insights.")
            appendLine("Today's date: ${java.time.LocalDate.now()}")
            appendLine()
            appendLine("IMPORTANT — DETECTING LIFE EVENTS AND PLANS:")
            appendLine("When the user mentions a life event or plan, respond helpfully and append ONE structured block.")
            appendLine()
            appendLine("ACTION TYPES:")
            appendLine()
            appendLine("1. Update a tracked record (use when an event affects an existing record):")
            appendLine("[LIFEPILOT_ACTION]")
            appendLine("{")
            appendLine("  \"actionType\": \"METADATA_UPDATE\",")
            appendLine("  \"objectType\": \"<exact objectType>\",")
            appendLine("  \"matchField\": \"<field used to identify the record>\",")
            appendLine("  \"matchValue\": \"<value of that field>\",")
            appendLine("  \"summary\": \"<one-line human description>\",")
            appendLine("  \"fields\": [{\"fieldId\": \"notes\", \"displayName\": \"Notes\", \"value\": \"<what happened>\", \"mode\": \"append\"}]")
            appendLine("}")
            appendLine("[/LIFEPILOT_ACTION]")
            appendLine()
            appendLine("2. Propose a new Goal (use when user mentions a significant plan or ambition):")
            appendLine("[LIFEPILOT_ACTION]")
            appendLine("{")
            appendLine("  \"actionType\": \"GOAL_PROPOSAL\",")
            appendLine("  \"summary\": \"<why this goal matters>\",")
            appendLine("  \"title\": \"<goal title>\",")
            appendLine("  \"description\": \"<goal description>\",")
            appendLine("  \"deadline\": \"<YYYY-MM-DD or null>\",")
            appendLine("  \"estimatedWeeks\": <number or null>,")
            appendLine("  \"suggestedTasks\": [\"<task 1>\", \"<task 2>\"],")
            appendLine("  \"linkedObjectId\": \"<objectId or null>\"")
            appendLine("}")
            appendLine("[/LIFEPILOT_ACTION]")
            appendLine()
            appendLine("3. Create a new Task (use when user mentions a one-off action to track):")
            appendLine("[LIFEPILOT_ACTION]")
            appendLine("{")
            appendLine("  \"actionType\": \"TASK_CREATION\",")
            appendLine("  \"summary\": \"<why this task matters>\",")
            appendLine("  \"title\": \"<task title>\",")
            appendLine("  \"description\": \"<optional detail>\",")
            appendLine("  \"dueDate\": \"<YYYY-MM-DD or null>\",")
            appendLine("  \"goalId\": \"<goalId or null>\",")
            appendLine("  \"objectId\": \"<objectId or null>\"")
            appendLine("}")
            appendLine("[/LIFEPILOT_ACTION]")
            appendLine()
            appendLine("4. Complete an existing Task (use when user says they finished something tracked):")
            appendLine("[LIFEPILOT_ACTION]")
            appendLine("{")
            appendLine("  \"actionType\": \"TASK_COMPLETION\",")
            appendLine("  \"summary\": \"<confirmation message>\",")
            appendLine("  \"taskId\": \"<taskId>\",")
            appendLine("  \"taskTitle\": \"<task title>\",")
            appendLine("  \"goalId\": \"<goalId or null>\"")
            appendLine("}")
            appendLine("[/LIFEPILOT_ACTION]")
            appendLine()
            appendLine("If you need information to give better help, append ONE question block:")
            appendLine("[ASK]<the question to ask the user>[/ASK]")
            appendLine()
            appendLine("5. Create a new record (use when user mentions a life entity not yet tracked):")
            appendLine("[LIFEPILOT_ACTION]")
            appendLine("{")
            appendLine("  \"actionType\": \"OBJECT_CREATION\",")
            appendLine("  \"summary\": \"<why this record matters>\",")
            appendLine("  \"objectType\": \"<type e.g. Vehicle, Property, Job, Insurance>\",")
            appendLine("  \"domain\": \"<domain e.g. Career, Property, Finance, Health, Identity, Travel>\",")
            appendLine("  \"title\": \"<record title>\",")
            appendLine("  \"fields\": [{\"fieldId\": \"notes\", \"displayName\": \"Notes\", \"value\": \"<initial context>\"}]")
            appendLine("}")
            appendLine("[/LIFEPILOT_ACTION]")
            appendLine()
            appendLine("Rules:")
            appendLine("Include [LIFEPILOT_ACTION] only when a clear event, plan, task, completion, or new life entity is detected.")
            appendLine("Include [ASK] only when more context would meaningfully improve the profile.")
            appendLine("Never include both [LIFEPILOT_ACTION] and [ASK] in the same response.")
            appendLine("Never make up data. Only use what the user tells you.")
            appendLine("Prefer GOAL_PROPOSAL for multi-step plans; TASK_CREATION for single actions.")
            appendLine("Use OBJECT_CREATION only when a significant life entity doesn't yet have a record.")
            appendLine()
            appendLine("USER LIFE DATA:")
            if (profile != null) appendLine("Profile: ${profile.displayName}")
            appendLine()
            val objectLabel = "Relevant records (${objects.size} of ${allObjects.size} total):"
            appendLine(objectLabel)
            objects.forEach { obj ->
                appendLine("  - [id=${obj.objectId}] ${obj.title} [type=${obj.objectType}, domain=${obj.domain}, status=${obj.status}]")
                val metadata = metadataByObject[obj.objectId] ?: emptyList()
                metadata.take(8).forEach { entry ->
                    appendLine("    ${entry.fieldId}: ${entry.value}")
                }
            }
            if (objects.isEmpty()) appendLine("  (No records yet.)")
            appendLine()
            appendLine("Pending tasks (${pendingTasks.size}):")
            pendingTasks.take(10).forEach { task ->
                val due = task.dueDate?.toString() ?: "no due date"
                appendLine("  - ${task.title} [priority=${task.priority}, due=$due]")
            }
            appendLine()
            appendLine("Upcoming reminders (next 30 days, ${upcomingReminders.size}):")
            upcomingReminders.take(10).forEach { reminder ->
                appendLine("  - ${reminder.title} [due=${reminder.triggerDate}, priority=${reminder.priority}]")
            }
        }
    }

    /**
     * Scores all objects against the user's query and returns the top N most relevant.
     * Uses simple keyword matching against title, type, domain, and metadata values.
     * This is intentionally lightweight — the full semantic search happens at the AI layer.
     */
    private fun selectRelevantObjects(
        query: String,
        allObjects: List<com.lifepilot.domain.model.LifeObject>,
        allMetadata: Map<String, List<com.lifepilot.domain.model.MetadataEntry>>,
        maxObjects: Int,
    ): List<com.lifepilot.domain.model.LifeObject> {
        val tokens = query.lowercase().split(" ", ",", ".", "?", "!").filter { it.length > 2 }
        if (tokens.isEmpty()) return allObjects.take(maxObjects)

        return allObjects
            .map { obj ->
                var score = 0f
                val titleLower = obj.title.lowercase()
                val typeLower = obj.objectType.lowercase()
                val domainLower = obj.domain.lowercase()

                tokens.forEach { token ->
                    if (titleLower.contains(token)) score += 3f
                    if (typeLower.contains(token)) score += 2f
                    if (domainLower.contains(token)) score += 1.5f
                }

                val metadata = allMetadata[obj.objectId] ?: emptyList()
                metadata.take(10).forEach { entry ->
                    tokens.forEach { token ->
                        if (entry.value.lowercase().contains(token)) score += 0.5f
                        if (entry.fieldId.lowercase().contains(token)) score += 0.3f
                    }
                }
                obj to score
            }
            .filter { (_, score) -> score > 0f }
            .sortedByDescending { (_, score) -> score }
            .take(maxObjects)
            .map { (obj, _) -> obj }
            .ifEmpty { allObjects.take(maxObjects) } // fallback to top N if no matches
    }

    private fun mapActionType(actionType: String): AttentionType {
        return when {
            actionType.contains("expir", ignoreCase = true) -> AttentionType.EXPIRING
            actionType.contains("verif", ignoreCase = true) -> AttentionType.PENDING_VERIFICATION
            actionType.contains("goal", ignoreCase = true) -> AttentionType.GOAL
            actionType.contains("task", ignoreCase = true) -> AttentionType.TASK
            else -> AttentionType.SUGGESTION
        }
    }

    private fun timeBasedGreeting(): String {
        return when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }
}
