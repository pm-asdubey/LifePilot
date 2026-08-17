package com.lifepilot.data.notification

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

/**
 * Guards the one piece of real logic in [AiTaskNotifier]: the "answer ready" notification is posted
 * only when the user is AWAY (app backgrounded). The foreground service start/stop are thin Android
 * plumbing wrapped in runCatching, so they no-op harmlessly in a JVM test.
 */
class AiTaskNotifierTest {

    private val context = mockk<Context>(relaxed = true)
    private val notificationHelper = mockk<NotificationHelper>(relaxed = true)
    private val foreground = mockk<ForegroundStateProvider>()
    private val notifier = AiTaskNotifier(context, notificationHelper, foreground)

    @Test
    fun `posts answer-ready notification when app is backgrounded`() {
        every { foreground.isForeground() } returns false

        notifier.onTurnFinished("conv-1", "Wedding planning")

        verify(exactly = 1) { notificationHelper.showAiAnswerReadyNotification("conv-1", "Wedding planning") }
    }

    @Test
    fun `does NOT notify when app is in the foreground`() {
        every { foreground.isForeground() } returns true

        notifier.onTurnFinished("conv-1", "Wedding planning")

        verify(exactly = 0) { notificationHelper.showAiAnswerReadyNotification(any(), any()) }
    }

    @Test
    fun `stopThinking never posts a result notification`() {
        notifier.stopThinking()

        verify(exactly = 0) { notificationHelper.showAiAnswerReadyNotification(any(), any()) }
    }
}
