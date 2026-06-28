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

        manager.createNotificationChannel(remindersChannel)
        manager.createNotificationChannel(generalChannel)
        manager.createNotificationChannel(morningBriefChannel)
        Timber.d("Notification channels created")
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
