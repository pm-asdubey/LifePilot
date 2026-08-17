package com.lifepilot.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifepilot.data.notification.NotificationHelper
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.engine.RuleEngine
import com.lifepilot.domain.model.ReminderStatus
import com.lifepilot.domain.model.TaskStatus
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ReminderRepository
import com.lifepilot.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@HiltWorker
class ReminderEvaluationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val objectRepository: ObjectRepository,
    private val reminderRepository: ReminderRepository,
    private val taskRepository: TaskRepository,
    private val preferenceManager: PreferenceManager,
    private val ruleEngine: RuleEngine,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val profileId = preferenceManager.getActiveProfileId()
                ?: return Result.success()

            val objects = objectRepository.observeObjectsByProfile(profileId)
                .firstOrNull()
                ?: return Result.success()

            for (obj in objects) {
                ruleEngine.evaluateRemindersForObject(obj.objectId)
            }

            fireTriggeredNotifications()
            fireTaskDueNotifications(profileId)

            Timber.d("Reminder evaluation complete for ${objects.size} objects")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Reminder evaluation failed")
            Result.retry()
        }
    }

    private suspend fun fireTriggeredNotifications() {
        val now = Instant.now()
        val upcoming = reminderRepository.observeUpcomingReminders(
            now.plus(1, ChronoUnit.DAYS)
        ).firstOrNull() ?: return

        for (reminder in upcoming) {
            if (reminder.status != ReminderStatus.SCHEDULED) continue
            if (reminder.triggerDate.isAfter(now)) continue

            notificationHelper.showReminderNotification(
                reminderId = reminder.reminderId,
                title = reminder.title,
                message = reminder.message ?: reminder.title,
                objectId = reminder.objectId,
            )
            reminderRepository.updateReminderStatus(reminder.reminderId, ReminderStatus.TRIGGERED)
        }
    }

    private suspend fun fireTaskDueNotifications(profileId: String) {
        val today = LocalDate.now()
        val tasks = taskRepository.observeTasksByProfile(profileId).firstOrNull() ?: return
        for (task in tasks) {
            if (task.status != TaskStatus.PENDING) continue
            val due = task.dueDate ?: continue
            if (!due.isEqual(today)) continue
            notificationHelper.showReminderNotification(
                reminderId = "task_due_${task.taskId}",
                title = task.title,
                message = "Due today",
                objectId = task.objectId,
                priority = com.lifepilot.domain.model.ReminderPriority.HIGH,
            )
        }
    }

    companion object {
        const val WORK_NAME = "reminder_evaluation"
    }
}
