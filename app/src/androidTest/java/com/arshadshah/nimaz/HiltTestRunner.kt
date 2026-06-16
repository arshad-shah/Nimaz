package com.arshadshah.nimaz

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom instrumentation runner that swaps in [HiltTestApplication] so Hilt can
 * inject test components. Wired via `testInstrumentationRunner` in build.gradle.
 *
 * NOTE: instrumented tests are not executed in the current CI lane (no
 * emulator). They are compiled for sanity via `assembleDebugAndroidTest`, and
 * are ready to run on a device / Firebase Test Lab when that lane is added.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
