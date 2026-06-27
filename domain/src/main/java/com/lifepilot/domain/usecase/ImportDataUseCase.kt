package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.MetadataFieldType
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProfileRepository
import org.json.JSONObject
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class ImportResult(
    val objectsImported: Int,
    val objectsSkipped: Int,
    val metadataEntriesImported: Int,
    val errors: List<String>,
)

class ImportDataUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val objectRepository: ObjectRepository,
    private val metadataRepository: MetadataRepository,
) {
    suspend operator fun invoke(jsonPayload: String, targetProfileId: String): Result<ImportResult> =
        runCatching {
            val root = JSONObject(jsonPayload)
            val version = root.optInt("version", 1)

            if (version > 1) error("Unsupported export version: $version")

            val profile = profileRepository.getProfileById(targetProfileId)
                ?: error("Target profile not found: $targetProfileId")

            val errors = mutableListOf<String>()
            var objectsImported = 0
            var objectsSkipped = 0
            var metadataEntriesImported = 0

            val objectsArray = root.optJSONArray("objects") ?: run {
                return@runCatching ImportResult(0, 0, 0, listOf("No objects array in payload"))
            }

            for (i in 0 until objectsArray.length()) {
                val obj = objectsArray.getJSONObject(i)
                runCatching {
                    val objectType = obj.getString("objectType")
                    val domain = obj.getString("domain")
                    val title = obj.getString("title")
                    val statusName = obj.optString("status", "ACTIVE")
                    val status = runCatching { ObjectStatus.valueOf(statusName) }.getOrDefault(ObjectStatus.ACTIVE)

                    val created = objectRepository.createObject(
                        profileId = profile.profileId,
                        objectType = objectType,
                        domain = domain,
                        title = title,
                        description = null,
                    )

                    if (status != ObjectStatus.ACTIVE) {
                        objectRepository.updateObjectStatus(created.objectId, status)
                    }

                    objectsImported++

                    val metadataObj = obj.optJSONObject("metadata")
                    if (metadataObj != null) {
                        val entries = metadataObj.keys().asSequence().mapNotNull { fieldId ->
                            val value = metadataObj.optString(fieldId).takeIf { it.isNotBlank() }
                                ?: return@mapNotNull null
                            MetadataEntry(
                                metadataId = UUID.randomUUID().toString(),
                                objectId = created.objectId,
                                fieldId = fieldId,
                                value = value,
                                fieldType = MetadataFieldType.TEXT,
                                source = MetadataSource.USER,
                                confidence = null,
                                version = 1,
                                updatedAt = Instant.now(),
                            )
                        }.toList()

                        if (entries.isNotEmpty()) {
                            metadataRepository.upsertMetadataBatch(entries)
                            metadataEntriesImported += entries.size
                        }
                    }
                }.onFailure { e ->
                    objectsSkipped++
                    errors.add("Object $i: ${e.message}")
                }
            }

            ImportResult(
                objectsImported = objectsImported,
                objectsSkipped = objectsSkipped,
                metadataEntriesImported = metadataEntriesImported,
                errors = errors,
            )
        }
}
