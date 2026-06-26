package com.lifepilot.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifepilot.data.database.entity.RelationshipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RelationshipDao {
    @Query("""
        SELECT * FROM relationships
        WHERE source_object_id = :objectId OR target_object_id = :objectId
        ORDER BY created_at DESC
    """)
    fun observeRelationshipsByObject(objectId: String): Flow<List<RelationshipEntity>>

    @Query("""
        SELECT * FROM relationships
        WHERE source_object_id = :objectId OR target_object_id = :objectId
    """)
    suspend fun getRelationshipsByObject(objectId: String): List<RelationshipEntity>

    @Query("""
        SELECT * FROM relationships
        WHERE (source_object_id = :sourceId AND target_object_id = :targetId)
           OR (source_object_id = :targetId AND target_object_id = :sourceId)
    """)
    suspend fun getRelationshipBetween(sourceId: String, targetId: String): RelationshipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationship(relationship: RelationshipEntity)

    @Query("DELETE FROM relationships WHERE relationship_id = :relationshipId")
    suspend fun deleteRelationship(relationshipId: String)
}
