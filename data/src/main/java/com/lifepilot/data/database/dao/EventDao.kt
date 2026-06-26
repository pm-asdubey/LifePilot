package com.lifepilot.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifepilot.data.database.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE object_id = :objectId ORDER BY timestamp DESC")
    fun observeEventsByObject(objectId: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE object_id = :objectId ORDER BY timestamp DESC")
    suspend fun getEventsByObject(objectId: String): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)
}
