package com.lifepilot.domain.repository

import com.lifepilot.domain.model.Reminder
import com.lifepilot.domain.model.ReminderStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ReminderRepository {
    fun observeRemindersByObject(objectId: String): Flow<List<Reminder>>
    fun observeUpcomingReminders(before: Instant): Flow<List<Reminder>>
    suspend fun createReminder(reminder: Reminder): Reminder
    suspend fun updateReminderStatus(reminderId: String, status: ReminderStatus)
    suspend fun deleteReminder(reminderId: String)
    suspend fun getRemindersForObject(objectId: String): List<Reminder>
}
