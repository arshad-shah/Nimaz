package com.arshadshah.nimaz

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NimazApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: NimazWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Initialize WorkManager with the custom configuration
        WorkManager.initialize(
            this,
            workManagerConfiguration
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}