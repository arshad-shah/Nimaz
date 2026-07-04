package com.arshadshah.nimaz.support

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.runner.AndroidJUnitRunner
import com.arshadshah.nimaz.core.util.BootReceiver
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
        // Disable the manifest-registered BootReceiver for the test process.
        //
        // BootReceiver is an @AndroidEntryPoint whose generated onReceive() injects
        // Hilt fields *before* our code runs. Under HiltTestApplication the Hilt
        // component doesn't exist until a test's HiltAndroidRule.inject() creates it,
        // so any broadcast delivered before the first test — e.g. a past-due exact
        // AlarmManager alarm left over from a prior run's MainActivity scheduling
        // prayer notifications — injects into a missing component and crashes the whole
        // instrumentation process ("The component was not created"), running 0 tests.
        //
        // No test exercises BootReceiver (boot/alarm rescheduling isn't under test;
        // scheduler logic is verified directly), so disabling it here — on the main
        // thread during bindApplication, before the looper dispatches any queued
        // broadcast — removes the crash deterministically without affecting the
        // shipped app. Best-effort: never let this fail the run.
        context?.let { ctx ->
            runCatching {
                ctx.packageManager.setComponentEnabledSetting(
                    ComponentName(ctx, BootReceiver::class.java),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
