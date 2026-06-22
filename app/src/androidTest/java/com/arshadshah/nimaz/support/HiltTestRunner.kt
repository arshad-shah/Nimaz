package com.arshadshah.nimaz.support

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Instrumentation runner that boots [HiltTestApplication] instead of the production
 * [com.arshadshah.nimaz.NimazApp].
 *
 * Why: NimazApp.onCreate() initializes Firebase, configures WorkManager with a
 * [HiltWorkerFactory], and kicks off [com.arshadshah.nimaz.core.init.AppInitializer]
 * (locale, prayer-notification scheduling, adhan download). None of that is wanted —
 * or reliable — on a headless emulator. HiltTestApplication still hosts the full Hilt
 * object graph (so `@HiltAndroidTest` classes can inject real DAOs, repositories,
 * schedulers, etc.) but skips the production bootstrap. Worker tests configure
 * WorkManager explicitly via [androidx.work.testing.WorkManagerTestInitHelper].
 *
 * Registered as `testInstrumentationRunner` in app/build.gradle.kts.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
