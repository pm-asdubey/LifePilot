package com.lifepilot.data.repository

import com.lifepilot.data.database.dao.ProjectDao
import com.lifepilot.data.database.entity.GoalEntity
import com.lifepilot.data.database.entity.ProjectEntity
import com.lifepilot.data.mapper.toDomain
import com.lifepilot.domain.model.Goal
import com.lifepilot.domain.model.GoalStatus
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.Project
import com.lifepilot.domain.model.ProjectStatus
import com.lifepilot.domain.model.Task
import com.lifepilot.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
) : ProjectRepository {

    override suspend fun createProject(
        profileId: String,
        title: String,
        description: String?,
        domain: String?,
        emoji: String,
        targetDate: LocalDate?,
        isAiProposed: Boolean,
    ): Project {
        val now = Instant.now().toEpochMilli()
        val entity = ProjectEntity(
            projectId = UUID.randomUUID().toString(),
            profileId = profileId,
            title = title,
            description = description,
            domain = domain,
            status = ProjectStatus.ACTIVE.name,
            emoji = emoji,
            targetDate = targetDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            isAiProposed = isAiProposed,
            createdAt = now,
            updatedAt = now,
        )
        projectDao.insertProject(entity)
        return entity.toDomainProject()
    }

    override suspend fun getProject(projectId: String): Project? =
        projectDao.getProjectById(projectId)?.toDomainProject()

    override suspend fun updateProject(project: Project) {
        projectDao.updateProject(project.toEntity())
    }

    override suspend fun deleteProject(projectId: String) {
        projectDao.deleteProject(projectId)
    }

    override fun observeProjects(profileId: String): Flow<List<Project>> =
        projectDao.observeProjects(profileId).map { entities ->
            entities.map { it.toDomainProject() }
        }

    override fun getProjectsForDomain(profileId: String, domain: String): Flow<List<Project>> =
        projectDao.getProjectsForDomain(profileId, domain).map { entities ->
            entities.map { it.toDomainProject() }
        }

    override suspend fun linkObject(projectId: String, objectId: String) {
        projectDao.linkObject(projectId, objectId)
    }

    override suspend fun linkTask(projectId: String, taskId: String) {
        projectDao.linkTask(projectId, taskId)
    }

    override suspend fun linkGoal(projectId: String, goalId: String) {
        projectDao.linkGoal(projectId, goalId)
    }

    override suspend fun getActiveProjects(profileId: String): List<Project> =
        projectDao.getActiveProjects(profileId).map { it.toDomainProject() }

    override fun observeObjectsForProject(projectId: String): Flow<List<LifeObject>> =
        projectDao.observeObjectsForProject(projectId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeTasksForProject(projectId: String): Flow<List<Task>> =
        projectDao.observeTasksForProject(projectId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeGoalsForProject(projectId: String): Flow<List<Goal>> =
        projectDao.observeGoalsForProject(projectId).map { entities ->
            entities.map { it.goalToDomain() }
        }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private fun ProjectEntity.toDomainProject(): Project = Project(
        projectId = projectId,
        profileId = profileId,
        title = title,
        description = description,
        domain = domain,
        status = runCatching { ProjectStatus.valueOf(status) }.getOrElse { ProjectStatus.ACTIVE },
        emoji = emoji,
        targetDate = targetDate?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        },
        isAiProposed = isAiProposed,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

    private fun Project.toEntity(): ProjectEntity = ProjectEntity(
        projectId = projectId,
        profileId = profileId,
        title = title,
        description = description,
        domain = domain,
        status = status.name,
        emoji = emoji,
        targetDate = targetDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        isAiProposed = isAiProposed,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
    )

    private fun GoalEntity.goalToDomain(): Goal = Goal(
        goalId = goalId,
        profileId = profileId,
        title = title,
        description = description,
        deadline = deadline?.let {
            LocalDate.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
        },
        status = runCatching { GoalStatus.valueOf(status) }.getOrElse { GoalStatus.ACTIVE },
        progress = progress,
        objectId = objectId,
        notes = notes,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )
}
