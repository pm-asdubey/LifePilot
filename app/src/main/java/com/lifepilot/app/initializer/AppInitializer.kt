package com.lifepilot.app.initializer

import com.lifepilot.data.notification.NotificationHelper
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.data.worker.WorkManagerScheduler
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInitializer @Inject constructor(
    private val schemaEngine: SchemaEngine,
    private val profileRepository: ProfileRepository,
    private val preferenceManager: PreferenceManager,
    private val workManagerScheduler: WorkManagerScheduler,
    private val notificationHelper: NotificationHelper,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize() {
        notificationHelper.createNotificationChannels()
        workManagerScheduler.scheduleReminderEvaluation()
        workManagerScheduler.scheduleMorningBrief()

        scope.launch {
            try {
                schemaEngine.loadSchemas()
                Timber.d("Schemas loaded: ${schemaEngine.getAllObjectTypes()}")
                ensureDefaultProfile()
            } catch (e: Exception) {
                Timber.e(e, "App initialization failed")
            }
        }
    }

    private suspend fun ensureDefaultProfile() {
        val profiles = profileRepository.observeProfiles().firstOrNull() ?: emptyList()
        if (profiles.isEmpty()) {
            val profile = profileRepository.createProfile("My Profile", isPrimary = true)
            preferenceManager.setActiveProfileId(profile.profileId)
            Timber.d("Created default profile: ${profile.profileId}")
        } else {
            val active = preferenceManager.getActiveProfileId()
            if (active == null) {
                val primary = profiles.find { it.isPrimary } ?: profiles.first()
                preferenceManager.setActiveProfileId(primary.profileId)
            }
        }
    }
}
