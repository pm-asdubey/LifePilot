package com.lifepilot.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifepilot.data.notification.NotificationHelper
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.model.TaskStatus
import com.lifepilot.domain.repository.GoalRepository
import com.lifepilot.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Fires daily at approximately 9 AM to remind the user of active goals and pending tasks.
 * Scheduled at app startup; WorkManager keeps the period even after reboots.
 */
@HiltWorker
class MorningBriefWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val goalRepository: GoalRepository,
    private val taskRepository: TaskRepository,
    private val preferenceManager: PreferenceManager,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "morning_brief"

        fun schedule(context: Context) {
            val now = java.time.ZonedDateTime.now()
            val targetHour = 9
            val nextRun = if (now.hour < targetHour) {
                now.withHour(targetHour).withMinute(0).withSecond(0).withNano(0)
            } else {
                now.plusDays(1).withHour(targetHour).withMinute(0).withSecond(0).withNano(0)
            }
            val initialDelay = java.time.Duration.between(now, nextRun).toMinutes()
                .coerceAtLeast(0)

            val request = PeriodicWorkRequestBuilder<MorningBriefWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            )
                .setInitialDelay(initialDelay, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
            Timber.d("MorningBriefWorker scheduled, first run in $initialDelay min")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val profileId = preferenceManager.getActiveProfileId() ?: return Result.success()

            val pendingTasks = taskRepository.observePendingTasks(profileId)
                .catch { emit(emptyList()) }
                .firstOrNull()
                ?.filter { it.status == TaskStatus.PENDING }
                ?: emptyList()

            val activeGoals = goalRepository.observeActiveGoals(profileId)
                .catch { emit(emptyList()) }
                .firstOrNull()
                ?: emptyList()

            if (pendingTasks.isEmpty() && activeGoals.isEmpty()) {
                return Result.success()
            }

            val title = buildBriefTitle(activeGoals.size, pendingTasks.size)
            val body = buildBriefBody(
                activeGoals.take(2).map { it.title },
                pendingTasks.take(3).map { it.title },
            )

            notificationHelper.showReminderNotification(
                reminderId = "morning_brief_${System.currentTimeMillis()}",
                title = title,
                message = body,
                objectId = null,
            )

            Timber.d("MorningBriefWorker: fired — ${activeGoals.size} goals, ${pendingTasks.size} tasks")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "MorningBriefWorker failed")
            Result.retry()
        }
    }

    private fun buildBriefTitle(goalCount: Int, taskCount: Int): String = when {
        goalCount > 0 && taskCount > 0 -> "Good morning — $goalCount goal${if (goalCount > 1) "s" else ""}, $taskCount task${if (taskCount > 1) "s" else ""} today"
        goalCount > 0 -> "Good morning — $goalCount active goal${if (goalCount > 1) "s" else ""}"
        else -> "Good morning — $taskCount task${if (taskCount > 1) "s" else ""} pending"
    }

    private fun buildBriefBody(goalTitles: List<String>, taskTitles: List<String>): String {
        val parts = mutableListOf<String>()
        if (goalTitles.isNotEmpty()) parts += goalTitles.joinToString(", ")
        if (taskTitles.isNotEmpty()) parts += taskTitles.joinToString(", ")
        return parts.joinToString(" · ")
    }
}
