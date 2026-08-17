package com.lifepilot.domain.model

/**
 * Single source of truth for how an incoming metadata value combines with an existing one.
 *
 * Previously this logic was duplicated in `HomeViewModel` and `ActionPlanExecutorImpl`, which risked
 * the two drifting. [UpdateMode.APPEND] concatenates onto the existing value on a new line (unless
 * there is nothing to append to); every other mode replaces.
 */
object MetadataMerge {
    fun merge(mode: UpdateMode, existing: String?, incoming: String): String =
        if (mode == UpdateMode.APPEND && !existing.isNullOrBlank()) "$existing\n$incoming" else incoming
}
