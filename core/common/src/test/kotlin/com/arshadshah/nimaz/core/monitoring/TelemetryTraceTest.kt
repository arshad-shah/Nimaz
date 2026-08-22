package com.arshadshah.nimaz.core.monitoring

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Pins the performance channel's contract.
 *
 * Performance monitoring was shipping in the app and measuring almost nothing: two trace
 * constants, four call sites, all of them in `:app`. The cause was structural rather than
 * neglect — [PerfMonitor] is an `object`, §6.1 tells ViewModels and use cases not to call the
 * objects, and [Telemetry] had no perf method, so a feature module had no legal way to record a
 * trace at all. `Telemetry.trace` is that way, and these are the properties a caller is entitled
 * to assume when they wrap a load in it.
 */
class TelemetryTraceTest {

    private val telemetry = RecordingTelemetry()

    @Test
    fun `trace runs the block and returns its value`() = runTest {
        val result = telemetry.trace(PerfMonitor.Traces.QURAN_PAGE_LOAD) { 42 }

        assertThat(result).isEqualTo(42)
    }

    @Test
    fun `trace records the name it was given`() = runTest {
        telemetry.trace(PerfMonitor.Traces.LIBRARY_SEARCH) { }

        assertThat(telemetry.traces.map { it.name })
            .containsExactly(PerfMonitor.Traces.LIBRARY_SEARCH)
    }

    /**
     * The fake must **run** the block, not merely record the call.
     *
     * A double that recorded and returned without invoking would make every test that traces a
     * load see no data and pass anyway — the fake introducing the blindness it exists to remove.
     * This asserts the side effect rather than the return value, because a `trace` that returned
     * correctly while skipping side effects would still be broken.
     */
    @Test
    fun `the recording double actually invokes the traced block`() = runTest {
        var ran = false

        telemetry.trace(PerfMonitor.Traces.SYNC_EXPORT) { ran = true }

        assertThat(ran).isTrue()
    }

    /**
     * A trace that wraps other telemetry reads in the order the work happened.
     *
     * Worth pinning because the obvious implementation — record *after* the block — reverses it,
     * and a caller reading `calls` to reconstruct a sequence would silently get it backwards.
     */
    @Test
    fun `a trace is recorded before the work it wraps`() = runTest {
        telemetry.trace(PerfMonitor.Traces.TAFSEER_LOAD) {
            telemetry.featureUsed("tafseer", "load")
        }

        assertThat(telemetry.calls.map { it::class }).containsExactly(
            TelemetryCall.Trace::class,
            TelemetryCall.FeatureUsed::class,
        ).inOrder()
    }

    /**
     * A throwing block propagates rather than being swallowed.
     *
     * An operation that fails is exactly the one whose duration is worth having, but a `trace`
     * that ate the exception would convert every traced failure into a silent success — far
     * worse than not tracing at all.
     */
    @Test
    fun `a failing block propagates its exception through trace`() = runTest {
        val boom = IllegalStateException("no such table: tafseer")

        val thrown = runCatching {
            telemetry.trace(PerfMonitor.Traces.TAFSEER_LOAD) { throw boom }
        }.exceptionOrNull()

        assertThat(thrown).isSameInstanceAs(boom)
        assertThat(telemetry.traces.map { it.name })
            .containsExactly(PerfMonitor.Traces.TAFSEER_LOAD)
    }

    @Test
    fun `traceValue records the metric and its value`() {
        telemetry.traceValue(PerfMonitor.Traces.CONTENT_ARTIFACT_INSTALL, "rows", 12_480)

        assertThat(telemetry.traceValues).containsExactly(
            TelemetryCall.TraceValue(
                PerfMonitor.Traces.CONTENT_ARTIFACT_INSTALL, "rows", 12_480,
            )
        )
    }

    /**
     * Every catalogued name is distinct and within Firebase's 100-character cap.
     *
     * Firebase silently drops a longer name, and two constants sharing a value would merge two
     * measurements into one graph that describes neither — the same failure the feature/action
     * catalog exists to prevent, one layer over.
     */
    @Test
    fun `trace names are unique and within the Firebase length limit`() {
        val names = PerfMonitor.Traces::class.java.declaredFields
            .filter { it.type == String::class.java }
            .map { it.isAccessible = true; it.get(PerfMonitor.Traces) as String }

        assertThat(names).hasSize(names.toSet().size)
        assertThat(names.filter { it.length > 100 }).isEmpty()
        assertThat(names.filter { it.isBlank() }).isEmpty()
        // A floor: the catalog ran to two entries for as long as it did precisely because
        // nothing said how few that was.
        assertThat(names.size).isAtLeast(15)
    }
}
