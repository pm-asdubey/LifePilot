package com.lifepilot.domain.repository

import com.lifepilot.domain.model.Goal
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.Project
import com.lifepilot.domain.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ProjectRepository {

    suspend fun createProject(
        profileId: String,
        title: String,
        description: String?,
        domain: String?,
        emoji: String = "🎯",
        targetDate: LocalDate? = null,
        isAiProposed: Boolean = false,
    ): Project

    suspend fun getProject(projectId: String): Project?

    suspend fun updateProject(project: Project)

    suspend fun deleteProject(projectId: String)

    fun observeProjects(profileId: String): Flow<List<Project>>

    fun getProjectsForDomain(profileId: String, domain: String): Flow<List<Project>>

    suspend fun linkObject(projectId: String, objectId: String)

    suspend fun linkTask(projectId: String, taskId: String)

    suspend fun linkGoal(projectId: String, goalId: String)

    suspend fun getActiveProjects(profileId: String): List<Project>

    fun observeObjectsForProject(projectId: String): Flow<List<LifeObject>>

    fun observeTasksForProject(projectId: String): Flow<List<Task>>

    fun observeGoalsForProject(projectId: String): Flow<List<Goal>>
}
