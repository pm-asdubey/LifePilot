package com.lifepilot.data.repository

import com.lifepilot.data.database.dao.GoalDao
import com.lifepilot.data.database.entity.GoalEntity
import com.lifepilot.domain.model.Goal
import com.lifepilot.domain.model.GoalStatus
import com.lifepilot.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
) : GoalRepository {

    override fun observeGoalsByProfile(profileId: String): Flow<List<Goal>> =
        goalDao.observeGoalsByProfile(profileId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeActiveGoals(profileId: String): Flow<List<Goal>> =
        goalDao.observeActiveGoals(profileId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getGoalById(goalId: String): Goal? =
        goalDao.getGoalById(goalId)?.toDomain()

    override suspend fun upsertGoal(goal: Goal) {
        goalDao.upsertGoal(goal.toEntity())
    }

    override suspend fun deleteGoal(goalId: String) {
        goalDao.deleteGoal(goalId)
    }

    override suspend fun updateProgress(goalId: String, progress: Int) {
        goalDao.updateGoalProgress(goalId, progress, Instant.now().toEpochMilli())
    }

    override suspend fun completeGoal(goalId: String) {
        goalDao.updateGoalStatus(goalId, GoalStatus.COMPLETED.name, Instant.now().toEpochMilli())
    }

    private fun GoalEntity.toDomain(): Goal = Goal(
        goalId = goalId,
        profileId = profileId,
        title = title,
        description = description,
        deadline = deadline?.let { LocalDate.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) },
        status = GoalStatus.valueOf(status),
        progress = progress,
        objectId = objectId,
        notes = notes,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

    private fun Goal.toEntity(): GoalEntity = GoalEntity(
        goalId = goalId,
        profileId = profileId,
        title = title,
        description = description,
        deadline = deadline?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        status = status.name,
        progress = progress,
        objectId = objectId,
        notes = notes,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
    )
}
