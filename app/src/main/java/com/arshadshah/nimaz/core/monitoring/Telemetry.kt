package com.arshadshah.nimaz.core.monitoring

import kotlinx.coroutines.CancellationException

/**
 * An injectable seam over [AppAnalytics] and [CrashReporter].
 *
 * Both of those are Kotlin `object`s holding a static [android.content.Context].
 * They no-op safely in unit tests, so they never *blocked* testing — but they made
 * it impossible for a test to assert that an action had been logged. Two live
 * defects survived because of exactly that:
 *
 *  - `AppAnalytics.logOnboardingStep` has **never fired in production**, because the
 *    event that triggers it is emitted by no screen. The drop-off funnel is empty.
 *  - `logFeatureUsed("zakat", "calculate")` sits on a branch no screen can reach, so
 *    the dashboard reports that nobody calculates zakat.
 *
 * Depend on this interface from ViewModels and use cases; bind the Firebase-backed
 * implementation in production and `RecordingTelemetry` in tests.
 *
 * Method names deliberately drop the `log` prefix — at a call site
 * `telemetry.featureUsed(...)` reads better than `telemetry.logFeatureUsed(...)`,
 * and the shorter names keep the [failure] pairing prominent.
 */
interface Telemetry {

    // -- Usage -------------------------------------------------------------

    /** A feature-usage event for actions that are not screens (counters, toggles, …). */
    fun featureUsed(feature: String, action: String? = null)

    /** A setting the user changed, and its new value. */
    fun settingChanged(setting: String, value: String)

    /**
     * A search that actually ran. The raw query is deliberately never recorded —
     * only the active filter and the query length — to keep user input out of
     * analytics. Call this once per completed search, not per keystroke.
     */
    fun search(filter: String, queryLength: Int)

    /** A prayer's tracker status changed — the app's core engagement signal. */
    fun prayerTracked(prayer: String, status: String, isJamaah: Boolean = false)

    /** A fasting record changed. */
    fun fastTracked(action: String, fastType: String? = null)

    // -- Failure -----------------------------------------------------------

    /**
     * A non-fatal error, recorded as an analytics event so error *frequency* and the
     * affected user share are visible in dashboards.
     *
     * Prefer [failure], which also captures the stack trace.
     */
    fun error(domain: String, type: String, message: String? = null)

    /** Records a non-fatal throwable with its stack trace. Prefer [failure]. */
    fun recordException(throwable: Throwable)

    // -- Diagnostics -------------------------------------------------------

    /** A breadcrumb attached to the next crash report. */
    fun breadcrumb(message: String)

    /** A key/value pinned to crash reports, e.g. the active calculation method. */
    fun customKey(key: String, value: String)

    // -- The pairing -------------------------------------------------------

    /**
     * Reports a failure to **both** channels: the stack trace to Crashlytics and the
     * frequency to analytics. `AppAnalytics`'s own documentation describes this as
     * the intended pairing, but across the ViewModel layer most catch sites reach one
     * channel or neither. This is the entry point every `catch` should use.
     *
     * [CancellationException] is ignored: coroutine cancellation is normal control
     * flow, not a failure. A reader ViewModel cancels loads every time the user
     * navigates away, and reporting those would bury real failures in noise.
     */
    fun failure(domain: String, type: String, throwable: Throwable) {
        if (throwable is CancellationException) return
        recordException(throwable)
        error(domain, type, throwable.message)
    }
}
