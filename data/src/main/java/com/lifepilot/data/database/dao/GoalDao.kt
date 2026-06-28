package com.lifepilot.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifepilot.data.database.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals WHERE profile_id = :profileId ORDER BY created_at DESC")
    fun observeGoalsByProfile(profileId: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE profile_id = :profileId AND status = 'ACTIVE' ORDER BY deadline ASC, created_at DESC")
    fun observeActiveGoals(profileId: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE goal_id = :goalId")
    suspend fun getGoalById(goalId: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE goal_id = :goalId")
    suspend fun deleteGoal(goalId: String)

    @Query("UPDATE goals SET progress = :progress, updated_at = :updatedAt WHERE goal_id = :goalId")
    suspend fun updateGoalProgress(goalId: String, progress: Int, updatedAt: Long)

    @Query("UPDATE goals SET status = :status, updated_at = :updatedAt WHERE goal_id = :goalId")
    suspend fun updateGoalStatus(goalId: String, status: String, updatedAt: Long)

    @Query("""
        SELECT * FROM goals
        WHERE profile_id = :profileId AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')
        ORDER BY created_at DESC
        LIMIT 20
    """)
    suspend fun searchGoals(profileId: String, query: String): List<GoalEntity>
}
