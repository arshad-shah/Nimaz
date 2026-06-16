package com.arshadshah.nimaz

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.arshadshah.nimaz.core.init.AppInitializer
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NimazApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appInitializer: AppInitializer

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        AppAnalytics.init(this)
        // Keep dev crashes out of production Crashlytics data.
        CrashReporter.setCollectionEnabled(!BuildConfig.DEBUG)
        CrashReporter.log("NimazApp.onCreate")
        CrashReporter.setCustomKey("db_schema_version", NimazDatabase.SCHEMA_VERSION)
        appInitializer.initialize()
    }
}
