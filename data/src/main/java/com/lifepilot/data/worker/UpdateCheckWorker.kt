package com.lifepilot.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifepilot.domain.repository.UpdateRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val updateRepository: UpdateRepository,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "update_check"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<UpdateCheckWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }

    override suspend fun doWork(): Result {
        return runCatching {
            updateRepository.checkForUpdate()
            Result.success()
        }.onFailure { e ->
            Timber.w(e, "UpdateCheckWorker: unexpected failure")
        }.getOrElse { Result.success() } // Never retry — check runs again on next launch
    }
}
