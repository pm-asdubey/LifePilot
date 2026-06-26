package com.lifepilot.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleReminderEvaluation() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<ReminderEvaluationWorker>(
            repeatInterval = 12,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            ReminderEvaluationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun scheduleDocumentOcr(versionId: String) {
        val inputData = workDataOf(DocumentOcrWorker.KEY_VERSION_ID to versionId)
        val request = OneTimeWorkRequestBuilder<DocumentOcrWorker>()
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(
            "${DocumentOcrWorker.WORK_NAME_PREFIX}$versionId",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelAll() {
        workManager.cancelAllWork()
    }
}
