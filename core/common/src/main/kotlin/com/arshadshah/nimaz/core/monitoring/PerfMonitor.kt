package com.arshadshah.nimaz.core.monitoring

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

/**
 * Thin wrapper around Firebase Performance Monitoring.
 *
 * Like [AppAnalytics] and [CrashReporter], every call is guarded so it safely
 * no-ops when Firebase is not initialized (builds without `google-services.json`,
 * where the `firebase-perf` Gradle plugin is also not applied). Firebase
 * Performance already auto-collects the app-start trace, foreground/background
 * traces, screen rendering (slow/frozen frames) and HTTP/S network requests;
 * this helper layers **custom traces** on top for the compute-heavy code paths
 * we care about (start-up, prayer-time calculation, notification scheduling) so
 * we can see how long they take in the field and on which devices.
 *
 * Custom attributes (max 5/trace, 40-char name, 100-char value) are good for
 * slicing a trace; custom metrics (max 32/trace) are good for counts attached
 * to a trace's duration.
 */
object PerfMonitor {

    private val performance: FirebasePerformance?
        get() = runCatching { FirebasePerformance.getInstance() }.getOrNull()

    /** Creates and starts a custom trace, or null when Performance is unavailable. */
    fun newTrace(name: String): Trace? =
        runCatching { performance?.newTrace(name)?.also { it.start() } }.getOrNull()

    /** Stops a trace, optionally attaching attributes (for slicing) and metrics (counts). */
    fun stop(
        trace: Trace?,
        attributes: Map<String, String> = emptyMap(),
        metrics: Map<String, Long> = emptyMap(),
    ) {
        trace ?: return
        runCatching {
            attributes.forEach { (k, v) -> trace.putAttribute(k.take(40), v.take(100)) }
            metrics.forEach { (k, v) -> trace.putMetric(k.take(100), v) }
            trace.stop()
        }
    }

    /** Measures a synchronous block of work as a custom trace. */
    inline fun <T> trace(name: String, block: () -> T): T {
        val trace = newTrace(name)
        return try {
            block()
        } finally {
            stop(trace)
        }
    }

    /** Measures a suspending block of work as a custom trace. */
    suspend fun <T> traceSuspend(name: String, block: suspend () -> T): T {
        val trace = newTrace(name)
        return try {
            block()
        } finally {
            stop(trace)
        }
    }

    /** Custom trace names (Firebase limit: 100 chars). */
    object Traces {
        const val APP_INITIALIZE = "app_initialize"
        const val NOTIFICATION_SCHEDULE = "notification_schedule"
    }
}
