package com.lifepilot.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.lifepilot.app.initializer.AppInitializer
import com.lifepilot.app.logging.ScrubbingTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class LifePilotApplication : Application(), Configuration.Provider {

    @Inject lateinit var appInitializer: AppInitializer
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        initLogging()
        appInitializer.initialize()
    }

    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(ScrubbingTree())
        }
    }
}
