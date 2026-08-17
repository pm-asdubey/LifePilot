package com.lifepilot.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lifepilot.data.R
import com.lifepilot.domain.model.Reminder
import com.lifepilot.domain.model.ReminderPriority
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_REMINDERS = "lifepilot_reminders"
        const val CHANNEL_GENERAL = "lifepilot_general"
        const val CHANNEL_MORNING_BRIEF = "lifepilot_morning_brief"
        const val CHANNEL_AI = "lifepilot_ai"

        // Foreground "thinking" notification (ongoing) + the "answer ready" result notification.
        const val AI_THINKING_NOTIF_ID = 4242
        const val AI_READY_NOTIF_ID = 4243
    }

    fun createNotificationChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val remindersChannel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Deadline and renewal reminders"
            enableVibration(true)
        }

        val generalChannel = NotificationChannel(
            CHANNEL_GENERAL,
            "General",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "General app notifications"
        }

        val morningBriefChannel = NotificationChannel(
            CHANNEL_MORNING_BRIEF,
            "Morning Brief",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Daily planning summary at 9 AM"
        }

        val aiChannel = NotificationChannel(
            CHANNEL_AI,
            "AI Assistant",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Progress and results while the AI works in the background"
        }

        manager.createNotificationChannel(remindersChannel)
        manager.createNotificationChannel(generalChannel)
        manager.createNotificationChannel(morningBriefChannel)
        manager.createNotificationChannel(aiChannel)
        Timber.d("Notification channels created")
    }

    /** Ongoing, silent notification shown by the foreground service while the AI is working. */
    fun buildAiThinkingNotification(): android.app.Notification {
        createNotificationChannels()
        return NotificationCompat.Builder(context, CHANNEL_AI)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("LifePilot is thinking…")
            .setContentText("Working on your answer — you can switch apps.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(launchPendingIntent(conversationId = null, requestCode = AI_THINKING_NOTIF_ID))
            .build()
    }

    /** Result notification: tapping it opens the app to the conversation that just got an answer. */
    fun showAiAnswerReadyNotification(conversationId: String, conversationTitle: String) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_AI)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Your answer is ready")
                .setContentText(conversationTitle.ifBlank { "Tap to see LifePilot's reply" })
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(launchPendingIntent(conversationId, requestCode = AI_READY_NOTIF_ID))
                .build()
            NotificationManagerCompat.from(context).notify(AI_READY_NOTIF_ID, notification)
        } catch (e: SecurityException) {
            Timber.w(e, "No notification permission for AI answer")
        } catch (e: Exception) {
            Timber.e(e, "Error showing AI answer notification")
        }
    }

    fun cancelAiAnswerReady() {
        NotificationManagerCompat.from(context).cancel(AI_READY_NOTIF_ID)
    }

    private fun launchPendingIntent(conversationId: String?, requestCode: Int): PendingIntent? {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                conversationId?.let { putExtra("conversationId", it) }
            } ?: return null
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun showReminderNotification(
        reminderId: String,
        title: String,
        message: String,
        objectId: String?,
        priority: ReminderPriority = ReminderPriority.MEDIUM,
    ) {
        try {
            val notifPriority = when (priority) {
                ReminderPriority.CRITICAL, ReminderPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
                else -> NotificationCompat.PRIORITY_DEFAULT
            }

            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    objectId?.let { putExtra("objectId", it) }
                }

            val notificationId = reminderId.hashCode()

            val pendingIntent = launchIntent?.let {
                PendingIntent.getActivity(
                    context,
                    notificationId,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(notifPriority)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Timber.d("Notification fired: $title")
        } catch (e: SecurityException) {
            Timber.w(e, "No notification permission")
        } catch (e: Exception) {
            Timber.e(e, "Error showing notification for reminder $reminderId")
        }
    }

    fun showReminderNotification(reminder: Reminder) {
        showReminderNotification(
            reminderId = reminder.reminderId,
            title = reminder.title,
            message = reminder.message ?: reminder.title,
            objectId = reminder.objectId,
            priority = reminder.priority,
        )
    }
}
