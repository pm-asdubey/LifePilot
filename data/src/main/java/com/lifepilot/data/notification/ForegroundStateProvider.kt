package com.lifepilot.data.notification

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reports whether the app is currently in the foreground. Extracted behind a class so
 * [AiTaskNotifier]'s "notify only when the user is away" decision is unit-testable (the real
 * implementation reads the process lifecycle, which isn't available in a plain JVM test).
 */
@Singleton
open class ForegroundStateProvider @Inject constructor() {
    open fun isForeground(): Boolean = runCatching {
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }.getOrDefault(true)
}
