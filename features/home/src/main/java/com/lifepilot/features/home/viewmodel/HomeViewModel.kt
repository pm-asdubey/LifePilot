package com.lifepilot.features.home.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.data.ai.AiProviderFactory
import com.lifepilot.data.storage.FileStorageManager
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.ai.AiCompletionResult
import com.lifepilot.domain.ai.AiMessage
import com.lifepilot.domain.ai.AiMessageRole
import com.lifepilot.domain.ai.AiProvider
import com.lifepilot.domain.engine.ActionPlanExecutor
import com.lifepilot.domain.engine.AttentionPriority
import com.lifepilot.domain.engine.DomainLifeStateEngine
import com.lifepilot.domain.engine.LifeStateEngine
import com.lifepilot.domain.engine.PlanningEngine
import com.lifepilot.domain.engine.PromptBuilder
import com.lifepilot.domain.engine.RetrievalEngine
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.ocr.OcrService
import com.lifepilot.domain.usecase.UploadDocumentUseCase
import com.lifepilot.domain.model.ActionItem
import com.lifepilot.domain.model.ActionPlan
import com.lifepilot.domain.model.ActionPlanType
import com.lifepilot.domain.model.AiProposal
import com.lifepilot.domain.model.AttachedDocumentContext
import com.lifepilot.domain.model.ClarifyingQuestion
import com.lifepilot.domain.model.FieldSensitivity
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.model.ObjectStatus
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
import com.lifepilot.domain.repository.ProjectRepository
import com.lifepilot.domain.usecase.UpdateObjectStatusUseCase
import com.lifepilot.features.home.state.AttentionItem
import com.lifepilot.features.home.state.AttentionType
import com.lifepilot.features.home.state.AttentionUrgency
import com.lifepilot.features.home.state.HomeMode
import com.lifepilot.features.home.state.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.json.JSONObject
import timber.log.Timber
import java.time.Instant
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
    private val goalRepository: GoalRepository,
    private val conversationRepository: ConversationRepository,
    private val objectRepository: ObjectRepository,
    private val metadataRepository: MetadataRepository,
    private val updateObjectStatusUseCase: UpdateObjectStatusUseCase,
    private val planningEngine: PlanningEngine,
    private val lifeStateEngine: LifeStateEngine,
    private val retrievalEngine: RetrievalEngine,
    private val promptBuilder: PromptBuilder,
    private val aiProviderFactory: AiProviderFactory,
    private val preferenceManager: PreferenceManager,
    private val domainLifeStateEngine: DomainLifeStateEngine,
    private val actionPlanExecutor: ActionPlanExecutor,
    private val schemaEngine: SchemaEngine,
    private val fileStorageManager: FileStorageManager,
    private val ocrService: OcrService,
    private val uploadDocumentUseCase: UploadDocumentUseCase,
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Kept current after each AI request for post-response parseAction() resolution.
    // RetrievalEngine populates these; HomeViewModel reads them — never writes them.
    private var currentObjectIndex: Map<String, Pair<String, String>> = emptyMap()
    private var currentObjectMetadataIndex: Map<String, List<com.lifepilot.domain.model.MetadataEntry>> = emptyMap()
    // Domains and snapshots active in the last retrieval — used to focus domain life state updates.
    private var currentRetrievalDomains: List<String> = emptyList()
    // objectId → domain lookup, built from all objects in the retrieval context.
    private var currentObjectDomainIndex: Map<String, String> = emptyMap()

    private var activeAiJob: Job? = null

    init {
        _uiState.update { it.copy(greeting = timeBasedGreeting()) }
        checkAiConfigured()
        observeBriefData()
        consumeResumeConversationId()
    }

    private fun consumeResumeConversationId() {
        val conversationId: String? = savedStateHandle.remove("resumeConversationId")
        if (!conversationId.isNullOrBlank()) {
            resumeConversation(conversationId)
        }
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
                            .onStart { emit(emptyList()) }
                            .catch { e -> Timber.e(e, "Error observing goals"); emit(emptyList()) },
                        conversationRepository.observeConversations(profile.profileId)
                            .onStart { emit(emptyList()) }
                            .catch { e -> Timber.e(e, "Error observing conversations"); emit(emptyList()) },
                        lifeStateEngine.observeAttentionRequired(profile.profileId)
                            .onStart { emit(emptyList()) }
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
            // Auto-start a new conversation if the current one has been idle for
            // longer than the threshold. Prevents new messages from appending to
            // an old conversation (ISSUE-001).
            if (shouldStartNewConversation()) {
                _uiState.update {
                    it.copy(
                        mode = HomeMode.AI_WORKSPACE,
                        currentConversationId = null,
                        conversationTitle = "New conversation",
                        messages = emptyList(),
                    )
                }
            } else {
                _uiState.update { it.copy(mode = HomeMode.AI_WORKSPACE) }
            }
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

        activeAiJob?.cancel()
        activeAiJob = viewModelScope.launch {
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

                // Step 1: retrieve life context from local DB.
                _uiState.update { it.copy(aiStatusMessage = "Looking up your records…") }
                val attachedContext = _uiState.value.attachedDocumentContext
                val retrievalContext = retrievalEngine.retrieve(profileId, userQuery = text)
                    .copy(attachedDocumentContext = attachedContext)
                currentObjectIndex = retrievalContext.allObjectIndex
                currentObjectDomainIndex = retrievalContext.allObjectDomainIndex
                currentObjectMetadataIndex = retrievalContext.allObjectMetadata
                currentRetrievalDomains = retrievalContext.relevantSnapshots.map { it.domain }.distinct()

                // Step 2: build the prompt from retrieved context.
                _uiState.update { it.copy(aiStatusMessage = "Building context…") }
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
                val result = executeWithRetries(
                    provider = provider,
                    systemPrompt = systemPrompt,
                    userMessage = text,
                    conversationHistory = history,
                )

                when (result) {
                    is AiCompletionResult.Success -> {
                        Timber.d("AI raw response: ${result.content}")
                        val (visibleContent, action, question) = parseAiResponse(result.content)
                        Timber.d("AI parsed: action=${action?.javaClass?.simpleName}, question=$question")
                        // When the AI response is entirely an action block with no surrounding text,
                        // visibleContent is blank. Use the action summary so no empty bubble appears.
                        val displayContent = visibleContent.ifBlank {
                            action?.summary?.takeIf { it.isNotBlank() } ?: return@launch
                        }
                        val assistantMsg = StoredMessage(
                            messageId = UUID.randomUUID().toString(),
                            conversationId = conversation.conversationId,
                            role = "ASSISTANT",
                            content = displayContent,
                            timestamp = Instant.now(),
                        )
                        conversationRepository.saveMessage(assistantMsg)

                        val autoProposal = action as? AiProposal.MetadataUpdate
                        val shouldAutoApply = autoProposal != null &&
                            autoProposal.proposalSensitivity != FieldSensitivity.SENSITIVE

                        if (shouldAutoApply) {
                            _uiState.update { s ->
                                s.copy(
                                    messages = s.messages + assistantMsg,
                                    isAiLoading = false,
                                    isAiConfigured = true,
                                    pendingAction = null,
                                    pendingContextQuestion = question,
                                )
                            }
                            viewModelScope.launch {
                                runCatching {
                                    autoApplyProposal(autoProposal, conversation.conversationId, profileId)
                                }.onFailure { Timber.e(it, "Auto-apply failed") }
                            }
                        } else {
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

                        // Fire domain life state update in the background — non-blocking.
                        // Captures the conversation turn that just completed.
                        val domainsSnapshot = currentRetrievalDomains
                        if (domainsSnapshot.isNotEmpty()) {
                            launch {
                                runCatching {
                                    domainLifeStateEngine.evaluateAndUpdate(
                                        profileId = profileId,
                                        userMessage = text,
                                        aiResponse = visibleContent,
                                        affectedDomains = domainsSnapshot,
                                    )
                                }.onFailure { Timber.w(it, "Domain life state update failed silently") }
                            }
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
                        conversationRepository.saveMessage(errorMsg)
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
                        conversationRepository.saveMessage(unavailableMsg)
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
                if (e is kotlinx.coroutines.CancellationException) {
                    Timber.d("AI request cancelled by user")
                    _uiState.update { it.copy(isAiLoading = false, aiStatusMessage = null) }
                } else {
                    Timber.e(e, "AI query failed")
                    _uiState.update { it.copy(isAiLoading = false, error = "Request failed: ${e.message}") }
                }
            } finally {
                activeAiJob = null
            }
        }
    }

    fun abortAi() {
        activeAiJob?.cancel()
        activeAiJob = null
        _uiState.update { it.copy(isAiLoading = false, aiStatusMessage = null) }
        val convId = _uiState.value.currentConversationId ?: return
        val abortMsg = StoredMessage(
            messageId = UUID.randomUUID().toString(),
            conversationId = convId,
            role = "ASSISTANT",
            content = "Request cancelled.",
            timestamp = Instant.now(),
        )
        viewModelScope.launch {
            runCatching { conversationRepository.saveMessage(abortMsg) }
            _uiState.update { s -> s.copy(messages = s.messages + abortMsg) }
        }
    }

    fun processAttachment(uri: Uri) {
        viewModelScope.launch {
            val profileId = preferenceManager.getActiveProfileId() ?: return@launch
            val convId = _uiState.value.currentConversationId ?: return@launch

            _uiState.update { it.copy(isAiLoading = true, aiStatusMessage = "Reading attachment…") }

            val fileName = resolveUriFileName(uri) ?: "attachment"
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val storedFile = fileStorageManager.copyFromUri(uri, "chat_import", fileName)

            if (storedFile == null) {
                _uiState.update { it.copy(isAiLoading = false, aiStatusMessage = null) }
                postSystemMessage(convId, "Sorry, I couldn't read that attachment.")
                return@launch
            }

            _uiState.update { it.copy(aiStatusMessage = "Scanning document…") }
            val ocrResult = ocrService.extractText(storedFile.absolutePath, mimeType)
            val ocrText = when (ocrResult) {
                is com.lifepilot.domain.ocr.OcrResult.Success -> ocrResult.text
                else -> ""
            }

            if (ocrText.isBlank()) {
                _uiState.update { it.copy(isAiLoading = false, aiStatusMessage = null) }
                postSystemMessage(convId, "I couldn't extract text from that file. You can still find it in Library.")
                createPlaceholderObject(profileId, convId, fileName, storedFile.absolutePath, mimeType)
                return@launch
            }

            _uiState.update { it.copy(aiStatusMessage = "Understanding document…") }
            val classificationPrompt = buildAttachmentClassificationPrompt(ocrText, fileName)
            val provider = aiProviderFactory.getProvider()
            val result = provider.complete(
                systemPrompt = classificationPrompt,
                userMessage = "Classify and extract this document.",
                conversationHistory = emptyList(),
            )

            _uiState.update { it.copy(isAiLoading = false, aiStatusMessage = null) }

            when (result) {
                is AiCompletionResult.Success -> {
                    val actionMatch = Regex(
                        "\\[LIFEPILOT_ACTION\\](.*?)\\[/LIFEPILOT_ACTION\\]",
                        RegexOption.DOT_MATCHES_ALL,
                    ).find(result.content)
                    val proposal = actionMatch?.groupValues?.get(1)?.let { parseAction(it.trim()) }

                    if (proposal is AiProposal.ObjectCreation) {
                        postSystemMessage(convId, "I found a ${proposal.objectType}. Please review before saving.")
                        val enrichedProposal = proposal.copy(
                            attachedFilePath = storedFile.absolutePath,
                            attachedFileName = fileName,
                            attachedMimeType = mimeType,
                        )
                        _uiState.update {
                            it.copy(
                                pendingAction = enrichedProposal,
                                attachedDocumentContext = AttachedDocumentContext(
                                    fileName = fileName,
                                    mimeType = mimeType,
                                    objectType = proposal.objectType,
                                    domain = proposal.domain,
                                    title = proposal.title,
                                    extractedFields = proposal.fields,
                                    isPendingApproval = true,
                                ),
                            )
                        }
                    } else if (proposal is AiProposal.MetadataUpdate) {
                        postSystemMessage(convId, "I found updates for ${proposal.objectTitle}. Please review.")
                        _uiState.update {
                            it.copy(
                                pendingAction = proposal,
                                attachedDocumentContext = AttachedDocumentContext(
                                    fileName = fileName,
                                    mimeType = mimeType,
                                    objectType = proposal.objectType,
                                    title = proposal.objectTitle,
                                    extractedFields = proposal.fields,
                                    isPendingApproval = true,
                                ),
                            )
                        }
                    } else {
                        postSystemMessage(convId, "I read the document but couldn't classify it. You can review it in Library.")
                        createPlaceholderObject(profileId, convId, fileName, storedFile.absolutePath, mimeType)
                    }
                }
                else -> {
                    postSystemMessage(convId, "I read the document but AI classification is unavailable. You can review it in Library.")
                    createPlaceholderObject(profileId, convId, fileName, storedFile.absolutePath, mimeType)
                }
            }
        }
    }

    private suspend fun createPlaceholderObject(
        profileId: String,
        convId: String,
        fileName: String,
        filePath: String,
        mimeType: String,
    ) {
        val title = fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
        val obj = objectRepository.createObject(
            profileId = profileId,
            objectType = "Document",
            domain = "General",
            title = title.ifBlank { "Attached document" },
            description = null,
        )
        uploadDocumentUseCase(
            objectId = obj.objectId,
            filePath = filePath,
            originalName = fileName,
            mimeType = mimeType,
            documentType = if (mimeType.contains("image")) "IMAGE" else "OTHER",
        )
        postSystemMessage(convId, "Saved '$title' to Library.")
    }

    private fun buildAttachmentClassificationPrompt(ocrText: String, fileName: String): String {
        val schemaTypes = schemaEngine.getAllObjectTypes().joinToString(", ")
        return buildString {
            appendLine("You are classifying a document uploaded to LifePilot.")
            appendLine("Available record types: $schemaTypes")
            appendLine("Return ONE action block using exact tags [LIFEPILOT_ACTION] and [/LIFEPILOT_ACTION].")
            appendLine("If this is a new document, use OBJECT_CREATION with objectType, domain, title, and fields.")
            appendLine("If it updates an existing record, use METADATA_UPDATE with objectType, matchField, matchValue, and fields.")
            appendLine("Sensitive values like passport numbers, Aadhaar, PAN, account numbers are allowed in the action block because the user will approve them.")
            appendLine()
            appendLine("OCR text from '$fileName':")
            appendLine(ocrText.take(4000))
        }
    }

    private fun resolveUriFileName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun postSystemMessage(convId: String, content: String) {
        val msg = StoredMessage(
            messageId = UUID.randomUUID().toString(),
            conversationId = convId,
            role = "ASSISTANT",
            content = content,
            timestamp = Instant.now(),
        )
        viewModelScope.launch {
            runCatching { conversationRepository.saveMessage(msg) }
            _uiState.update { s -> s.copy(messages = s.messages + msg) }
        }
    }

    fun approveAction() {
        val proposal = _uiState.value.pendingAction ?: return
        _uiState.update { it.copy(pendingAction = null, attachedDocumentContext = null) }
        viewModelScope.launch {
            try {
                val profileId = preferenceManager.getActiveProfileId() ?: return@launch
                val convId = _uiState.value.currentConversationId ?: return@launch

                when (proposal) {
                    is AiProposal.ActionPlan -> executeActionPlan(proposal.plan, convId, profileId)
                    else -> executeSingleProposal(proposal, convId, profileId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to execute AI proposal")
            }
        }
    }

    fun approveActionPlan(plan: ActionPlan) {
        _uiState.update { it.copy(pendingAction = null) }
        viewModelScope.launch {
            try {
                val profileId = preferenceManager.getActiveProfileId() ?: return@launch
                val convId = _uiState.value.currentConversationId ?: return@launch
                executeActionPlan(plan, convId, profileId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to execute action plan")
            }
        }
    }

    private suspend fun executeSingleProposal(proposal: AiProposal, convId: String, profileId: String) {
        val confirmText = executeProposal(proposal)
        val confirmMsg = StoredMessage(
            messageId = UUID.randomUUID().toString(),
            conversationId = convId,
            role = "ASSISTANT",
            content = confirmText,
            timestamp = Instant.now(),
        )
        conversationRepository.saveMessage(confirmMsg)
        _uiState.update { s -> s.copy(messages = s.messages + confirmMsg) }

        val affectedDomain = proposalDomain(proposal)
        if (affectedDomain != null) {
            runCatching {
                domainLifeStateEngine.evaluateAndUpdate(
                    profileId = profileId,
                    userMessage = "Action approved: $confirmText",
                    aiResponse = confirmText,
                    affectedDomains = listOf(affectedDomain),
                )
            }.onFailure { Timber.w(it, "Post-approval domain update failed") }
        }
    }

    private suspend fun autoApplyProposal(proposal: AiProposal.MetadataUpdate, convId: String, profileId: String) {
        executeProposal(proposal)
        val fieldNames = proposal.fields.joinToString(", ") { it.displayName.ifBlank { it.fieldId } }
        val confirmText = when (proposal.proposalSensitivity) {
            FieldSensitivity.PREFERENCE -> "Noted: $fieldNames"
            else -> "Saved: $fieldNames"
        }
        val confirmMsg = StoredMessage(
            messageId = UUID.randomUUID().toString(),
            conversationId = convId,
            role = "ASSISTANT",
            content = confirmText,
            timestamp = Instant.now(),
        )
        conversationRepository.saveMessage(confirmMsg)
        _uiState.update { s -> s.copy(messages = s.messages + confirmMsg) }

        val affectedDomain = proposalDomain(proposal)
        if (affectedDomain != null) {
            runCatching {
                domainLifeStateEngine.evaluateAndUpdate(
                    profileId = profileId,
                    userMessage = "Auto-saved: $confirmText",
                    aiResponse = confirmText,
                    affectedDomains = listOf(affectedDomain),
                )
            }.onFailure { Timber.w(it, "Post-auto-apply domain update failed") }
        }
    }

    private suspend fun executeActionPlan(plan: ActionPlan, convId: String, profileId: String) {
        _uiState.update { it.copy(isAiLoading = true) }

        val executedItems = mutableListOf<Pair<ActionItem, Boolean>>()

        val result = actionPlanExecutor.execute(plan) { item, itemResult ->
            executedItems.add(item to itemResult.isSuccess)
            val status = if (itemResult.isSuccess) "✓" else "✗"
            val progressMsg = StoredMessage(
                messageId = UUID.randomUUID().toString(),
                conversationId = convId,
                role = "ASSISTANT",
                content = "$status ${item.summary}",
                timestamp = Instant.now(),
            )
            viewModelScope.launch {
                conversationRepository.saveMessage(progressMsg)
            }
            _uiState.update { s -> s.copy(messages = s.messages + progressMsg) }
        }

        _uiState.update { it.copy(isAiLoading = false) }

        val taskDueDates = plan.items
            .filterIsInstance<ActionItem.CreateTask>()
            .mapNotNull { it.dueDate }
            .distinct()
            .sorted()
        val reminderText = when {
            taskDueDates.isEmpty() -> ""
            taskDueDates.size == 1 -> " I'll remind you about the task on ${taskDueDates.first()}."
            else -> " I'll remind you about the tasks on the dates mentioned."
        }

        val confirmation = buildString {
            appendLine("Action plan completed: ${plan.summary}.$reminderText")
            appendLine()
            val recordUpdates = executedItems.filter { it.first.isRecordUpdate() && it.second }
            val objectCreations = executedItems.filter { it.first.isObjectCreation() && it.second }
            val taskCreations = executedItems.filter { it.first.isTaskCreation() && it.second }

            if (recordUpdates.isNotEmpty()) {
                appendLine("✓ Records updated")
                recordUpdates.forEach { appendLine("  • ${it.first.summary}") }
                appendLine()
            }
            if (objectCreations.isNotEmpty()) {
                appendLine("✓ Objects created")
                objectCreations.forEach { appendLine("  • ${it.first.summary}") }
                appendLine()
            }
            if (taskCreations.isNotEmpty()) {
                appendLine("✓ Tasks created")
                taskCreations.forEach { appendLine("  • ${it.first.summary}") }
                appendLine()
            }
            if (result.isFailure) {
                appendLine("Some items failed. Please review the messages above.")
                appendLine()
            }
            appendLine("Now, what can I help you with?")
        }

        val summaryMsg = StoredMessage(
            messageId = UUID.randomUUID().toString(),
            conversationId = convId,
            role = "ASSISTANT",
            content = confirmation.trim(),
            timestamp = Instant.now(),
        )
        conversationRepository.saveMessage(summaryMsg)
        _uiState.update { s -> s.copy(messages = s.messages + summaryMsg) }
    }

    private fun proposalDomain(proposal: AiProposal): String? = when (proposal) {
        is AiProposal.ObjectCreation -> proposal.domain
        is AiProposal.MetadataUpdate ->
            currentObjectDomainIndex[proposal.objectId] ?: currentRetrievalDomains.firstOrNull()
        is AiProposal.StatusUpdate ->
            currentObjectDomainIndex[proposal.objectId] ?: currentRetrievalDomains.firstOrNull()
        is AiProposal.GoalProposal ->
            proposal.linkedObjectId?.let { currentObjectDomainIndex[it] }
                ?: currentRetrievalDomains.firstOrNull()
        is AiProposal.TaskCreation ->
            proposal.objectId?.let { currentObjectDomainIndex[it] }
                ?: currentRetrievalDomains.firstOrNull()
        is AiProposal.TaskCompletion -> currentRetrievalDomains.firstOrNull()
        is AiProposal.ActionPlan -> currentRetrievalDomains.firstOrNull()
        is AiProposal.ProjectCreation -> proposal.domain ?: currentRetrievalDomains.firstOrNull()
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
                val fieldsToSave = proposal.fields.ifEmpty {
                    proposal.initialNotes?.let {
                        listOf(
                            ProposedField(
                                fieldId = "notes",
                                displayName = "Notes",
                                value = it,
                            )
                        )
                    } ?: emptyList()
                }
                for (field in fieldsToSave) {
                    metadataRepository.upsertMetadata(
                        objectId = createdObject.objectId,
                        fieldId = field.fieldId,
                        value = field.value,
                        source = MetadataSource.AI_EXTRACTED,
                        confidence = 0.8f,
                    )
                }
                val attachedFilePath = proposal.attachedFilePath
                if (!attachedFilePath.isNullOrBlank()) {
                    uploadDocumentUseCase(
                        objectId = createdObject.objectId,
                        filePath = attachedFilePath,
                        originalName = proposal.attachedFileName ?: "attachment",
                        mimeType = proposal.attachedMimeType ?: "application/octet-stream",
                        documentType = if (proposal.attachedMimeType?.contains("image") == true) "IMAGE" else "OTHER",
                    )
                }
                "\"${proposal.title}\" added to your records."
            }
            is AiProposal.StatusUpdate -> {
                updateObjectStatusUseCase(
                    objectId = proposal.objectId,
                    newStatus = proposal.newStatus,
                ).getOrThrow()
                "${proposal.objectTitle} marked as ${proposal.newStatus.name.lowercase().replaceFirstChar { it.uppercase() }}."
            }
            is AiProposal.ActionPlan -> {
                error("ActionPlan proposals must be executed via executeActionPlan(), not executeProposal()")
            }
            is AiProposal.ProjectCreation -> {
                val project = projectRepository.createProject(
                    profileId = profileId,
                    title = proposal.title,
                    description = proposal.description,
                    domain = proposal.domain,
                    emoji = "🎯",
                    targetDate = null,
                    isAiProposed = true,
                )
                // Link any objects the AI identified as belonging to this initiative.
                for (objectId in proposal.linkedObjectIds) {
                    runCatching { projectRepository.linkObject(project.projectId, objectId) }
                        .onFailure { Timber.w(it, "Failed to link objectId=$objectId to project ${project.projectId}") }
                }
                val linkedSuffix = if (proposal.linkedObjectIds.isNotEmpty()) {
                    " (${proposal.linkedObjectIds.size} records linked)"
                } else ""
                "Project \"${proposal.title}\" created.$linkedSuffix You can find it in Planner → Projects."
            }
        }
    }

    fun dismissAction() {
        _uiState.update { it.copy(pendingAction = null, attachedDocumentContext = null) }
    }

    fun dismissContextQuestion() {
        _uiState.update { it.copy(pendingContextQuestion = null) }
    }

    /**
     * Starts a fresh conversation while remaining in the AI workspace.
     * Called from the AI workspace "new chat" action.
     */
    fun startNewConversation() {
        _uiState.update {
            it.copy(
                currentConversationId = null,
                conversationTitle = "New conversation",
                messages = emptyList(),
                inputText = "",
                isAiLoading = false,
                aiStatusMessage = null,
                pendingAction = null,
                pendingContextQuestion = null,
                attachedDocumentContext = null,
                error = null,
            )
        }
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
                aiStatusMessage = null,
                pendingAction = null,
                pendingContextQuestion = null,
                attachedDocumentContext = null,
                error = null,
            )
        }
    }

    fun returnToBrief() {
        _uiState.update {
            it.copy(
                mode = HomeMode.DAILY_BRIEF,
                isAiLoading = false,
                aiStatusMessage = null,
                pendingAction = null,
                pendingContextQuestion = null,
                attachedDocumentContext = null,
                error = null,
                // Leaving the AI workspace means the active conversation is done;
                // the next question from the brief must start fresh (ISSUE-001).
                currentConversationId = null,
                conversationTitle = "New conversation",
                messages = emptyList(),
                inputText = "",
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
        val cleaned = raw.stripMarkdown()
        var content = cleaned
        var action: AiProposal? = null

        val actionMatch = ACTION_PATTERN.find(cleaned)
        if (actionMatch != null) {
            content = content.replace(actionMatch.value, "").trim()
            action = runCatching { parseAction(actionMatch.groupValues[1].trim()) }.getOrNull()
        }

        // Keep clarifying questions inside the normal AI reply instead of showing a separate UI card.
        val askMatch = ASK_PATTERN.find(cleaned)
        if (askMatch != null) {
            val questionText = askMatch.groupValues[1].trim()
            content = content.replace(askMatch.value, questionText).trim()
        }

        return ParsedResponse(content.trim(), action, question = null)
    }

    private fun String.stripMarkdown(): String {
        return this
            .replace(Regex("\\*\\*(.+?)\\*\\*")) { it.groupValues[1] }
            .replace(Regex("__(.+?)__")) { it.groupValues[1] }
            .replace(Regex("\\*(.+?)\\*")) { it.groupValues[1] }
            .replace(Regex("_(.+?)_")) { it.groupValues[1] }
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
                    val fieldsArray = obj.optJSONArray("fields")
                    val parsedFields = mutableListOf<ProposedField>()
                    if (fieldsArray != null) {
                        for (i in 0 until fieldsArray.length()) {
                            val fieldObj = fieldsArray.getJSONObject(i)
                            parsedFields.add(
                                ProposedField(
                                    fieldId = fieldObj.optString("fieldId", "notes"),
                                    displayName = fieldObj.optString("displayName", fieldObj.optString("fieldId", "Field")),
                                    value = fieldObj.optString("value", ""),
                                    mode = if (fieldObj.optString("mode") == "append") UpdateMode.APPEND else UpdateMode.SET,
                                )
                            )
                        }
                    }
                    AiProposal.ObjectCreation(
                        proposalId = UUID.randomUUID().toString(),
                        summary = summary,
                        objectType = obj.optString("objectType", "Record"),
                        domain = obj.optString("domain", "General"),
                        title = obj.optString("title", "New record"),
                        initialNotes = parsedFields.firstOrNull()?.value?.takeIf { it.isNotBlank() },
                        fields = parsedFields,
                    )
                }
                "STATUS_UPDATE" -> {
                    val resolved = resolveObjectForAction(obj) ?: return null
                    val newStatus = parseStatus(obj.optString("newStatus", "INACTIVE"))
                    AiProposal.StatusUpdate(
                        proposalId = UUID.randomUUID().toString(),
                        summary = summary.ifBlank { "Status updated" },
                        objectId = resolved.objectId,
                        objectTitle = resolved.title,
                        newStatus = newStatus,
                    )
                }
                "PROJECT_CREATION" -> {
                    val linkedObjectsArray = obj.optJSONArray("linkedObjectIds")
                    val linkedObjectIds = buildList {
                        if (linkedObjectsArray != null) for (i in 0 until linkedObjectsArray.length()) add(linkedObjectsArray.getString(i))
                    }
                    AiProposal.ProjectCreation(
                        proposalId = UUID.randomUUID().toString(),
                        summary = summary,
                        title = obj.optString("title", "New project"),
                        description = obj.optString("description", "").takeIf { it.isNotBlank() && it != "null" },
                        domain = obj.optString("domain", "").takeIf { it.isNotBlank() && it != "null" },
                        linkedObjectIds = linkedObjectIds,
                    )
                }
                "ACTION_PLAN" -> parseActionPlan(obj, summary)
                else -> parseMetadataUpdate(obj, summary)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse LIFEPILOT_ACTION block")
            null
        }
    }

    private fun parseActionPlan(obj: JSONObject, summary: String): AiProposal.ActionPlan? {
        return try {
            val planType = runCatching {
                ActionPlanType.valueOf(obj.optString("planType", "CUSTOM").uppercase())
            }.getOrDefault(ActionPlanType.CUSTOM)

            val itemsArray = obj.optJSONArray("items") ?: return null
            val items = buildList {
                for (i in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(i)
                    parseActionItem(itemObj)?.let { add(it) }
                }
            }
            if (items.isEmpty()) return null

            val questionsArray = obj.optJSONArray("clarifyingQuestions")
            val questions = buildList {
                if (questionsArray != null) {
                    for (i in 0 until questionsArray.length()) {
                        val qObj = questionsArray.getJSONObject(i)
                        add(
                            ClarifyingQuestion(
                                questionId = qObj.optString("questionId", "q$i"),
                                text = qObj.optString("text", ""),
                                affectedItemIds = qObj.optJSONArray("affectedItemIds")?.let { arr ->
                                    buildList { for (j in 0 until arr.length()) add(arr.getString(j)) }
                                } ?: emptyList(),
                            )
                        )
                    }
                }
            }

            val plan = ActionPlan(
                planId = UUID.randomUUID().toString(),
                type = planType,
                summary = summary.ifBlank { "Action plan" },
                items = items,
                clarifyingQuestions = questions,
            )

            AiProposal.ActionPlan(
                proposalId = UUID.randomUUID().toString(),
                summary = summary.ifBlank { "Action plan" },
                plan = plan,
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse ACTION_PLAN block")
            null
        }
    }

    private fun parseActionItem(obj: JSONObject): ActionItem? {
        return try {
            val type = obj.optString("type", "").uppercase()
            val itemId = obj.optString("itemId", UUID.randomUUID().toString())
            val itemSummary = obj.optString("summary", "")
            val dependsOn = obj.optJSONArray("dependsOn")?.let { arr ->
                buildList { for (i in 0 until arr.length()) add(arr.getString(i)) }
            } ?: emptyList()

            when (type) {
                "UPDATE_RECORD" -> {
                    val resolved = resolveObjectForAction(obj) ?: return null
                    val fieldsArray = obj.optJSONArray("fields") ?: return null
                    val fields = buildList {
                        for (i in 0 until fieldsArray.length()) {
                            val f = fieldsArray.getJSONObject(i)
                            add(
                                ProposedField(
                                    fieldId = f.optString("fieldId", "notes"),
                                    displayName = f.optString("displayName", "Field"),
                                    value = f.optString("value", ""),
                                    mode = runCatching {
                                        UpdateMode.valueOf(f.optString("mode", "SET").uppercase())
                                    }.getOrDefault(UpdateMode.SET),
                                )
                            )
                        }
                    }
                    ActionItem.UpdateRecord(
                        itemId = itemId,
                        summary = itemSummary,
                        objectId = resolved.objectId,
                        objectTitle = resolved.title,
                        objectType = obj.optString("objectType", ""),
                        fields = fields,
                        dependsOn = dependsOn,
                    )
                }
                "CREATE_RECORD" -> ActionItem.CreateRecord(
                    itemId = itemId,
                    summary = itemSummary,
                    objectType = obj.optString("objectType", "Record"),
                    domain = obj.optString("domain", "General"),
                    title = obj.optString("title", "New record"),
                    initialNotes = obj.optString("initialNotes", "").takeIf { it.isNotBlank() && it != "null" },
                    projectItemId = obj.optString("projectItemId", "").takeIf { it.isNotBlank() && it != "null" },
                    dependsOn = dependsOn,
                )
                "UPDATE_STATUS" -> {
                    val resolved = resolveObjectForAction(obj) ?: return null
                    ActionItem.UpdateStatus(
                        itemId = itemId,
                        summary = itemSummary,
                        objectId = resolved.objectId,
                        objectTitle = resolved.title,
                        newStatus = parseStatus(obj.optString("newStatus", "INACTIVE")),
                        dependsOn = dependsOn,
                    )
                }
                "CREATE_PROJECT" -> ActionItem.CreateProject(
                    itemId = itemId,
                    summary = itemSummary,
                    title = obj.optString("title", "New project"),
                    description = obj.optString("description", "").takeIf { it.isNotBlank() && it != "null" },
                    emoji = obj.optString("emoji", "🎯").takeIf { it.isNotBlank() && it != "null" } ?: "🎯",
                    domain = obj.optString("domain", "").takeIf { it.isNotBlank() && it != "null" },
                    dependsOn = dependsOn,
                )
                "CREATE_TASK" -> ActionItem.CreateTask(
                    itemId = itemId,
                    summary = itemSummary,
                    title = obj.optString("title", "New task"),
                    description = obj.optString("description", "").takeIf { it.isNotBlank() && it != "null" },
                    dueDate = obj.optString("dueDate", "").takeIf { it.isNotBlank() && it != "null" }
                        ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                    priority = runCatching {
                        TaskPriority.valueOf(obj.optString("priority", "MEDIUM").uppercase())
                    }.getOrDefault(TaskPriority.MEDIUM),
                    goalId = obj.optString("goalId", "").takeIf { it.isNotBlank() && it != "null" },
                    objectId = obj.optString("objectId", "").takeIf { it.isNotBlank() && it != "null" },
                    projectItemId = obj.optString("projectItemId", "").takeIf { it.isNotBlank() && it != "null" },
                    dependsOn = dependsOn,
                )
                "UPDATE_DOMAIN_UNDERSTANDING" -> ActionItem.UpdateDomainUnderstanding(
                    itemId = itemId,
                    summary = itemSummary,
                    domain = obj.optString("domain", "General"),
                    dependsOn = dependsOn,
                )
                else -> {
                    Timber.w("Unknown ActionItem type: $type")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse ActionItem")
            null
        }
    }

    private data class ResolvedObject(
        val objectId: String,
        val title: String,
    )

    /**
     * Maps free-form status strings returned by the AI to the canonical
     * [ObjectStatus] enum. Handles common synonyms so cancellations, endings,
     * and closures are not silently defaulted to INACTIVE (ISSUE-022).
     */
    private fun parseStatus(statusString: String): ObjectStatus {
        val normalized = statusString.uppercase().replace("[^A-Z0-9_]".toRegex(), "_")
        return runCatching {
            ObjectStatus.valueOf(normalized)
        }.getOrElse {
            when {
                statusString.contains("cancel", ignoreCase = true) -> ObjectStatus.INACTIVE
                statusString.contains("end", ignoreCase = true) -> ObjectStatus.INACTIVE
                statusString.contains("close", ignoreCase = true) -> ObjectStatus.INACTIVE
                statusString.contains("expire", ignoreCase = true) -> ObjectStatus.EXPIRED
                statusString.contains("renew", ignoreCase = true) -> ObjectStatus.RENEWAL_DUE
                statusString.contains("archive", ignoreCase = true) -> ObjectStatus.ARCHIVED
                statusString.contains("draft", ignoreCase = true) -> ObjectStatus.DRAFT
                else -> {
                    Timber.w("Unknown status in STATUS_UPDATE: $statusString; defaulting to INACTIVE")
                    ObjectStatus.INACTIVE
                }
            }
        }
    }

    private fun resolveObjectForAction(obj: JSONObject): ResolvedObject? {
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
        return ResolvedObject(resolvedObjectId, resolvedTitle)
    }

    private fun parseMetadataUpdate(obj: JSONObject, summary: String): AiProposal.MetadataUpdate? {
        val resolved = resolveObjectForAction(obj) ?: return null
        val objectType = obj.optString("objectType", "")
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
        val sensitivity = computeMetadataSensitivity(objectType, fields)
        return AiProposal.MetadataUpdate(
            proposalId = UUID.randomUUID().toString(),
            objectId = resolved.objectId,
            objectTitle = resolved.title,
            objectType = objectType,
            summary = summary.ifBlank { "Update suggested" },
            fields = fields,
            proposalSensitivity = sensitivity,
        )
    }

    private fun computeMetadataSensitivity(
        objectType: String,
        fields: List<ProposedField>,
    ): FieldSensitivity {
        val schema = schemaEngine.getSchema(objectType)
        val maxSensitivity = fields.map { field ->
            val fieldDef = schema?.fields?.find { it.fieldId.equals(field.fieldId, ignoreCase = true) }
            runCatching {
                FieldSensitivity.valueOf(fieldDef?.sensitivityLevel?.uppercase() ?: "STANDARD")
            }.getOrDefault(FieldSensitivity.STANDARD)
        }.maxByOrNull { it.ordinal } ?: FieldSensitivity.STANDARD
        return maxSensitivity
    }

    /**
     * Calls the AI provider with exponential-backoff retries and increasing read timeouts.
     * Updates [HomeUiState.aiStatusMessage] so the UI can show retry progress.
     * Never retries permanent failures (4xx client errors except 429).
     */
    private suspend fun executeWithRetries(
        provider: AiProvider,
        systemPrompt: String,
        userMessage: String,
        conversationHistory: List<AiMessage>,
    ): AiCompletionResult {
        val timeouts = listOf(60L, 90L, 120L)
        val delaysMs = listOf(0L, 5_000L, 15_000L)
        val statusMessages = listOf("Thinking…", "Taking longer than usual, retrying…", "Still working on it…")

        var lastResult: AiCompletionResult = AiCompletionResult.Error("No attempts made")

        for (attempt in 0 until timeouts.size) {
            _uiState.update { it.copy(aiStatusMessage = statusMessages[attempt]) }

            if (delaysMs[attempt] > 0) {
                delay(delaysMs[attempt])
            }

            lastResult = try {
                provider.complete(
                    systemPrompt = systemPrompt,
                    userMessage = userMessage,
                    conversationHistory = conversationHistory,
                    readTimeoutSeconds = timeouts[attempt],
                )
            } catch (e: Exception) {
                Timber.w(e, "AI attempt ${attempt + 1} failed with exception")
                AiCompletionResult.Error(message = e.message ?: "Request failed")
            }

            when (lastResult) {
                is AiCompletionResult.Success -> {
                    _uiState.update { it.copy(aiStatusMessage = null) }
                    return lastResult
                }
                is AiCompletionResult.Unavailable -> {
                    _uiState.update { it.copy(aiStatusMessage = null) }
                    return lastResult
                }
                is AiCompletionResult.Error -> {
                    if (!isRetryable(lastResult)) {
                        _uiState.update { it.copy(aiStatusMessage = null) }
                        return lastResult
                    }
                    Timber.w("AI attempt ${attempt + 1} failed, will retry if attempts remain: ${lastResult.message}")
                }
            }
        }

        _uiState.update { it.copy(aiStatusMessage = null) }
        return lastResult
    }

    private fun isRetryable(result: AiCompletionResult.Error): Boolean {
        return when (result.code) {
            400, 401, 403, 404 -> false
            else -> true // timeout, 5xx, 429, network errors, unknown codes
        }
    }

    private fun ActionItem.isRecordUpdate(): Boolean = when (this) {
        is ActionItem.UpdateRecord,
        is ActionItem.UpdateStatus,
        is ActionItem.UpdateDomainUnderstanding -> true
        is ActionItem.CreateProject,
        is ActionItem.CreateRecord,
        is ActionItem.CreateTask -> false
    }

    private fun ActionItem.isObjectCreation(): Boolean = this is ActionItem.CreateRecord

    private fun ActionItem.isTaskCreation(): Boolean = this is ActionItem.CreateTask

    private fun mapActionType(actionType: String): AttentionType = when {
        actionType.contains("expir", ignoreCase = true) -> AttentionType.EXPIRING
        actionType.contains("verif", ignoreCase = true) -> AttentionType.PENDING_VERIFICATION
        actionType.contains("goal", ignoreCase = true) -> AttentionType.GOAL
        actionType.contains("task", ignoreCase = true) -> AttentionType.TASK
        else -> AttentionType.SUGGESTION
    }

    /**
     * Returns true if the current conversation has been idle longer than the
     * auto-reset threshold, or if no current conversation exists.
     */
    private fun shouldStartNewConversation(): Boolean {
        val currentId = _uiState.value.currentConversationId ?: return true
        val conversation = _uiState.value.allConversations
            .firstOrNull { it.conversationId == currentId }
            ?: return true
        val idleMillis = java.time.Duration.between(conversation.updatedAt, Instant.now()).toMillis()
        return idleMillis > CONVERSATION_IDLE_THRESHOLD_MS
    }

    private fun timeBasedGreeting(): String = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }

    companion object {
        // Four hours. If a user returns to the AI workspace after this idle
        // period, the next message starts a new conversation.
        private const val CONVERSATION_IDLE_THRESHOLD_MS = 4 * 60 * 60 * 1000L

        private val ACTION_PATTERN = Regex(
            "\\[LIFEPILOT_ACTION\\](.*?)\\[/LIFEPILOT_ACTION\\]",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val ASK_PATTERN = Regex(
            "\\[ASK\\](.*?)\\[/ASK\\]",
            RegexOption.DOT_MATCHES_ALL,
        )
    }
}
