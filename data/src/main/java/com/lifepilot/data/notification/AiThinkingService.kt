package com.lifepilot.data.notification

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Minimal foreground service that keeps the process alive (and unfrozen by aggressive OEMs) while an
 * AI turn runs in the ViewModel's coroutine. It performs no work itself — [AiTaskNotifier] starts it
 * on turn start and stops it on completion. Being a foreground service tells Android "the user is
 * waiting on this", so switching apps no longer pauses the network call.
 */
@AndroidEntryPoint
class AiThinkingService : Service() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runCatching {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            ServiceCompat.startForeground(
                this,
                NotificationHelper.AI_THINKING_NOTIF_ID,
                notificationHelper.buildAiThinkingNotification(),
                type,
            )
        }.onFailure { Timber.w(it, "AI foreground service startForeground failed") }
        return START_NOT_STICKY
    }
}
