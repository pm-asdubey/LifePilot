package com.lifepilot.data.notification

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges an AI conversation turn to the OS: starts a foreground service so the request survives the
 * user switching apps, and — if the app is in the background when the answer lands — posts an
 * "answer ready" notification that deep-links back to the conversation.
 *
 * Injected into the ViewModel so the orchestration logic stays in one place; this class only owns the
 * Android service/notification plumbing.
 */
@Singleton
class AiTaskNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationHelper: NotificationHelper,
    private val foregroundStateProvider: ForegroundStateProvider,
) {
    /** Call when an AI turn begins — keeps the process alive if the user backgrounds the app. */
    fun onTurnStarted() {
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, AiThinkingService::class.java))
        }.onFailure { Timber.w(it, "Could not start AI foreground service") }
    }

    /** Call when the answer is ready — stops the service and notifies only if app is backgrounded. */
    fun onTurnFinished(conversationId: String, conversationTitle: String) {
        stopThinking()
        if (!foregroundStateProvider.isForeground()) {
            notificationHelper.showAiAnswerReadyNotification(conversationId, conversationTitle)
        }
    }

    /** Stops the foreground service without posting a result (errors, cancellation, cleanup). */
    fun stopThinking() {
        runCatching { context.stopService(Intent(context, AiThinkingService::class.java)) }
            .onFailure { Timber.w(it, "Could not stop AI foreground service") }
    }
}
