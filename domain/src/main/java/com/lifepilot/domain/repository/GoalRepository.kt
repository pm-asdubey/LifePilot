package com.lifepilot.domain.repository

import com.lifepilot.domain.model.Goal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun observeGoalsByProfile(profileId: String): Flow<List<Goal>>
    fun observeActiveGoals(profileId: String): Flow<List<Goal>>
    suspend fun getGoalById(goalId: String): Goal?
    suspend fun upsertGoal(goal: Goal)
    suspend fun deleteGoal(goalId: String)
    suspend fun updateProgress(goalId: String, progress: Int)
    suspend fun completeGoal(goalId: String)
}
