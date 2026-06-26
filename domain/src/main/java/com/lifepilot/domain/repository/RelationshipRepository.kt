package com.lifepilot.domain.repository

import com.lifepilot.domain.model.Relationship
import kotlinx.coroutines.flow.Flow

interface RelationshipRepository {
    fun observeRelationshipsByObject(objectId: String): Flow<List<Relationship>>
    suspend fun getRelationshipsByObject(objectId: String): List<Relationship>
    suspend fun createRelationship(
        sourceObjectId: String,
        targetObjectId: String,
        relationshipType: String,
    ): Relationship
    suspend fun deleteRelationship(relationshipId: String)
}
