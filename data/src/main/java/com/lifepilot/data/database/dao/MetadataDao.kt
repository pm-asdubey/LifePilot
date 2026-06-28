package com.lifepilot.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifepilot.data.database.entity.MetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetadataDao {
    @Query("SELECT * FROM metadata WHERE object_id = :objectId ORDER BY field_id ASC")
    fun observeMetadataByObject(objectId: String): Flow<List<MetadataEntity>>

    @Query("SELECT * FROM metadata WHERE object_id = :objectId ORDER BY field_id ASC")
    suspend fun getMetadataByObject(objectId: String): List<MetadataEntity>

    @Query("SELECT * FROM metadata WHERE object_id = :objectId AND field_id = :fieldId")
    suspend fun getMetadataByField(objectId: String, fieldId: String): MetadataEntity?

    @Query("SELECT * FROM metadata WHERE metadata_id = :metadataId")
    suspend fun getMetadataById(metadataId: String): MetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(metadata: MetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadataBatch(metadata: List<MetadataEntity>)

    @Query("DELETE FROM metadata WHERE metadata_id = :metadataId")
    suspend fun deleteMetadata(metadataId: String)

    @Query("DELETE FROM metadata WHERE object_id = :objectId")
    suspend fun deleteAllMetadataForObject(objectId: String)

    @Query("SELECT * FROM metadata WHERE object_id IN (:objectIds) ORDER BY object_id ASC, field_id ASC")
    suspend fun getMetadataForObjects(objectIds: List<String>): List<MetadataEntity>

    @Query("""
        SELECT m.* FROM metadata m
        INNER JOIN objects o ON m.object_id = o.object_id
        WHERE o.profile_id = :profileId
          AND m.value LIKE '%' || :query || '%'
        ORDER BY m.object_id ASC
    """)
    suspend fun searchMetadataValues(profileId: String, query: String): List<MetadataEntity>
}
