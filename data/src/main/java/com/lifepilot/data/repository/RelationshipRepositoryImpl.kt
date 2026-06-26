package com.lifepilot.data.repository

import com.lifepilot.data.database.dao.RelationshipDao
import com.lifepilot.data.database.entity.RelationshipEntity
import com.lifepilot.domain.model.Relationship
import com.lifepilot.domain.model.RelationshipStatus
import com.lifepilot.domain.repository.RelationshipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelationshipRepositoryImpl @Inject constructor(
    private val relationshipDao: RelationshipDao,
) : RelationshipRepository {

    override fun observeRelationshipsByObject(objectId: String): Flow<List<Relationship>> =
        relationshipDao.observeRelationshipsByObject(objectId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getRelationshipsByObject(objectId: String): List<Relationship> =
        relationshipDao.getRelationshipsByObject(objectId).map { it.toDomain() }

    override suspend fun createRelationship(
        sourceObjectId: String,
        targetObjectId: String,
        relationshipType: String,
    ): Relationship {
        val entity = RelationshipEntity(
            relationshipId = UUID.randomUUID().toString(),
            sourceObjectId = sourceObjectId,
            targetObjectId = targetObjectId,
            relationshipType = relationshipType,
            status = RelationshipStatus.ACTIVE.name,
            createdAt = Instant.now().toEpochMilli(),
        )
        relationshipDao.insertRelationship(entity)
        return entity.toDomain()
    }

    override suspend fun deleteRelationship(relationshipId: String) {
        relationshipDao.deleteRelationship(relationshipId)
    }
}

private fun RelationshipEntity.toDomain() = Relationship(
    relationshipId = relationshipId,
    sourceObjectId = sourceObjectId,
    targetObjectId = targetObjectId,
    relationshipType = relationshipType,
    status = try { RelationshipStatus.valueOf(status) } catch (_: Exception) { RelationshipStatus.ACTIVE },
    createdAt = Instant.ofEpochMilli(createdAt),
)
