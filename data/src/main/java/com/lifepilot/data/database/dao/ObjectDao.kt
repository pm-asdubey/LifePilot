package com.lifepilot.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifepilot.data.database.entity.ObjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjectDao {
    @Query("""
        SELECT * FROM objects
        WHERE profile_id = :profileId AND deleted = 0
        ORDER BY updated_at DESC
    """)
    fun observeObjectsByProfile(profileId: String): Flow<List<ObjectEntity>>

    @Query("""
        SELECT * FROM objects
        WHERE profile_id = :profileId AND domain = :domain AND deleted = 0
        ORDER BY updated_at DESC
    """)
    fun observeObjectsByDomain(profileId: String, domain: String): Flow<List<ObjectEntity>>

    @Query("SELECT * FROM objects WHERE object_id = :objectId AND deleted = 0")
    fun observeObjectById(objectId: String): Flow<ObjectEntity?>

    @Query("SELECT * FROM objects WHERE object_id = :objectId AND deleted = 0")
    suspend fun getObjectById(objectId: String): ObjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObject(obj: ObjectEntity)

    @Update
    suspend fun updateObject(obj: ObjectEntity)

    @Query("UPDATE objects SET status = :status, updated_at = :updatedAt WHERE object_id = :objectId")
    suspend fun updateStatus(objectId: String, status: String, updatedAt: Long)

    @Query("UPDATE objects SET archived = 1, updated_at = :updatedAt WHERE object_id = :objectId")
    suspend fun archiveObject(objectId: String, updatedAt: Long)

    @Query("UPDATE objects SET deleted = 1, updated_at = :updatedAt WHERE object_id = :objectId")
    suspend fun softDeleteObject(objectId: String, updatedAt: Long)

    @Query("""
        SELECT domain, COUNT(*) as count
        FROM objects
        WHERE profile_id = :profileId AND deleted = 0 AND archived = 0
        GROUP BY domain
    """)
    fun observeDomainCounts(profileId: String): Flow<List<DomainCount>>

    @Query("""
        SELECT * FROM objects
        WHERE profile_id = :profileId AND deleted = 0
        AND (LOWER(title) LIKE '%' || LOWER(:query) || '%'
          OR LOWER(description) LIKE '%' || LOWER(:query) || '%'
          OR LOWER(object_type) LIKE '%' || LOWER(:query) || '%')
        ORDER BY updated_at DESC
        LIMIT 50
    """)
    suspend fun searchObjects(profileId: String, query: String): List<ObjectEntity>
}

data class DomainCount(
    val domain: String,
    val count: Int,
)
