package com.lifepilot.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifepilot.data.engine.RuleEngineImpl
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.repository.ObjectRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

@HiltWorker
class ReminderEvaluationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val objectRepository: ObjectRepository,
    private val preferenceManager: PreferenceManager,
    private val ruleEngine: RuleEngineImpl,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val profileId = preferenceManager.getActiveProfileId()
                ?: return Result.success()

            val objects = objectRepository.observeObjectsByProfile(profileId)
                .firstOrNull()
                ?: return Result.success()

            for (obj in objects) {
                ruleEngine.evaluateRemindersForObject(obj.objectId)
            }

            Timber.d("Reminder evaluation complete for ${objects.size} objects")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Reminder evaluation failed")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "reminder_evaluation"
    }
}
