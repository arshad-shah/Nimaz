package com.arshadshah.nimaz.core.monitoring

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The other three Firebase wrappers, held to the same promise as `AppAnalytics`: safe no-ops when
 * Firebase is not initialised.
 *
 * `AppAnalyticsSafetyTest` explains why that matters. These three add two wrinkles of their own.
 *
 * **`PerfMonitor` hands back a handle.** `newTrace` returns null when Performance is unavailable,
 * and every caller then passes that null straight to `stop`. So the null path is not an edge
 * case — in a build without `google-services.json` it is the *only* path, which means `stop(null)`
 * has to be ordinary rather than defensive. `trace {}` and `traceSuspend {}` wrap that in a
 * `finally`, so a wrapper that threw on a null handle would convert "no performance monitoring"
 * into "every measured block fails after doing its work".
 *
 * **`FirebaseTelemetry` is the seam the whole app logs through.** It is a pass-through by design
 * — the KDoc says so — but "thin" is a claim about the code, not a guarantee at runtime: every
 * one of its twenty methods reaches an object that talks to Firebase, and a ViewModel calling
 * `telemetry.featureUsed(...)` in a debug build must not be the thing that crashes it.
 *
 * The value a trace returns is asserted, because that is the one thing here that is not
 * fire-and-forget: `trace {}` exists to measure a block *and give you its result*, and a wrapper
 * that swallowed the value would be caught by nothing else.
 */
@RunWith(RobolectricTestRunner::class)
class FirebaseWrapperSafetyTest {

    // ---- CrashReporter ----

    @Test
    fun `recording an exception without Crashlytics is a no-op`() {
        CrashReporter.recordException(IllegalStateException("boom"))
    }

    @Test
    fun `a breadcrumb without Crashlytics is a no-op`() {
        CrashReporter.log("about to do the risky thing")
    }

    @Test
    fun `every custom-key overload is a no-op`() {
        CrashReporter.setCustomKey("screen", "Quran")
        CrashReporter.setCustomKey("surah", 18)
        CrashReporter.setCustomKey("offline", true)
    }

    // ---- PerfMonitor ----

    @Test
    fun `starting a trace without Performance yields no handle rather than throwing`() {
        assertThat(PerfMonitor.newTrace(PerfMonitor.Traces.QURAN_SURAH_LOAD)).isNull()
    }

    @Test
    fun `stopping a trace that was never started is ordinary, not an error`() {
        // In a build without google-services.json this is the only path there is.
        PerfMonitor.stop(null)
        PerfMonitor.stop(null, attributes = mapOf("k" to "v"), metrics = mapOf("n" to 1L))
    }

    @Test
    fun `a measured block still runs, and still returns its value`() {
        // The reason `trace {}` exists is to measure a block *and* hand back its result.
        val result = PerfMonitor.trace("test_trace") { 6 * 7 }

        assertThat(result).isEqualTo(42)
    }

    @Test
    fun `a measured block that throws propagates its own failure, not the wrapper's`() {
        val thrown = runCatching {
            PerfMonitor.trace<Unit>("test_trace") { throw IllegalStateException("from the block") }
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
        assertThat(thrown).hasMessageThat().isEqualTo("from the block")
    }

    @Test
    fun `a suspending measured block returns its value too`() = runTest {
        val result = PerfMonitor.traceSuspend("test_trace") { "done" }

        assertThat(result).isEqualTo("done")
    }

    @Test
    fun `a suspending measured block that throws still stops its trace`() = runTest {
        val thrown = runCatching {
            PerfMonitor.traceSuspend<Unit>("test_trace") { error("from the block") }
        }.exceptionOrNull()

        assertThat(thrown).hasMessageThat().isEqualTo("from the block")
    }

    // ---- FirebaseTelemetry ----

    private val telemetry: Telemetry = FirebaseTelemetry()

    @Test
    fun `every recording method on the production telemetry is a no-op`() {
        telemetry.featureUsed("quran", "open_surah")
        telemetry.featureUsed("quran", null)
        telemetry.settingChanged("calculation_method", "MWL")
        telemetry.search("quran", 6)
        telemetry.aiAnswered(proofCount = 2, durationMs = 400, confidence = "high")
        telemetry.prayerTracked("FAJR", "PRAYED", isJamaah = true)
        telemetry.fastTracked("completed", "ramadan")
        telemetry.fastTracked("completed", null)
        telemetry.error("quran", "read_failed", "boom")
        telemetry.error("quran", "read_failed", null)
        telemetry.recordException(IllegalStateException("boom"))
        telemetry.breadcrumb("a breadcrumb")
        telemetry.userProperty("post_notif_granted", "true")
        telemetry.userProperty("post_notif_granted", null)
        telemetry.testNotification(allPrayers = true)
    }

    @Test
    fun `every announcement method on the production telemetry is a no-op`() {
        telemetry.announcementShown("a1", "update")
        telemetry.announcementCtaClicked("a1", "Route.Home")
        telemetry.announcementCtaClicked("a1", null)
        telemetry.announcementDismissed("a1")
        telemetry.announcementRouteRejected("a1", "nonsense")
        telemetry.announcementRouteRejected("a1", null)
    }

    @Test
    fun `the onboarding methods on the production telemetry are no-ops`() {
        telemetry.onboardingStep(1)
        telemetry.onboardingCompleted(
            locationGranted = true,
            notificationGranted = true,
            batteryOptimizationDisabled = false,
        )
    }

    @Test
    fun `a traced block through the production telemetry returns its value`() = runTest {
        val result = telemetry.trace("test_trace") { "measured" }

        assertThat(result).isEqualTo("measured")
    }

    @Test
    fun `recording a value against a trace is a no-op`() {
        telemetry.traceValue("test_trace", metric = "rows", value = 120)
    }
}
