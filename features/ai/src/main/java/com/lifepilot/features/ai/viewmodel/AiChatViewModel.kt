package com.lifepilot.features.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.data.ai.AiProviderFactory
import com.lifepilot.domain.ai.AiCompletionResult
import com.lifepilot.domain.ai.AiMessage
import com.lifepilot.domain.ai.AiMessageRole
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.repository.ReminderRepository
import com.lifepilot.domain.repository.TaskRepository
import com.lifepilot.features.ai.state.AiChatState
import com.lifepilot.features.ai.state.ChatMessage
import com.lifepilot.features.ai.state.MessageRole
import com.lifepilot.features.ai.state.ProposedAction
import com.lifepilot.features.ai.state.ProposedField
import com.lifepilot.features.ai.state.UpdateMode
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
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val objectRepository: ObjectRepository,
    private val metadataRepository: MetadataRepository,
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val aiProviderFactory: AiProviderFactory,
) : ViewModel() {

    private val _state = MutableStateFlow(AiChatState())
    val state: StateFlow<AiChatState> = _state.asStateFlow()

    private var cachedSystemPrompt: String? = null
    // objectId -> (title, type) for action matching
    private var objectIndex: Map<String, Pair<String, String>> = emptyMap()
    // objectId -> metadata entries for field-based matching
    private var objectMetadataIndex: Map<String, List<com.lifepilot.domain.model.MetadataEntry>> = emptyMap()

    init {
        viewModelScope.launch {
            val configured = aiProviderFactory.isConfigured()
            _state.update { it.copy(isConfigured = configured) }
        }
    }

    fun onInputChange(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank()) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = text,
        )

        _state.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                inputText = "",
                isLoading = true,
                error = null,
                pendingAction = null,
                pendingContextQuestion = null,
            )
        }

        viewModelScope.launch {
            try {
                val systemPrompt = cachedSystemPrompt ?: buildSystemPrompt().also { cachedSystemPrompt = it }
                val history = _state.value.messages
                    .dropLast(1)
                    .map { msg ->
                        AiMessage(
                            role = when (msg.role) {
                                MessageRole.USER -> AiMessageRole.USER
                                MessageRole.ASSISTANT -> AiMessageRole.ASSISTANT
                                MessageRole.SYSTEM -> AiMessageRole.SYSTEM
                            },
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
                        val rawContent = result.content
                        val (visibleContent, action, question) = parseAiResponse(rawContent)
                        val assistantMessage = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            role = MessageRole.ASSISTANT,
                            content = visibleContent,
                        )
                        _state.update { state ->
                            state.copy(
                                messages = state.messages + assistantMessage,
                                isLoading = false,
                                isConfigured = true,
                                pendingAction = action,
                                pendingContextQuestion = question,
                            )
                        }
                    }
                    is AiCompletionResult.Error -> {
                        val assistantMessage = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            role = MessageRole.ASSISTANT,
                            content = "Error: ${result.message}",
                        )
                        _state.update { state ->
                            state.copy(messages = state.messages + assistantMessage, isLoading = false)
                        }
                    }
                    is AiCompletionResult.Unavailable -> {
                        val assistantMessage = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            role = MessageRole.ASSISTANT,
                            content = "AI provider not configured. Go to Settings → AI Provider to set it up.",
                        )
                        _state.update { state ->
                            state.copy(messages = state.messages + assistantMessage, isLoading = false, isConfigured = false)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "AI query failed")
                _state.update { it.copy(isLoading = false, error = "Request failed: ${e.message}") }
            }
        }
    }

    fun approveAction() {
        val action = _state.value.pendingAction ?: return
        _state.update { it.copy(pendingAction = null) }
        viewModelScope.launch {
            try {
                for (field in action.fields) {
                    val existing = if (field.mode == UpdateMode.APPEND) {
                        metadataRepository.getMetadataByField(action.objectId, field.fieldId)?.value
                    } else null
                    val finalValue = if (existing != null && field.mode == UpdateMode.APPEND) {
                        "$existing\n${field.value}"
                    } else {
                        field.value
                    }
                    metadataRepository.upsertMetadata(
                        objectId = action.objectId,
                        fieldId = field.fieldId,
                        value = finalValue,
                        source = MetadataSource.AI_EXTRACTED,
                        confidence = 0.9f,
                    )
                }
                val confirmMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.ASSISTANT,
                    content = "Saved to ${action.objectTitle}.",
                )
                _state.update { it.copy(messages = it.messages + confirmMessage) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to apply AI action")
            }
        }
    }

    fun dismissAction() {
        _state.update { it.copy(pendingAction = null) }
    }

    fun dismissContextQuestion() {
        _state.update { it.copy(pendingContextQuestion = null) }
    }

    private data class ParsedResponse(
        val visibleContent: String,
        val action: ProposedAction?,
        val question: String?,
    )

    private fun parseAiResponse(raw: String): ParsedResponse {
        var content = raw
        var action: ProposedAction? = null
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

    private fun parseAction(json: String): ProposedAction? {
        return try {
            val obj = JSONObject(json)
            val objectType = obj.optString("objectType", "")
            val matchField = obj.optString("matchField", "")
            val matchValue = obj.optString("matchValue", "")
            val summary = obj.optString("summary", "Update suggested")

            // Collect all objects of the right type
            val candidateIds = objectIndex.entries
                .filter { (_, v) -> v.second.equals(objectType, ignoreCase = true) }
                .map { it.key }

            val resolvedObjectId = when {
                candidateIds.isEmpty() -> return null
                // Single match — no need to check field
                candidateIds.size == 1 -> candidateIds.first()
                // Multiple matches — try to pick the right one via matchField+matchValue
                matchField.isNotBlank() && matchValue.isNotBlank() -> {
                    candidateIds.firstOrNull { id ->
                        // Check object title
                        val title = objectIndex[id]?.first ?: ""
                        title.contains(matchValue, ignoreCase = true) ||
                            // Check metadata fields
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
            ProposedAction(
                id = UUID.randomUUID().toString(),
                objectId = resolvedObjectId,
                objectTitle = resolvedTitle,
                objectType = objectType,
                summary = summary,
                fields = fields,
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse LIFEPILOT_ACTION block")
            null
        }
    }

    private suspend fun buildSystemPrompt(): String {
        val profile = profileRepository.observeActiveProfile()
            .catch { }
            .firstOrNull()

        val allObjects = profile?.let {
            objectRepository.observeObjectsByProfile(it.profileId)
                .catch { }
                .firstOrNull()
                ?: emptyList()
        } ?: emptyList()
        val objects = allObjects.take(30)

        val metadataByObject = runCatching {
            metadataRepository.getMetadataForObjects(objects.map { it.objectId })
        }.getOrElse { emptyMap() }

        // Build indexes for later action resolution
        objectIndex = objects.associate { it.objectId to (it.title to it.objectType) }
        objectMetadataIndex = metadataByObject

        val pendingTasks = profile?.let {
            taskRepository.observePendingTasks(it.profileId)
                .catch { }
                .firstOrNull()
                ?: emptyList()
        } ?: emptyList()

        val upcomingReminders = runCatching {
            reminderRepository.observeUpcomingReminders(
                java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS)
            ).catch { }.firstOrNull() ?: emptyList()
        }.getOrElse { emptyList() }

        return buildString {
            appendLine("You are the LifePilot AI assistant. You help users manage their administrative life.")
            appendLine("You have access to the user's structured life data below. Answer questions based ONLY on this data.")
            appendLine("Be concise, practical, and focused on actionable insights.")
            appendLine("Today's date: ${java.time.LocalDate.now()}")
            appendLine()
            appendLine("IMPORTANT — DETECTING LIFE EVENTS:")
            appendLine("When the user mentions ANY life event that affects a tracked record, you MUST:")
            appendLine("1. Respond helpfully in plain language.")
            appendLine("2. Identify which tracked record this event relates to (look at the records list below).")
            appendLine("3. Append ONE structured update block in this EXACT format:")
            appendLine()
            appendLine("[LIFEPILOT_ACTION]")
            appendLine("{")
            appendLine("  \"objectType\": \"<exact objectType from the record>\",")
            appendLine("  \"matchField\": \"<field used to identify the record, e.g. companyName, policyNumber, destination>\",")
            appendLine("  \"matchValue\": \"<value of that field>\",")
            appendLine("  \"summary\": \"<one-line human description of what changed>\",")
            appendLine("  \"fields\": [")
            appendLine("    {\"fieldId\": \"notes\", \"displayName\": \"Notes\", \"value\": \"<what happened, dated>\", \"mode\": \"append\"}")
            appendLine("  ]")
            appendLine("}")
            appendLine("[/LIFEPILOT_ACTION]")
            appendLine()
            appendLine("Life events by record type (these are examples, not exhaustive):")
            appendLine("  Job: interview scheduled/done, offer received, joined, resigned, got a promotion, salary changed")
            appendLine("       → use career_notes field (mode: append) + interview_status field (mode: set) where relevant")
            appendLine("  Passport: renewed, lost, applied for new, visa stamped")
            appendLine("  Insurance: renewed, claimed, premium changed, policy lapsed")
            appendLine("  Property: rented out, sold, refinanced, renovation started")
            appendLine("  Vehicle: serviced, MOT done, insured, sold, registration renewed")
            appendLine("  Travel: trip booked, visa approved, flight changed, trip completed")
            appendLine("  Health: doctor visit, diagnosis, medication changed, test result received")
            appendLine("  Education: enrolled, exam taken, result received, graduated")
            appendLine("  Investment: bought, sold, value changed, dividends received")
            appendLine("  Loan: EMI paid, pre-closed, interest rate changed")
            appendLine("  Tax: filed, refund received, notice received")
            appendLine("  Subscription: renewed, cancelled, plan upgraded")
            appendLine("  Bank Account: opened, closed, account details changed")
            appendLine("  Pension: contribution changed, retirement date updated")
            appendLine("  Utilities: provider changed, tariff updated, contract renewed")
            appendLine("  Will: updated, witnessed, executor changed")
            appendLine()
            appendLine("For Job records, prefer the career_notes field (mode: append) over the generic notes field.")
            appendLine("For all other records, use the notes field (mode: append) to log what happened.")
            appendLine("Also update specific structured fields where they exist (e.g. expiryDate, status).")
            appendLine()
            appendLine("If you need information to give better help, append ONE question block:")
            appendLine("[ASK]<the question to ask the user>[/ASK]")
            appendLine()
            appendLine("Rules:")
            appendLine("Only include [LIFEPILOT_ACTION] when a clear life event affecting a tracked record is detected.")
            appendLine("Only include [ASK] when more context would meaningfully improve the profile.")
            appendLine("Never include both [LIFEPILOT_ACTION] and [ASK] in the same response.")
            appendLine("Never make up data. Only use what the user tells you.")
            appendLine("If no matching record exists in the list, do NOT emit a LIFEPILOT_ACTION block.")
            appendLine()
            appendLine("USER LIFE DATA:")
            if (profile != null) {
                appendLine("Profile: ${profile.displayName}")
            }
            appendLine()
            val objectLabel = if (allObjects.size > 30) "Records (showing 30 of ${allObjects.size})" else "Records (${objects.size} total)"
            appendLine("$objectLabel:")
            objects.forEach { obj ->
                appendLine("  - [id=${obj.objectId}] ${obj.title} [type=${obj.objectType}, domain=${obj.domain}, status=${obj.status}]")
                val metadata = metadataByObject[obj.objectId] ?: emptyList()
                if (metadata.isNotEmpty()) {
                    metadata.take(8).forEach { entry ->
                        appendLine("    ${entry.fieldId}: ${entry.value}")
                    }
                }
            }
            if (objects.isEmpty()) {
                appendLine("  (No records yet. Encourage the user to add their first record.)")
            }
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

    fun clearMessages() {
        cachedSystemPrompt = null
        _state.update { it.copy(messages = emptyList(), pendingAction = null, pendingContextQuestion = null) }
    }
}
