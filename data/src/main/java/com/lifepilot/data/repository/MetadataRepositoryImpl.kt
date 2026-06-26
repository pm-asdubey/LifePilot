package com.lifepilot.data.repository

import com.lifepilot.data.database.dao.MetadataDao
import com.lifepilot.data.mapper.toDomain
import com.lifepilot.data.mapper.toEntity
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.MetadataFieldType
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.repository.MetadataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class MetadataRepositoryImpl @Inject constructor(
    private val metadataDao: MetadataDao,
) : MetadataRepository {

    override fun observeMetadataByObject(objectId: String): Flow<List<MetadataEntry>> =
        metadataDao.observeMetadataByObject(objectId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getMetadataByObject(objectId: String): List<MetadataEntry> =
        metadataDao.getMetadataByObject(objectId).map { it.toDomain() }

    override suspend fun getMetadataByField(objectId: String, fieldId: String): MetadataEntry? =
        metadataDao.getMetadataByField(objectId, fieldId)?.toDomain()

    override suspend fun upsertMetadata(
        objectId: String,
        fieldId: String,
        value: String,
        source: MetadataSource,
        confidence: Float?,
    ): MetadataEntry {
        val existing = metadataDao.getMetadataByField(objectId, fieldId)
        val entry = MetadataEntry(
            metadataId = existing?.metadataId ?: UUID.randomUUID().toString(),
            objectId = objectId,
            fieldId = fieldId,
            fieldType = MetadataFieldType.TEXT,
            value = value,
            version = (existing?.version ?: 0) + 1,
            confidence = confidence,
            source = source,
            updatedAt = Instant.now(),
        )
        metadataDao.upsertMetadata(entry.toEntity())
        return entry
    }

    override suspend fun upsertMetadataBatch(entries: List<MetadataEntry>): List<MetadataEntry> {
        metadataDao.upsertMetadataBatch(entries.map { it.toEntity() })
        return entries
    }

    override suspend fun deleteMetadata(metadataId: String) {
        metadataDao.deleteMetadata(metadataId)
    }
}
