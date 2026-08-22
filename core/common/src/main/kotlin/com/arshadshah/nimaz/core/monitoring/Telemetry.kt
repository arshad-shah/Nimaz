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
 *  - `AppAnalytics.logOnboardingStep` fired **zero times in production**: the event that
 *    triggers it was emitted by no screen, so the drop-off funnel was empty.
 *  - `logFeatureUsed("zakat", "calculate")` sat on a branch no screen could reach, so the
 *    dashboard reported that nobody calculates zakat.
 *
 * Both are fixed — `OnboardingScreen` now drives the funnel from the pager's `snapshotFlow`,
 * and `ZakatCalculatorScreen` dispatches `Recalculate` — and both are stated in the past tense
 * deliberately. They are kept as the argument for the seam rather than as a bug list: what let
 * each of them survive was that a static call cannot be asserted in a test, and that is the
 * property this interface exists to remove.
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

    /**
     * "Ask with Proof" answered a question: how many citations resolved locally into proof
     * cards, how long the round trip took, and the answer's confidence. The question text is
     * never recorded.
     */
    fun aiAnswered(proofCount: Int, durationMs: Long, confidence: String)

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

    // -- Announcements -----------------------------------------------------

    /**
     * The announcement funnel: shown -> (cta clicked | dismissed), plus the rejection case.
     *
     * All four were static calls in `HomeViewModel`, which made the funnel untestable end to
     * end — and a funnel nobody can assert is how this codebase already lost one: §6.1 records
     * `logOnboardingStep` as never having fired.
     */
    fun announcementShown(id: String, type: String)

    fun announcementCtaClicked(id: String, route: String?)

    fun announcementDismissed(id: String)

    /** An announcement carried a route the app refused to open. */
    fun announcementRouteRejected(id: String, route: String?)

    // -- User properties ---------------------------------------------------

    /**
     * A user property, for segmenting every later event (calculation method, madhab, …).
     *
     * A null [value] clears the property, which is how Firebase distinguishes "not set" from
     * "set to empty" — worth preserving, because a segment that silently becomes the empty
     * string merges with every other unset user.
     */
    fun userProperty(name: String, value: String?)

    /** The user fired a test notification from settings; [allPrayers] distinguishes the two buttons. */
    fun testNotification(allPrayers: Boolean)

    // -- Onboarding --------------------------------------------------------

    /**
     * A step of the first-run flow was reached.
     *
     * On the seam rather than left on the object for a specific reason: this is the event that
     * once fired zero times in production, because nothing dispatched what triggers it. The
     * dispatch was repaired in `OnboardingScreen`; what had let it stay broken unnoticed was
     * that a static call cannot be asserted in a test. Routing it here is what lets
     * `OnboardingFunnelTest` hold the repair in place.
     */
    fun onboardingStep(page: Int)

    /** The first-run flow finished, with the permissions the user granted on the way through. */
    fun onboardingCompleted(
        locationGranted: Boolean,
        notificationGranted: Boolean,
        batteryOptimizationDisabled: Boolean,
    )

    // -- Performance -------------------------------------------------------

    /**
     * Measures [block] as a Firebase Performance custom trace named [name].
     *
     * This exists because performance monitoring was, in practice, unreachable. The
     * SDK was wired up and paid for, [PerfMonitor] wrapped it correctly, and the
     * whole thing ran to **two** trace constants at **four** call sites, all in
     * `:app` — because [PerfMonitor] is an `object` and §6.1 tells every ViewModel
     * and use case not to call the objects. There was no seam, so a feature module
     * could not record a trace without committing a documented deviation. The
     * absence read as "we chose not to instrument the features"; it was really
     * "the features had no way to".
     *
     * Names come from [PerfMonitor.Traces], for the same reason feature and action
     * strings come from the catalog: a trace whose name drifts between call sites
     * splits into two graphs that each describe half the traffic. Firebase caps a
     * trace name at 100 characters and silently drops anything longer.
     *
     * Returns whatever [block] returns, and stops the trace on the way out of a
     * throw as well as a return — an operation that fails is exactly the one whose
     * duration is worth having.
     */
    suspend fun <T> trace(name: String, block: suspend () -> T): T = block()

    /**
     * Records a single measurement against a trace without wrapping a block —
     * for a duration already known, such as one measured across a callback
     * boundary that `trace` cannot span.
     */
    fun traceValue(name: String, metric: String, value: Long)

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
