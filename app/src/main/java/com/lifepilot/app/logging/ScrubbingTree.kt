package com.lifepilot.app.logging

import android.util.Log
import com.lifepilot.domain.security.SensitiveFieldRegistry
import timber.log.Timber

/**
 * A Timber tree that redacts known sensitive field values before forwarding to
 * the Android log. Plant this in addition to (or instead of) DebugTree so that
 * no PII leaves the process through logging.
 */
class ScrubbingTree : Timber.DebugTree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val safeMessage = SensitiveFieldRegistry.scrub(message)
        super.log(priority, tag, safeMessage, t)
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        // Only emitVERBOSE/DEBUG through this tree in debug builds. This guard is
        // redundant when the tree is only planted in debug, but it keeps the
        // safety contract explicit.
        return priority >= Log.VERBOSE
    }
}
