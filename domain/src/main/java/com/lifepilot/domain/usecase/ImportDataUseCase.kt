package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.MetadataFieldType
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProfileRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(jsonPayload: String, targetProfileId: String): Result<ImportResult> =
        runCatching {
            val root = json.parseToJsonElement(jsonPayload).jsonObject
            val version = root["version"]?.jsonPrimitive?.int ?: 1

            if (version > 1) error("Unsupported export version: $version")

            val profile = profileRepository.getProfileById(targetProfileId)
                ?: error("Target profile not found: $targetProfileId")

            val errors = mutableListOf<String>()
            var objectsImported = 0
            var objectsSkipped = 0
            var metadataEntriesImported = 0

            val objectsArray = root["objects"]?.jsonArray ?: run {
                return@runCatching ImportResult(0, 0, 0, listOf("No objects array in payload"))
            }

            objectsArray.forEachIndexed { i, element ->
                runCatching {
                    val obj = element.jsonObject
                    val objectType = obj["objectType"]!!.jsonPrimitive.content
                    val domain = obj["domain"]!!.jsonPrimitive.content
                    val title = obj["title"]!!.jsonPrimitive.content
                    val statusName = obj["status"]?.jsonPrimitive?.content ?: "ACTIVE"
                    val status = runCatching { ObjectStatus.valueOf(statusName) }
                        .getOrDefault(ObjectStatus.ACTIVE)

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

                    val metadataObj = obj["metadata"]?.jsonObject
                    if (metadataObj != null) {
                        val entries = metadataObj.entries.mapNotNull { (fieldId, valueElement) ->
                            val value = runCatching { valueElement.jsonPrimitive.content }
                                .getOrNull()?.takeIf { it.isNotBlank() }
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
                        }

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
