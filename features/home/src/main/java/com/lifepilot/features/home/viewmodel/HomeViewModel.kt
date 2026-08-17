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
import com.lifepilot.domain.engine.RetrievalPlanner
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.ocr.OcrService
import com.lifepilot.domain.usecase.UploadDocumentUseCase
import com.lifepilot.domain.model.ActionItem
import com.lifepilot.domain.model.ActionPlan
import com.lifepilot.domain.model.ActionPlanNormalizer
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
    private val retrievalPlanner: RetrievalPlanner,
    private val aiActionParser: AiActionParser,
    private val aiTaskNotifier: com.lifepilot.data.notification.AiTaskNotifier,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Kept current after each AI request for post-response parseAction() resolution.
    // RetrievalEngine populates these; HomeViewModel reads them — never writes them.
    private var currentObjectIndex: Map<String, Pair<String, String>> = emptyMap()
    private var currentObjectMetadataIndex: Map<String, List<com.lifepilot.domain.model.MetadataEntry>> = emptyMap()

    // OCR text of the queued (not-yet-sent) attachment, extracted in the background on scan so it's
    // ready to travel with the user's first message. Cleared once the attachment is resolved.
    private var pendingAttachmentOcrText: String? = null

    // Classification computed in the background on scan (OCR → AI) but NOT stored — held here so it's
    // ready to be presented for save when the user sends their first message. Null if not classified.
    private var pendingClassifiedAction: AiProposal? = null
    // Domains and snapshots active in the last retrieval — used to focus domain life state updates.
    private var currentRetrievalDomains: List<String> = emptyList()
    // objectId → domain lookup, built from all objects in the retrieval context.
    private var currentObjectDomainIndex: Map<String, String> = emptyMap()

    private var activeAiJob: Job? = null

    // System prompt from the most recent sendMessage call — reused verbatim by plan-batch
    // continuations so each batch sees the same life context without rebuilding retrieval.
    private var lastSystemPrompt: String = ""

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
                        projectRepository.observeProjects(profile.profileId)
                            .onStart { emit(emptyList()) }
                            .catch { e -> Timber.e(e, "Error observing projects"); emit(emptyList()) },
                    ) { goals, conversations, attentionItems, projects ->
                        BriefData(profile.displayName, profile.profileId, goals, conversations, attentionItems, projects)
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
                    val activeProjects = data.projects
                        .filter { it.status == com.lifepilot.domain.model.ProjectStatus.ACTIVE }
                        .take(4)
                    _uiState.update { state ->
                        state.copy(
                            profileName = data.profileName,
                            isLoadingBrief = false,
                            activeProjects = activeProjects,
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
        val projects: List<com.lifepilot.domain.model.Project>,
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

                // If a document is queued but the background classify hasn't set its context yet
                // (fast send / OCR still running), ensure OCR is done and attach a minimal context
                // so the AI still reads the document alongside this message.
                // Capture all three attachment fields NOW — state is cleared when the chip is pinned
                // to the message bubble (below), so reading from state after that returns null.
                val pendingPath = _uiState.value.pendingAttachmentPath
                val pendingDisplayName = _uiState.value.pendingAttachmentDisplayName
                val pendingMimeType = _uiState.value.pendingAttachmentMimeType
                if (pendingPath != null && _uiState.value.attachedDocumentContext == null) {
                    if (pendingAttachmentOcrText == null) {
                        val ocr = runCatching {
                            ocrService.extractText(
                                pendingPath,
                                _uiState.value.pendingAttachmentMimeType ?: "application/octet-stream",
                            )
                        }.getOrNull()
                        pendingAttachmentOcrText = (ocr as? com.lifepilot.domain.ocr.OcrResult.Success)?.text
                    }
                    _uiState.update {
                        it.copy(
                            attachedDocumentContext = AttachedDocumentContext(
                                fileName = _uiState.value.pendingAttachmentDisplayName ?: "attachment",
                                mimeType = _uiState.value.pendingAttachmentMimeType ?: "application/octet-stream",
                                ocrText = pendingAttachmentOcrText,
                                isPendingApproval = true,
                            ),
                        )
                    }
                }

                // Pin the attachment to THIS message so its chip stays anchored here instead of
                // floating to the bottom of the conversation as later messages arrive. Its context is
                // still passed to the AI on this turn (below) and cleared afterwards so it is not
                // re-injected into every subsequent request.
                _uiState.value.attachedDocumentContext?.let { attached ->
                    if (pendingPath != null) {
                        _uiState.update { s ->
                            s.copy(
                                attachedDocumentByMessageId = s.attachedDocumentByMessageId + (userMessageId to attached),
                                // Clear the input-bar chip immediately on Send — the document is now
                                // pinned to the message bubble, not floating in the input bar.
                                pendingAttachmentPath = null,
                                pendingAttachmentDisplayName = null,
                                pendingAttachmentMimeType = null,
                            )
                        }
                    }
                }

                // Step 1: retrieval planning — ask AI which domains are relevant (lightweight).
                // Read documentType now for the planning call; attachedContext is re-read
                // AFTER planning so any background OCR/classify that finishes during the
                // planning round-trip is captured rather than a stale null.
                _uiState.update { it.copy(aiStatusMessage = "Looking up your records…") }
                val summary = retrievalEngine.getSummary(profileId)
                val relevantDomains = runCatching {
                    retrievalPlanner.planDomains(
                        userQuery = text,
                        documentType = _uiState.value.attachedDocumentContext?.objectType,
                        summary = summary,
                    )
                }.getOrElse { emptyList() }.ifEmpty { null } // null = load all (fallback)

                // Step 2: targeted retrieval — read attachedContext fresh so any OCR that
                // completed during the planning call is included.
                val attachedContext = _uiState.value.attachedDocumentContext
                val retrievalContext = retrievalEngine.retrieve(
                    profileId = profileId,
                    userQuery = text,
                    relevantDomains = relevantDomains,
                ).copy(attachedDocumentContext = attachedContext)
                currentObjectIndex = retrievalContext.allObjectIndex
                currentObjectDomainIndex = retrievalContext.allObjectDomainIndex
                currentObjectMetadataIndex = retrievalContext.allObjectMetadata
                currentRetrievalDomains = retrievalContext.relevantSnapshots.map { it.domain }.distinct()

                // Step 3: build the prompt from targeted context.
                _uiState.update { it.copy(aiStatusMessage = "Building context…") }
                val systemPrompt = promptBuilder.build(retrievalContext, userQuery = text)
                lastSystemPrompt = systemPrompt

                val history = _uiState.value.messages
                    .dropLast(1)
                    .filter { it.conversationId == conversation.conversationId }
                    .map { msg ->
                        AiMessage(
                            role = if (msg.role == "USER") AiMessageRole.USER else AiMessageRole.ASSISTANT,
                            content = msg.content,
                        )
                    }

                // Keep the process alive (foreground service) so the request survives the user
                // switching apps; we notify when the answer lands if they're away.
                aiTaskNotifier.onTurnStarted()

                val provider = aiProviderFactory.getProvider()
                val result = completeWithContinuation(
                    provider = provider,
                    systemPrompt = systemPrompt,
                    userMessage = text,
                    history = history,
                )

                when (result) {
                    is AiCompletionResult.Success -> {
                        Timber.d("AI raw response: ${result.content}")
                        val (visibleContent, rawAction, question) = parseAiResponse(result.content)
                        // If the AI returned a partial ACTION_PLAN (has_more=true), silently fetch the
                        // remaining batches and merge them before doing anything else. The user sees
                        // "Building your plan…" the whole time and gets ONE consolidated plan to review.
                        val resolvedRawAction = if (rawAction is AiProposal.ActionPlan && rawAction.plan.hasMore) {
                            resolvePlanBatches(rawAction, provider)
                        } else {
                            rawAction
                        }
                        // If a document is queued and the AI classified it, attach the file to the
                        // proposal so approving it saves the PDF against the new record.
                        // Use the pre-captured values — pendingAttachmentPath/DisplayName/MimeType were
                        // cleared from state when the chip was pinned to the message bubble above.
                        val action = if (pendingPath != null && resolvedRawAction is AiProposal.ObjectCreation) {
                            resolvedRawAction.copy(
                                attachedFilePath = pendingPath,
                                attachedFileName = pendingDisplayName,
                                attachedMimeType = pendingMimeType,
                            )
                        } else {
                            resolvedRawAction
                        }
                        Timber.d("AI parsed: action=${action?.javaClass?.simpleName}, question=$question")
                        // When the AI response is entirely an action block with no surrounding text,
                        // visibleContent is blank. Use the action summary so no empty bubble appears.
                        val displayContent = visibleContent.ifBlank {
                            action?.summary?.takeIf { it.isNotBlank() } ?: run {
                                // Nothing to show and no action — stop the spinner instead of
                                // leaving it running forever on an empty AI response (B7).
                                _uiState.update { it.copy(isAiLoading = false, aiStatusMessage = null) }
                                return@launch
                            }
                        }
                        val assistantMsg = StoredMessage(
                            messageId = UUID.randomUUID().toString(),
                            conversationId = conversation.conversationId,
                            role = "ASSISTANT",
                            content = displayContent,
                            timestamp = Instant.now(),
                        )
                        conversationRepository.saveMessage(assistantMsg)

                        // Answer is committed — stop the keep-alive service and, if the user is away,
                        // post the "answer ready" notification that deep-links to this conversation.
                        aiTaskNotifier.onTurnFinished(conversation.conversationId, _uiState.value.conversationTitle)

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

                        // Resolve the queued document now that the AI has read it and answered.
                        // Prefer a fresh classification from this turn; else the one computed on scan.
                        // Use pendingPath (captured before state was cleared) — reading from state here
                        // returns null because the chip-pin update already cleared it.
                        if (pendingPath != null) {
                            val saveProposal = (action as? AiProposal.ObjectCreation)
                                ?: (pendingClassifiedAction as? AiProposal.ObjectCreation)
                            if (saveProposal != null) {
                                // Present the classification for the user to approve and save.
                                // Ensure file path is set regardless of which proposal we use.
                                val proposalWithFile = if (saveProposal.attachedFilePath.isNullOrBlank()) {
                                    saveProposal.copy(
                                        attachedFilePath = pendingPath,
                                        attachedFileName = pendingDisplayName ?: saveProposal.attachedFileName,
                                        attachedMimeType = pendingMimeType ?: saveProposal.attachedMimeType,
                                    )
                                } else saveProposal
                                _uiState.update { it.copy(pendingAction = proposalWithFile) }
                            } else {
                                // Couldn't classify — save as a general document so it isn't lost.
                                val pName = pendingDisplayName ?: "attachment"
                                val pMime = pendingMimeType ?: "application/octet-stream"
                                runCatching {
                                    createPlaceholderObject(profileId, conversation.conversationId, pName, pendingPath, pMime)
                                }.onFailure { Timber.w(it, "Failed to save queued document") }
                            }
                            pendingClassifiedAction = null
                            clearPendingAttachment()
                            _uiState.update { it.copy(attachedDocumentContext = null) }
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
                        Timber.w("AI error: ${result.message}")
                        val errorMsg = StoredMessage(
                            messageId = UUID.randomUUID().toString(),
                            conversationId = conversation.conversationId,
                            role = "ASSISTANT",
                            content = friendlyAiError(result.message),
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
                // Safety net: always release the keep-alive service (idempotent; onTurnFinished may
                // have already stopped it and posted the result notification).
                aiTaskNotifier.stopThinking()
            }
        }
    }

    fun abortAi() {
        activeAiJob?.cancel()
        activeAiJob = null
        aiTaskNotifier.stopThinking()
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

    /**
     * Queues a scanned/captured/picked document as an attachment chip in the chat. It is NOT
     * classified or stored here — OCR runs in the background so the text is ready, then the document
     * travels with the user's first message ([sendMessage]) where the AI reads it, answers, and
     * (on approval) it is saved. This is the "document sits in chat until you send" flow.
     */
    fun processAttachment(uri: Uri) {
        viewModelScope.launch {
            val profileId = preferenceManager.getActiveProfileId() ?: return@launch

            val conversation = conversationRepository.getOrCreateConversation(
                profileId = profileId,
                conversationId = _uiState.value.currentConversationId,
            )
            val convId = conversation.conversationId

            _uiState.update {
                it.copy(
                    currentConversationId = convId,
                    conversationTitle = conversation.title,
                    mode = HomeMode.AI_WORKSPACE,
                    aiStatusMessage = "Reading document…",
                )
            }

            val fileName = resolveUriFileName(uri) ?: "attachment"
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val storedFile = fileStorageManager.copyFromUri(uri, "chat_import", fileName)

            if (storedFile == null) {
                _uiState.update { it.copy(aiStatusMessage = null) }
                postSystemMessage(convId, "Sorry, I couldn't read that attachment.")
                return@launch
            }

            // Show the attachment chip immediately; storage waits for the first message.
            _uiState.update {
                it.copy(
                    pendingAttachmentPath = storedFile.absolutePath,
                    pendingAttachmentDisplayName = fileName,
                    pendingAttachmentMimeType = mimeType,
                    aiStatusMessage = "Reading document…",
                )
            }

            // OCR + classify in the background so the document is already understood by the time the
            // user sends their first message — but nothing is stored yet.
            val ocr = runCatching { ocrService.extractText(storedFile.absolutePath, mimeType) }.getOrNull()
            val ocrText = (ocr as? com.lifepilot.domain.ocr.OcrResult.Success)?.text
            pendingAttachmentOcrText = ocrText

            var classified: AiProposal.ObjectCreation? = null
            if (!ocrText.isNullOrBlank()) {
                val prompt = buildAttachmentClassificationPrompt(ocrText, fileName)
                val provider = runCatching { aiProviderFactory.getProvider() }.getOrNull()
                val result = provider?.let {
                    runCatching { it.complete(prompt, "Classify and extract this document.", emptyList()) }.getOrNull()
                }
                val content = (result as? AiCompletionResult.Success)?.content
                val actionJson = content?.let {
                    Regex("\\[LIFEPILOT_ACTION\\](.*?)\\[/LIFEPILOT_ACTION\\]", RegexOption.DOT_MATCHES_ALL)
                        .find(it)?.groupValues?.get(1)
                }
                classified = actionJson
                    ?.let { runCatching { aiActionParser.parseAction(it.trim(), currentObjectIndex, currentObjectMetadataIndex) }.getOrNull() }
                    as? AiProposal.ObjectCreation
            }

            pendingClassifiedAction = classified?.copy(
                attachedFilePath = storedFile.absolutePath,
                attachedFileName = fileName,
                attachedMimeType = mimeType,
            )

            _uiState.update {
                it.copy(
                    aiStatusMessage = null,
                    attachedDocumentContext = AttachedDocumentContext(
                        fileName = fileName,
                        mimeType = mimeType,
                        objectType = classified?.objectType,
                        domain = classified?.domain,
                        title = classified?.title,
                        extractedFields = classified?.fields ?: emptyList(),
                        ocrText = ocrText,
                        // If we couldn't classify now, let the first send re-attempt it.
                        isPendingApproval = classified == null,
                    ),
                )
            }
        }
    }

    fun clearPendingAttachment() {
        pendingAttachmentOcrText = null
        _uiState.update {
            it.copy(
                pendingAttachmentPath = null,
                pendingAttachmentDisplayName = null,
                pendingAttachmentMimeType = null,
            )
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
            appendLine("The block MUST contain a SINGLE JSON object with an \"actionType\" field — no text before the '{'.")
            appendLine("Example: [LIFEPILOT_ACTION]{\"actionType\":\"OBJECT_CREATION\",\"objectType\":\"pan_card\",\"domain\":\"Identity\",\"title\":\"PAN Card\",\"summary\":\"Found a PAN card\",\"fields\":[{\"fieldId\":\"pan_number\",\"displayName\":\"PAN\",\"value\":\"ABCDE1234F\"}]}[/LIFEPILOT_ACTION]")
            appendLine("If this is a new document, use actionType OBJECT_CREATION with objectType, domain, title, and fields.")
            appendLine("If it updates an existing record, use METADATA_UPDATE with objectType, matchField, matchValue, and fields.")
            appendLine("IMPORTANT: You MUST use one of the exact objectType strings from the available record types list above.")
            appendLine("If the document does not match any known type, use objectType 'Certificate' and describe the document type in the initialNotes field so the user understands what was saved.")
            appendLine("NEVER invent a new objectType that is not in the available record types list.")
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

    fun approveEditedAction(editedProposal: AiProposal) {
        _uiState.update { it.copy(pendingAction = null, attachedDocumentContext = null) }
        viewModelScope.launch {
            try {
                val profileId = preferenceManager.getActiveProfileId() ?: return@launch
                val convId = _uiState.value.currentConversationId ?: return@launch

                when (editedProposal) {
                    is AiProposal.ActionPlan -> executeActionPlan(editedProposal.plan, convId, profileId)
                    else -> executeSingleProposal(editedProposal, convId, profileId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to execute edited AI proposal")
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
                    val existing = metadataRepository.getMetadataByField(proposal.objectId, field.fieldId)?.value
                    val finalValue = com.lifepilot.domain.model.MetadataMerge.merge(field.mode, existing, field.value)
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
                    projectId = proposal.projectId,
                ).getOrThrow()
                val projectSuffix = if (proposal.projectId != null) " (added to project)" else ""
                "Task \"${proposal.title}\" added to your Planner.$projectSuffix"
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
                // Enter the Life State Engine so the AI-created record generates tasks + reminders,
                // exactly like the manual create path (CreateObjectUseCase) — nothing bypasses it.
                runCatching {
                    lifeStateEngine.processObjectEvent(
                        objectId = createdObject.objectId,
                        eventType = "OBJECT_CREATED",
                        payload = "{\"objectType\":\"${proposal.objectType}\"}",
                    )
                }.onFailure { Timber.w(it, "Life State Engine event failed for ${createdObject.objectId}") }
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
                    emoji = proposal.emoji?.takeIf { it.isNotBlank() }
                        ?: com.lifepilot.domain.model.DomainEmoji.forDomain(proposal.domain),
                    targetDate = proposal.targetDate,
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
                attachedDocumentByMessageId = emptyMap(),
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
                attachedDocumentByMessageId = emptyMap(),
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
                attachedDocumentByMessageId = emptyMap(),
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

    /** Maps a raw provider error to a calm, user-facing message. The raw text is logged separately. */
    private fun friendlyAiError(message: String): String {
        val m = message.lowercase()
        return when {
            m.contains("timeout") || m.contains("etimedout") || m.contains("failed to connect") ||
                m.contains("unable to resolve host") || m.contains("connection") ->
                "I couldn't reach the AI service — check your connection and tap Retry."
            m.contains("401") || m.contains("403") || m.contains("api key") || m.contains("unauthorized") ->
                "Your AI key was rejected. Check it in Settings → Intelligence."
            m.contains("429") || m.contains("rate") ->
                "The AI service is busy right now. Wait a moment and tap Retry."
            else -> "Something went wrong. Tap Retry to try again."
        }
    }

    private fun parseAiResponse(raw: String): ParsedResponse {
        var content = raw
        var action: AiProposal? = null

        // Extract the action block FIRST, from the raw string, before any text normalisation — so
        // the block's contents (which contain underscores like LIFEPILOT_ACTION / ACTION_PLAN) can
        // never be corrupted by downstream processing. Visible Markdown is rendered by MessageBubble.
        val actionMatch = ACTION_PATTERN.find(raw)
        if (actionMatch != null) {
            content = content.replace(actionMatch.value, "")
            // Delegate to the shared, unit-tested AiActionParser (single source of truth). The
            // retrieval indices let it resolve object references for UPDATE/STATUS actions.
            action = runCatching {
                aiActionParser.parseAction(
                    actionMatch.groupValues[1].trim(),
                    currentObjectIndex,
                    currentObjectMetadataIndex,
                )
            }.getOrNull()
        }

        // Safety net: a very long plan can exceed the model's output limit and truncate mid-JSON,
        // leaving an OPENING [LIFEPILOT_ACTION] with no closing tag. ACTION_PATTERN then can't match
        // and the raw JSON would leak into the chat (with underscores eaten by stripMarkdown). Strip
        // any dangling opener and surface a friendly retry hint instead of junk.
        var truncatedAction = false
        if (action == null) {
            val openIdx = content.indexOf("[LIFEPILOT_ACTION]")
            if (openIdx >= 0) {
                content = content.substring(0, openIdx).trim()
                truncatedAction = true
            }
        }

        // Deterministic formatting: split run-on numbered lists onto their own lines. Markdown
        // emphasis (**bold**, *italic*, # headers) is intentionally KEPT here — MessageBubble renders
        // it via toDisplayAnnotatedString. The action block was already removed above, so its
        // underscores are safe.
        content = content.normalizeNumberedList().trim()

        if (truncatedAction && content.isBlank()) {
            content = "That plan was longer than I could finish in one go. Tap Retry (or ask me to " +
                "keep it shorter) and I'll set it up."
        }

        // [ASK] tags contain no underscores so they survive stripMarkdown() intact.
        val askMatch = ASK_PATTERN.find(content)
        if (askMatch != null) {
            val questionText = askMatch.groupValues[1].trim()
            content = content.replace(askMatch.value, questionText).trim()
        }

        return ParsedResponse(content.trim(), action, question = null)
    }

    /**
     * Puts each "N. " item of a run-on numbered list on its own line. Models sometimes emit an entire
     * plan as one paragraph (observed on long marriage/tax plans). Only reformats when there are 2+
     * inline markers so ordinary prose (e.g. "by 2027", "version 2.0") is left untouched.
     */
    private fun String.normalizeNumberedList(): String {
        val markers = Regex("(?<=\\s)\\d{1,2}\\.\\s").findAll(this).count()
        if (markers < 2) return this
        return this.replace(Regex("(\\S)[ \\t]+(\\d{1,2})\\.\\s+(?=[A-Za-z])")) { m ->
            "${m.groupValues[1]}\n${m.groupValues[2]}. "
        }
    }

    /**
     * Silently fetches subsequent plan batches when the AI set has_more=true on the first batch.
     *
     * Each batch is ≤8 items so it reliably fits in one AI response. We loop up to [MAX_PLAN_BATCHES]
     * times, then merge all items and apply [ActionPlanNormalizer.ensureProject] exactly once on the
     * combined list. The caller gets a single, complete [AiProposal.ActionPlan] — the user sees
     * "Building your plan…" throughout and one consolidated plan card at the end.
     */
    private suspend fun resolvePlanBatches(
        firstBatch: AiProposal.ActionPlan,
        provider: AiProvider,
    ): AiProposal.ActionPlan {
        // Collect items from each batch, stripping any per-batch CreateProject (ensureProject was
        // skipped for has_more batches in the parser; we apply it once at the end on the merged list).
        val allItems = mutableListOf<ActionItem>()
        allItems.addAll(firstBatch.plan.items.filterNot { it is ActionItem.CreateProject })

        var current = firstBatch
        var batchCount = 1

        while (current.plan.hasMore && batchCount < MAX_PLAN_BATCHES) {
            val context = current.plan.continuationContext ?: break
            _uiState.update { it.copy(aiStatusMessage = "Building your plan… (part ${batchCount + 1})") }

            val continuationMsg = "Continue the action plan. Emit ONLY the next batch of tasks covering: $context. " +
                "Use the same ACTION_PLAN JSON format. Max 8 items. Set has_more: true if still more remain after this batch."

            val batchResult = executeWithRetries(
                provider = provider,
                systemPrompt = lastSystemPrompt,
                userMessage = continuationMsg,
                conversationHistory = emptyList(),
            )

            if (batchResult !is AiCompletionResult.Success) break

            val (_, batchAction, _) = parseAiResponse(batchResult.content)
            val nextBatch = batchAction as? AiProposal.ActionPlan ?: break

            allItems.addAll(nextBatch.plan.items.filterNot { it is ActionItem.CreateProject })
            current = nextBatch
            batchCount++
        }

        val mergedPlan = ActionPlanNormalizer.ensureProject(
            firstBatch.plan.copy(
                items = allItems,
                hasMore = false,
                continuationContext = null,
            )
        )
        return firstBatch.copy(plan = mergedPlan)
    }

    /**
     * Calls the AI provider with exponential-backoff retries and increasing read timeouts.
     * Updates [HomeUiState.aiStatusMessage] so the UI can show retry progress.
     * Never retries permanent failures (4xx client errors except 429).
     */
    /**
     * Runs the AI request and, if the model truncated its output (hit max_tokens), transparently asks
     * it to continue and stitches the parts together. The user sees ONE complete answer assembled from
     * up to [MAX_CONTINUATIONS] + 1 chunks — not a cut-off response with raw JSON. Bounded so a
     * pathological model can't loop forever; if still incomplete after the cap, the caller's
     * parse-time safety net strips any dangling action block and offers a retry.
     */
    private suspend fun completeWithContinuation(
        provider: AiProvider,
        systemPrompt: String,
        userMessage: String,
        history: List<AiMessage>,
    ): AiCompletionResult {
        val first = executeWithRetries(provider, systemPrompt, userMessage, history)
        if (first !is AiCompletionResult.Success || !first.truncated) return first

        var assembled = first.content
        var model = first.model
        var continuations = 0
        while (continuations < MAX_CONTINUATIONS) {
            _uiState.update { it.copy(aiStatusMessage = "Writing the rest…") }
            val contHistory = history +
                AiMessage(role = AiMessageRole.USER, content = userMessage) +
                AiMessage(role = AiMessageRole.ASSISTANT, content = assembled)
            val next = executeWithRetries(
                provider = provider,
                systemPrompt = systemPrompt,
                userMessage = "Continue your previous message from exactly where it stopped. Do not repeat " +
                    "any text you already sent — output only the remaining part, and finish it completely.",
                conversationHistory = contHistory,
            )
            if (next !is AiCompletionResult.Success) {
                // Keep what we have; still-truncated flag lets the safety net handle it gracefully.
                return AiCompletionResult.Success(assembled, model, truncated = true)
            }
            assembled += next.content
            model = next.model
            continuations++
            if (!next.truncated) return AiCompletionResult.Success(assembled, model, truncated = false)
        }
        return AiCompletionResult.Success(assembled, model, truncated = true)
    }

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

        // Max automatic "continue" round-trips when the model truncates a long response
        // (so a big plan is assembled from up to 3 chunks total before parsing/display).
        private const val MAX_CONTINUATIONS = 2

        // Safety cap on the number of has_more plan batches to fetch before giving up.
        // 5 batches × 8 tasks = 40 tasks maximum — far more than any realistic life event plan.
        private const val MAX_PLAN_BATCHES = 5

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
