package com.arshadshah.nimaz.core.monitoring

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TracedFlowTest {

    private val telemetry = RecordingTelemetry()

    @Test
    fun `the first emission is timed`() = runTest {
        flowOf(1, 2, 3).traceFirstEmission(telemetry, "load").toList()

        val call = telemetry.traceValues.single()
        assertThat(call.name).isEqualTo("load")
        assertThat(call.metric).isEqualTo(FIRST_EMISSION_METRIC)
        assertThat(call.value).isAtLeast(0L)
    }

    /**
     * Once, not once per value. A Room flow re-emits on every write to the table it watches, and
     * a screen open for a minute can emit dozens of times — each one would otherwise be filed as
     * a fresh "load" and drag the reported median to nearly zero.
     */
    @Test
    fun `later emissions are not timed`() = runTest {
        flowOf(1, 2, 3, 4, 5).traceFirstEmission(telemetry, "load").toList()

        assertThat(telemetry.traceValues).hasSize(1)
    }

    /**
     * A screen closed before its data arrives is absent from the numbers rather than recorded as
     * instant — which is the difference between "we have no measurement" and "it was fast".
     */
    @Test
    fun `a flow that never emits reports nothing`() = runTest {
        emptyFlow<Int>().traceFirstEmission(telemetry, "load").toList()

        assertThat(telemetry.traceValues).isEmpty()
    }

    @Test
    fun `every value passes through unchanged`() = runTest {
        val values = flowOf("a", "b", "c").traceFirstEmission(telemetry, "load").toList()

        assertThat(values).containsExactly("a", "b", "c").inOrder()
    }

    /** The clock starts at collection, not at construction — a cold flow does nothing until then. */
    @Test
    fun `an unconsumed flow reports nothing`() = runTest {
        flowOf(1).traceFirstEmission(telemetry, "load")

        assertThat(telemetry.traceValues).isEmpty()
    }

    /** Two collections of the same cold flow are two loads, because each one really is. */
    @Test
    fun `each collection is timed separately`() = runTest {
        val traced = flowOf(1).traceFirstEmission(telemetry, "load")
        traced.toList()
        traced.toList()

        assertThat(telemetry.traceValues).hasSize(2)
    }

    @Test
    fun `a failure before the first value reports nothing`() = runTest {
        val boom = flow<Int> { throw IllegalStateException("no rows") }

        runCatching { boom.traceFirstEmission(telemetry, "load").toList() }

        assertThat(telemetry.traceValues).isEmpty()
    }

    /**
     * The measurement spans the wait, not the subscription. A hot flow collected before anything
     * is published must record the gap — this is the case the whole helper exists for, and a
     * `flowOf` fixture cannot show it because it emits during collection setup.
     */
    @Test
    fun `the time waited for a hot source is measured`() = runTest {
        val source = MutableSharedFlow<Int>()
        val seen = mutableListOf<Int>()

        val job = launch {
            source.traceFirstEmission(telemetry, "load").collect { seen += it }
        }
        runCurrent()
        assertThat(telemetry.traceValues).isEmpty()

        source.emit(7)
        runCurrent()

        assertThat(seen).containsExactly(7)
        assertThat(telemetry.traceValues).hasSize(1)
        job.cancel()
    }
}
