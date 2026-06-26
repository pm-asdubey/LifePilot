package com.lifepilot.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifepilot.data.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("""
        SELECT * FROM tasks
        WHERE profile_id = :profileId
        ORDER BY due_date ASC, priority DESC
    """)
    fun observeTasksByProfile(profileId: String): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE object_id = :objectId
        ORDER BY due_date ASC, priority DESC
    """)
    fun observeTasksByObject(objectId: String): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE profile_id = :profileId AND status IN ('PENDING', 'IN_PROGRESS')
        ORDER BY due_date ASC, priority DESC
    """)
    fun observePendingTasks(profileId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE task_id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, completed_at = :completedAt WHERE task_id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: String, completedAt: Long?)

    @Query("DELETE FROM tasks WHERE task_id = :taskId")
    suspend fun deleteTask(taskId: String)
}
