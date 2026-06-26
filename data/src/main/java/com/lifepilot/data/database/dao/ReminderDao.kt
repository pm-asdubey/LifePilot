package com.lifepilot.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifepilot.data.database.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE object_id = :objectId ORDER BY trigger_date ASC")
    fun observeRemindersByObject(objectId: String): Flow<List<ReminderEntity>>

    @Query("""
        SELECT * FROM reminders
        WHERE trigger_date <= :beforeMs AND status = 'SCHEDULED'
        ORDER BY trigger_date ASC
    """)
    fun observeUpcomingReminders(beforeMs: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE object_id = :objectId ORDER BY trigger_date ASC")
    suspend fun getRemindersForObject(objectId: String): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET status = :status WHERE reminder_id = :reminderId")
    suspend fun updateStatus(reminderId: String, status: String)

    @Query("DELETE FROM reminders WHERE reminder_id = :reminderId")
    suspend fun deleteReminder(reminderId: String)
}
