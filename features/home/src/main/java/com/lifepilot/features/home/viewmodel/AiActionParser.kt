package com.lifepilot.features.home.viewmodel

import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.model.ActionItem
import com.lifepilot.domain.model.ActionPlan
import com.lifepilot.domain.model.ActionPlanNormalizer
import com.lifepilot.domain.model.ActionPlanType
import com.lifepilot.domain.model.AiProposal
import com.lifepilot.domain.model.ClarifyingQuestion
import com.lifepilot.domain.model.FieldSensitivity
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.ProposedField
import com.lifepilot.domain.model.TaskPriority
import com.lifepilot.domain.model.UpdateMode
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standalone parser for AI action blocks extracted from [LIFEPILOT_ACTION]...[/LIFEPILOT_ACTION] tags.
 *
 * Extracted from HomeViewModel to make parsing logic independently testable.
 *
 * @param schemaEngine Used to look up field sensitivity definitions for METADATA_UPDATE proposals.
 *
 * Runtime context ([objectIndex], [objectMetadataIndex]) is passed per-call rather than injected
 * because it is populated fresh on every AI retrieval — it is NOT fixed state at construction time.
 */
@Singleton
class AiActionParser @Inject constructor(
    private val schemaEngine: SchemaEngine,
) {

    /**
     * Lightweight holder for a resolved object reference used internally by action-item parsing.
     * Exposed as `internal` so tests in the same module can assert on intermediate resolution.
     */
    internal data class ResolvedObject(
        val objectId: String,
        val title: String,
    )

    /**
     * Parse a JSON string extracted from a [LIFEPILOT_ACTION] block into an [AiProposal].
     *
     * Returns `null` on any parse failure (malformed JSON, missing required fields, etc.)
     * so that the caller can safely ignore unrecognised action blocks without crashing.
     *
     * @param json             Raw string content of the action block. Must be a JSON object.
     * @param objectIndex      objectId → (title, objectType) lookup built by RetrievalEngine.
     * @param objectMetadataIndex objectId → metadata entries, used for fine-grained matching.
     */
    fun parseAction(
        json: String,
        objectIndex: Map<String, Pair<String, String>> = emptyMap(),
        objectMetadataIndex: Map<String, List<MetadataEntry>> = emptyMap(),
    ): AiProposal? {
        return try {
            // Models sometimes prefix the JSON with the bare action type (e.g. "OBJECT_CREATION {…}")
            // or wrap it in ```json fences. Isolate the outermost {…} so parsing survives that — this
            // was the root cause of scanned documents failing to classify (JSONException on a bare
            // "OBJECT_CREATION" token).
            val start = json.indexOf('{')
            val end = json.lastIndexOf('}')
            val prefix = if (start > 0) json.substring(0, start) else ""
            val jsonText = if (start >= 0 && end > start) json.substring(start, end + 1) else json
            val obj = JSONObject(jsonText)
            // Prefer an explicit actionType; else recover the bare action-type token the model wrote
            // before the JSON object.
            val actionType = obj.optString("actionType", "").ifBlank {
                Regex("[A-Z_]{4,}").find(prefix)?.value ?: "METADATA_UPDATE"
            }
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
                        projectId = obj.optString("projectId", "").takeIf { it.isNotBlank() && it != "null" },
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
                    val resolved = resolveObjectForAction(obj, objectIndex, objectMetadataIndex) ?: return null
                    val newStatus = parseStatus(obj.optString("newStatus", "INACTIVE"))
                    AiProposal.StatusUpdate(
                        proposalId = UUID.randomUUID().toString(),
                        summary = summary.ifBlank { "Status updated" },
                        objectId = resolved.objectId,
                        objectTitle = resolved.title,
                        newStatus = newStatus,
                    )
                }
                "ACTION_PLAN" -> parseActionPlan(obj, summary, objectIndex, objectMetadataIndex)
                "PROJECT_CREATION" -> {
                    val linkedObjectIdsArray = obj.optJSONArray("linkedObjectIds")
                    val linkedObjectIds = buildList {
                        if (linkedObjectIdsArray != null) {
                            for (i in 0 until linkedObjectIdsArray.length()) {
                                val id = linkedObjectIdsArray.optString(i, "")
                                if (id.isNotBlank()) add(id)
                            }
                        }
                    }
                    AiProposal.ProjectCreation(
                        proposalId = UUID.randomUUID().toString(),
                        summary = summary,
                        title = obj.optString("title", "New project"),
                        description = obj.optString("description", "").takeIf { it.isNotBlank() && it != "null" },
                        domain = obj.optString("domain", "").takeIf { it.isNotBlank() && it != "null" },
                        emoji = obj.optString("emoji", "").takeIf { it.isNotBlank() && it != "null" },
                        targetDate = obj.optString("targetDate", "").takeIf { it.isNotBlank() && it != "null" }
                            ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                        linkedObjectIds = linkedObjectIds,
                    )
                }
                else -> parseMetadataUpdate(obj, summary, objectIndex, objectMetadataIndex)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse LIFEPILOT_ACTION block")
            null
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun parseActionPlan(
        obj: JSONObject,
        summary: String,
        objectIndex: Map<String, Pair<String, String>>,
        objectMetadataIndex: Map<String, List<MetadataEntry>>,
    ): AiProposal.ActionPlan? {
        return try {
            val planType = runCatching {
                ActionPlanType.valueOf(obj.optString("planType", "CUSTOM").uppercase())
            }.getOrDefault(ActionPlanType.CUSTOM)

            val itemsArray = obj.optJSONArray("items") ?: return null
            val items = buildList {
                for (i in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(i)
                    parseActionItem(itemObj, objectIndex, objectMetadataIndex)?.let { add(it) }
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

            val hasMore = obj.optBoolean("has_more", false)
            val continuationContext = obj.optString("continuation_context").takeIf { it.isNotBlank() }

            val plan = ActionPlan(
                planId = UUID.randomUUID().toString(),
                type = planType,
                summary = summary.ifBlank { "Action plan" },
                items = items,
                clarifyingQuestions = questions,
                hasMore = hasMore,
                continuationContext = continuationContext,
            ).let { if (!hasMore) ActionPlanNormalizer.ensureProject(it) else it }

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

    private fun parseActionItem(
        obj: JSONObject,
        objectIndex: Map<String, Pair<String, String>>,
        objectMetadataIndex: Map<String, List<MetadataEntry>>,
    ): ActionItem? {
        return try {
            val type = obj.optString("type", "").uppercase()
            val itemId = obj.optString("itemId", UUID.randomUUID().toString())
            val itemSummary = obj.optString("summary", "")
            val dependsOn = obj.optJSONArray("dependsOn")?.let { arr ->
                buildList { for (i in 0 until arr.length()) add(arr.getString(i)) }
            } ?: emptyList()

            when (type) {
                "UPDATE_RECORD" -> {
                    val resolved = resolveObjectForAction(obj, objectIndex, objectMetadataIndex) ?: return null
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
                    val resolved = resolveObjectForAction(obj, objectIndex, objectMetadataIndex) ?: return null
                    ActionItem.UpdateStatus(
                        itemId = itemId,
                        summary = itemSummary,
                        objectId = resolved.objectId,
                        objectTitle = resolved.title,
                        newStatus = parseStatus(obj.optString("newStatus", "INACTIVE")),
                        dependsOn = dependsOn,
                    )
                }
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
                "CREATE_PROJECT" -> ActionItem.CreateProject(
                    itemId = itemId,
                    summary = itemSummary,
                    title = obj.optString("title", "New project"),
                    description = obj.optString("description", "").takeIf { it.isNotBlank() && it != "null" },
                    emoji = obj.optString("emoji", "🎯").takeIf { it.isNotBlank() && it != "null" } ?: "🎯",
                    domain = obj.optString("domain", "").takeIf { it.isNotBlank() && it != "null" },
                    targetDate = obj.optString("targetDate", "").takeIf { it.isNotBlank() && it != "null" }
                        ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
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

    /**
     * Maps free-form status strings returned by the AI to the canonical [ObjectStatus] enum.
     * Handles common synonyms so cancellations, endings, and closures are not silently
     * defaulted to INACTIVE (ISSUE-022).
     */
    internal fun parseStatus(statusString: String): ObjectStatus {
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

    internal fun resolveObjectForAction(
        obj: JSONObject,
        objectIndex: Map<String, Pair<String, String>>,
        objectMetadataIndex: Map<String, List<MetadataEntry>>,
    ): ResolvedObject? {
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
        return ResolvedObject(resolvedObjectId, resolvedTitle)
    }

    private fun parseMetadataUpdate(
        obj: JSONObject,
        summary: String,
        objectIndex: Map<String, Pair<String, String>>,
        objectMetadataIndex: Map<String, List<MetadataEntry>>,
    ): AiProposal.MetadataUpdate? {
        val resolved = resolveObjectForAction(obj, objectIndex, objectMetadataIndex) ?: return null
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
}
