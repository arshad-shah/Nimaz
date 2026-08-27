package com.arshadshah.nimaz.core.monitoring

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Thin wrapper around Firebase Crashlytics.
 *
 * Every call is guarded so it safely no-ops when Firebase is not initialized —
 * for example debug or PR-check builds that ship without `google-services.json`.
 * This lets the rest of the app report errors unconditionally without having to
 * know whether Crashlytics is wired up in the current build variant.
 */
object CrashReporter {

    private val crashlytics: FirebaseCrashlytics?
        get() = runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()

    /** Records a non-fatal exception. Use in catch blocks that would otherwise swallow errors. */
    fun recordException(throwable: Throwable) {
        crashlytics?.recordException(throwable)
    }

    /** Adds a breadcrumb that is attached to subsequent crash reports. */
    fun log(message: String) {
        crashlytics?.log(message)
    }

    /** Sets a custom key that appears on crash reports, useful for diagnosing state-dependent crashes. */
    fun setCustomKey(key: String, value: String) {
        crashlytics?.setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Int) {
        crashlytics?.setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Boolean) {
        crashlytics?.setCustomKey(key, value)
    }
}
