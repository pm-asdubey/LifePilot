package com.lifepilot.domain.repository

import com.lifepilot.domain.model.UpdateStatus
import kotlinx.coroutines.flow.Flow

interface UpdateRepository {
    /** Emits the latest known update status from persistent cache. */
    fun observeUpdateStatus(): Flow<UpdateStatus>

    /** Performs a network check and persists the result. Silently no-ops on failure. */
    suspend fun checkForUpdate()
}
