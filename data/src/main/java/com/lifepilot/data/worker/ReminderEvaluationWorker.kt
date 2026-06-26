package com.lifepilot.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifepilot.data.engine.RuleEngineImpl
import com.lifepilot.data.notification.NotificationHelper
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.model.ReminderStatus
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ReminderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit

@HiltWorker
class ReminderEvaluationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val objectRepository: ObjectRepository,
    private val reminderRepository: ReminderRepository,
    private val preferenceManager: PreferenceManager,
    private val ruleEngine: RuleEngineImpl,
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

    companion object {
        const val WORK_NAME = "reminder_evaluation"
    }
}
