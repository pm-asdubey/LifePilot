package com.lifepilot.data.repository

import com.lifepilot.data.database.dao.ReminderDao
import com.lifepilot.data.database.entity.ReminderEntity
import com.lifepilot.domain.model.Reminder
import com.lifepilot.domain.model.ReminderPriority
import com.lifepilot.domain.model.ReminderStatus
import com.lifepilot.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao,
) : ReminderRepository {

    override fun observeRemindersByObject(objectId: String): Flow<List<Reminder>> =
        reminderDao.observeRemindersByObject(objectId).map { it.map { e -> e.toDomain() } }

    override fun observeUpcomingReminders(before: Instant): Flow<List<Reminder>> =
        reminderDao.observeUpcomingReminders(before.toEpochMilli()).map { it.map { e -> e.toDomain() } }

    override suspend fun createReminder(reminder: Reminder): Reminder {
        reminderDao.insertReminder(reminder.toEntity())
        return reminder
    }

    override suspend fun updateReminderStatus(reminderId: String, status: ReminderStatus) {
        reminderDao.updateStatus(reminderId, status.name)
    }

    override suspend fun deleteReminder(reminderId: String) {
        reminderDao.deleteReminder(reminderId)
    }

    override suspend fun getRemindersForObject(objectId: String): List<Reminder> =
        reminderDao.getRemindersForObject(objectId).map { it.toDomain() }

    private fun ReminderEntity.toDomain(): Reminder = Reminder(
        reminderId = reminderId,
        objectId = objectId,
        reminderType = reminderType,
        triggerDate = Instant.ofEpochMilli(triggerDate),
        priority = ReminderPriority.valueOf(priority),
        status = ReminderStatus.valueOf(status),
        title = title,
        message = message,
    )

    private fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
        reminderId = reminderId,
        objectId = objectId,
        reminderType = reminderType,
        triggerDate = triggerDate.toEpochMilli(),
        priority = priority.name,
        status = status.name,
        title = title,
        message = message,
    )
}
