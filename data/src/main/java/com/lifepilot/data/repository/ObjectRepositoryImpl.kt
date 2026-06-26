package com.lifepilot.data.repository

import com.lifepilot.data.database.dao.MetadataDao
import com.lifepilot.data.database.dao.ObjectDao
import com.lifepilot.data.mapper.toDomain
import com.lifepilot.data.mapper.toEntity
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.repository.ObjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class ObjectRepositoryImpl @Inject constructor(
    private val objectDao: ObjectDao,
    private val metadataDao: MetadataDao,
) : ObjectRepository {

    override fun observeObjectsByProfile(profileId: String): Flow<List<LifeObject>> =
        objectDao.observeObjectsByProfile(profileId).map { entities ->
            entities.map { entity ->
                val metadata = metadataDao.getMetadataByObject(entity.objectId)
                entity.toDomain(metadata.map { it.toDomain() })
            }
        }

    override fun observeObjectsByDomain(profileId: String, domain: String): Flow<List<LifeObject>> =
        objectDao.observeObjectsByDomain(profileId, domain).map { entities ->
            entities.map { entity ->
                val metadata = metadataDao.getMetadataByObject(entity.objectId)
                entity.toDomain(metadata.map { it.toDomain() })
            }
        }

    override fun observeObjectById(objectId: String): Flow<LifeObject?> =
        objectDao.observeObjectById(objectId).map { entity ->
            entity?.let {
                val metadata = metadataDao.getMetadataByObject(objectId)
                it.toDomain(metadata.map { m -> m.toDomain() })
            }
        }

    override suspend fun getObjectById(objectId: String): LifeObject? {
        val entity = objectDao.getObjectById(objectId) ?: return null
        val metadata = metadataDao.getMetadataByObject(objectId)
        return entity.toDomain(metadata.map { it.toDomain() })
    }

    override suspend fun createObject(
        profileId: String,
        objectType: String,
        domain: String,
        title: String,
        description: String?,
    ): LifeObject {
        val now = Instant.now()
        val obj = LifeObject(
            objectId = UUID.randomUUID().toString(),
            profileId = profileId,
            objectType = objectType,
            domain = domain,
            title = title,
            description = description,
            status = ObjectStatus.ACTIVE,
            metadata = emptyList(),
            archived = false,
            deleted = false,
            createdAt = now,
            updatedAt = now,
        )
        objectDao.insertObject(obj.toEntity())
        return obj
    }

    override suspend fun updateObject(lifeObject: LifeObject): LifeObject {
        val updated = lifeObject.copy(updatedAt = Instant.now())
        objectDao.updateObject(updated.toEntity())
        return updated
    }

    override suspend fun updateObjectStatus(objectId: String, status: ObjectStatus) {
        objectDao.updateStatus(objectId, status.name, Instant.now().toEpochMilli())
    }

    override suspend fun archiveObject(objectId: String) {
        objectDao.archiveObject(objectId, Instant.now().toEpochMilli())
    }

    override suspend fun deleteObject(objectId: String) {
        objectDao.softDeleteObject(objectId, Instant.now().toEpochMilli())
    }

    override fun observeObjectCountByDomain(profileId: String): Flow<Map<String, Int>> =
        objectDao.observeDomainCounts(profileId).map { counts ->
            counts.associate { it.domain to it.count }
        }
}
