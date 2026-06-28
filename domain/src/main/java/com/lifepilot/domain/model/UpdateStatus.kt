package com.lifepilot.domain.model

sealed class UpdateStatus {
    /** No check has been performed yet this session. */
    data object Unknown : UpdateStatus()

    /** A check is in progress. */
    data object Checking : UpdateStatus()

    /** The installed version is the latest. */
    data object UpToDate : UpdateStatus()

    /** A newer version is available. */
    data class UpdateAvailable(val info: UpdateInfo) : UpdateStatus()

    /** The check could not be completed (offline, timeout, rate-limited, etc.). */
    data object UnableToCheck : UpdateStatus()
}
