package com.lifepilot.domain.usecase

import com.lifepilot.domain.repository.DocumentRepository
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.repository.ReminderRepository
import com.lifepilot.domain.repository.TaskRepository
import com.lifepilot.domain.repository.TimelineRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

data class ExportBundle(
    val exportedAt: Long,
    val version: Int = 1,
    val profileCount: Int,
    val objectCount: Int,
    val documentCount: Int,
    val taskCount: Int,
    val reminderCount: Int,
    val timelineCount: Int,
    val jsonPayload: String,
)

class ExportDataUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val objectRepository: ObjectRepository,
    private val documentRepository: DocumentRepository,
    private val metadataRepository: MetadataRepository,
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val timelineRepository: TimelineRepository,
) {
    suspend operator fun invoke(profileId: String): Result<ExportBundle> = runCatching {
        val profile = profileRepository.getProfileById(profileId)
            ?: error("Profile not found: $profileId")
        val objects = objectRepository.observeObjectsByProfile(profileId).firstOrNull() ?: emptyList()
        val tasks = taskRepository.observeTasksByProfile(profileId).firstOrNull() ?: emptyList()
        val timeline = timelineRepository.observeTimeline(profileId).firstOrNull() ?: emptyList()

        val allMetadata = objects.flatMap { obj ->
            metadataRepository.getMetadataByObject(obj.objectId)
        }

        val allDocuments = objects.flatMap { obj ->
            documentRepository.observeDocumentsByObject(obj.objectId).firstOrNull() ?: emptyList()
        }

        val allReminders = objects.flatMap { obj ->
            reminderRepository.getRemindersForObject(obj.objectId)
        }

        val metadataByObject = allMetadata.groupBy { it.objectId }

        val payload = buildString {
            appendLine("{")
            appendLine("  \"version\": 1,")
            appendLine("  \"exportedAt\": ${System.currentTimeMillis()},")
            appendLine("  \"profile\": {")
            appendLine("    \"profileId\": \"${profile.profileId}\",")
            appendLine("    \"displayName\": \"${profile.displayName.sanitize()}\",")
            appendLine("    \"isPrimary\": ${profile.isPrimary}")
            appendLine("  },")
            appendLine("  \"objects\": [")
            objects.forEachIndexed { objIndex, obj ->
                appendLine("    {")
                appendLine("      \"objectId\": \"${obj.objectId}\",")
                appendLine("      \"objectType\": \"${obj.objectType}\",")
                appendLine("      \"domain\": \"${obj.domain}\",")
                appendLine("      \"title\": \"${obj.title.sanitize()}\",")
                appendLine("      \"status\": \"${obj.status.name}\",")
                appendLine("      \"createdAt\": \"${obj.createdAt}\",")
                appendLine("      \"updatedAt\": \"${obj.updatedAt}\",")
                appendLine("      \"metadata\": {")
                val metas = metadataByObject[obj.objectId] ?: emptyList()
                metas.forEachIndexed { metaIndex, meta ->
                    val comma = if (metaIndex < metas.lastIndex) "," else ""
                    appendLine("        \"${meta.fieldId}\": \"${meta.value.sanitize()}\"$comma")
                }
                val objComma = if (objIndex < objects.lastIndex) "," else ""
                appendLine("      }")
                appendLine("    }$objComma")
            }
            appendLine("  ],")
            appendLine("  \"taskCount\": ${tasks.size},")
            appendLine("  \"reminderCount\": ${allReminders.size},")
            appendLine("  \"documentCount\": ${allDocuments.size},")
            appendLine("  \"timelineCount\": ${timeline.size}")
            append("}")
        }

        ExportBundle(
            exportedAt = System.currentTimeMillis(),
            version = 1,
            profileCount = 1,
            objectCount = objects.size,
            documentCount = allDocuments.size,
            taskCount = tasks.size,
            reminderCount = allReminders.size,
            timelineCount = timeline.size,
            jsonPayload = payload,
        )
    }

    private fun String.sanitize(): String = replace("\"", "\\\"").replace("\n", "\\n")
}
