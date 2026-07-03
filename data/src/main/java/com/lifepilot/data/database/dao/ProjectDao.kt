package com.lifepilot.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifepilot.data.database.entity.GoalEntity
import com.lifepilot.data.database.entity.ObjectEntity
import com.lifepilot.data.database.entity.ProjectEntity
import com.lifepilot.data.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE project_id = :projectId")
    suspend fun getProjectById(projectId: String): ProjectEntity?

    @Query("DELETE FROM projects WHERE project_id = :projectId")
    suspend fun deleteProject(projectId: String)

    @Query("SELECT * FROM projects WHERE profile_id = :profileId ORDER BY created_at DESC")
    fun observeProjects(profileId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE profile_id = :profileId ORDER BY created_at DESC")
    fun observeProjectsByProfile(profileId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE profile_id = :profileId AND status = 'ACTIVE' ORDER BY created_at DESC")
    fun observeActiveProjectsByProfile(profileId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE profile_id = :profileId AND status = 'ACTIVE' ORDER BY updated_at DESC")
    suspend fun getActiveProjects(profileId: String): List<ProjectEntity>

    @Query("""
        SELECT * FROM projects
        WHERE profile_id = :profileId AND domain = :domain
        ORDER BY created_at DESC
    """)
    fun getProjectsForDomain(profileId: String, domain: String): Flow<List<ProjectEntity>>

    @Query("UPDATE objects SET project_id = :projectId WHERE object_id = :objectId")
    suspend fun linkObject(projectId: String, objectId: String)

    @Query("UPDATE tasks SET project_id = :projectId WHERE task_id = :taskId")
    suspend fun linkTask(projectId: String, taskId: String)

    @Query("UPDATE goals SET project_id = :projectId WHERE goal_id = :goalId")
    suspend fun linkGoal(projectId: String, goalId: String)

    @Query("SELECT * FROM objects WHERE project_id = :projectId ORDER BY updated_at DESC")
    fun observeObjectsForProject(projectId: String): Flow<List<ObjectEntity>>

    @Query("SELECT * FROM tasks WHERE project_id = :projectId ORDER BY due_date ASC")
    fun observeTasksForProject(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM goals WHERE project_id = :projectId ORDER BY created_at DESC")
    fun observeGoalsForProject(projectId: String): Flow<List<GoalEntity>>
}
