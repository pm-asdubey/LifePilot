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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(metadata: MetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadataBatch(metadata: List<MetadataEntity>)

    @Query("DELETE FROM metadata WHERE metadata_id = :metadataId")
    suspend fun deleteMetadata(metadataId: String)

    @Query("DELETE FROM metadata WHERE object_id = :objectId")
    suspend fun deleteAllMetadataForObject(objectId: String)
}
