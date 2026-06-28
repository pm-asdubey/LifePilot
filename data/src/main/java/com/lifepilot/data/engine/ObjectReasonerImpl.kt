package com.lifepilot.data.engine

import com.lifepilot.domain.engine.ObjectReasoner
import com.lifepilot.domain.model.AiObjectContext
import com.lifepilot.domain.model.ObjectSnapshot
import com.lifepilot.domain.repository.DocumentRepository
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.TaskRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObjectReasonerImpl @Inject constructor(
    private val objectRepository: ObjectRepository,
    private val metadataRepository: MetadataRepository,
    private val taskRepository: TaskRepository,
    private val documentRepository: DocumentRepository,
) : ObjectReasoner {

    override suspend fun buildSnapshot(profileId: String, objectId: String): ObjectSnapshot? {
        val obj = runCatching { objectRepository.getObjectById(objectId) }.getOrNull() ?: return null
        val allMetadata = runCatching { metadataRepository.getMetadataByObject(objectId) }.getOrElse { emptyList() }

        val aiContextEntry = allMetadata.firstOrNull { it.fieldId == "ai_context" }
        val aiContext = aiContextEntry?.let { runCatching { parseAiContext(it.value) }.getOrNull() }
        val regularMetadata = allMetadata.filter { it.fieldId != "ai_context" }

        val pendingTaskCount = runCatching {
            taskRepository.observeTasksByObject(objectId)
                .catch { }
                .firstOrNull()
                ?.size ?: 0
        }.getOrElse { 0 }

        val documentCount = runCatching {
            documentRepository.getDocumentsByObject(objectId).size
        }.getOrElse { 0 }

        return ObjectSnapshot(
            objectId = obj.objectId,
            title = obj.title,
            objectType = obj.objectType,
            domain = obj.domain,
            status = obj.status,
            metadata = regularMetadata,
            aiContext = aiContext,
            pendingTaskCount = pendingTaskCount,
            documentCount = documentCount,
        )
    }

    override fun parseAiContext(json: String): AiObjectContext? {
        return try {
            val obj = JSONObject(json)
            val factsArray = obj.optJSONArray("importantFacts") ?: JSONArray()
            val suggestionsArray = obj.optJSONArray("suggestions") ?: JSONArray()
            AiObjectContext(
                summary = obj.optString("summary", ""),
                importantFacts = buildList { for (i in 0 until factsArray.length()) add(factsArray.getString(i)) },
                currentSituation = obj.optString("currentSituation", ""),
                suggestions = buildList { for (i in 0 until suggestionsArray.length()) add(suggestionsArray.getString(i)) },
                lastUpdated = runCatching { Instant.parse(obj.optString("lastUpdated")) }.getOrElse { Instant.now() },
                confidence = obj.optDouble("confidence", 0.8).toFloat(),
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse AiObjectContext")
            null
        }
    }

    override fun serializeAiContext(context: AiObjectContext): String {
        return JSONObject().apply {
            put("summary", context.summary)
            put("importantFacts", JSONArray(context.importantFacts))
            put("currentSituation", context.currentSituation)
            put("suggestions", JSONArray(context.suggestions))
            put("lastUpdated", context.lastUpdated.toString())
            put("confidence", context.confidence.toDouble())
        }.toString()
    }
}
