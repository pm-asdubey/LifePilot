package com.lifepilot.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifepilot.data.database.entity.TimelineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineDao {
    @Query("""
        SELECT * FROM timeline
        WHERE profile_id = :profileId
        ORDER BY timestamp DESC
        LIMIT 100
    """)
    fun observeTimeline(profileId: String): Flow<List<TimelineEntity>>

    @Query("""
        SELECT * FROM timeline
        WHERE object_id = :objectId
        ORDER BY timestamp DESC
    """)
    fun observeTimelineByObject(objectId: String): Flow<List<TimelineEntity>>

    @Query("""
        SELECT * FROM timeline
        WHERE profile_id = :profileId AND domain = :domain
        ORDER BY timestamp DESC
        LIMIT 100
    """)
    fun observeTimelineByDomain(profileId: String, domain: String): Flow<List<TimelineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimelineEntry(entry: TimelineEntity)

    @Query("DELETE FROM timeline WHERE timeline_id = :timelineId")
    suspend fun deleteTimelineEntry(timelineId: String)
}
