package com.arshadshah.nimaz.core.monitoring

import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Pins the failure contract for ViewModel coroutines.
 *
 * The audit in #352 found 41 catch sites covering 293 `viewModelScope.launch`
 * calls — roughly 86% of launched coroutines had no failure path at all. In
 * `viewModelScope` (a `SupervisorJob` on `Dispatchers.Main.immediate`) an exception
 * in a child `launch` is *not* contained: it reaches the thread's uncaught handler
 * and kills the app. For flow collectors the failure mode is quieter but no better —
 * the collector dies, `isLoading` stays true forever, and nothing is reported.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SafeLaunchTest {

    private val telemetry = RecordingTelemetry()

    // viewModelScope runs on Dispatchers.Main.immediate, so the Main dispatcher has
    // to be a TestDispatcher or launchSafely's body never executes. runTest then
    // shares this scheduler, so advanceUntilIdle() drives both.
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class TestViewModel : ViewModel()

    @Test
    fun `a throwing block is contained and reported to both channels`() = runTest {
        val vm = TestViewModel()
        val boom = IllegalStateException("no such table: quran_ayah")

        vm.launchSafely(telemetry, domain = "quran", type = "load_surah") { throw boom }
        advanceUntilIdle()

        // Contained: reaching here at all means it did not escape to the uncaught handler.
        assertThat(telemetry.exceptions).containsExactly(boom)
        assertThat(telemetry.errors.single().domain).isEqualTo("quran")
        assertThat(telemetry.errors.single().type).isEqualTo("load_surah")
    }

    @Test
    fun `onFailure runs so the ViewModel can clear isLoading`() = runTest {
        val vm = TestViewModel()
        var loading = true

        vm.launchSafely(
            telemetry,
            domain = "quran",
            type = "load_surah",
            onFailure = { loading = false },
        ) { throw RuntimeException("boom") }
        advanceUntilIdle()

        assertThat(loading).isFalse()
    }

    @Test
    fun `a successful block reports nothing`() = runTest {
        val vm = TestViewModel()
        var ran = false

        vm.launchSafely(telemetry, "quran", "load_surah") { ran = true }
        advanceUntilIdle()

        assertThat(ran).isTrue()
        assertThat(telemetry.calls).isEmpty()
    }

    @Test
    fun `cancelling the job does not report a failure`() = runTest {
        // Readers cancel loads constantly as the user navigates. Reporting those
        // would bury real failures in noise.
        val vm = TestViewModel()

        val job = vm.launchSafely(telemetry, "hadith", "load_chapter") {
            delay(10_000)
        }
        job.cancelAndJoin()
        advanceUntilIdle()

        assertThat(telemetry.calls).isEmpty()
    }

    @Test
    fun `catchAndReport keeps the collector alive and reports the failure`() = runTest {
        val received = mutableListOf<Int>()
        val boom = IllegalStateException("row missing")

        val failing = flow {
            emit(1)
            emit(2)
            throw boom
        }

        CoroutineScope(dispatcher).launch {
            failing
                .catchAndReport(telemetry, domain = "dua", type = "load_category")
                .toList(received)
        }
        advanceUntilIdle()

        // Everything emitted before the throw still reached the collector, and the
        // failure was reported rather than silently ending the stream.
        assertThat(received).containsExactly(1, 2).inOrder()
        assertThat(telemetry.exceptions).containsExactly(boom)
        assertThat(telemetry.errors.single().domain).isEqualTo("dua")
    }

    @Test
    fun `catchAndReport can substitute a fallback value`() = runTest {
        val received = mutableListOf<Int>()

        flow<Int> { throw RuntimeException("boom") }
            .catchAndReport(telemetry, "dua", "load_category") { emit(-1) }
            .let { guarded -> CoroutineScope(dispatcher).launch { guarded.toList(received) } }
        advanceUntilIdle()

        assertThat(received).containsExactly(-1)
        assertThat(telemetry.errors).hasSize(1)
    }

    @Test
    fun `catchAndReport does not report cancellation`() = runTest {
        val upstream = MutableSharedFlow<Int>()

        val job = CoroutineScope(dispatcher).launch {
            upstream.catchAndReport(telemetry, "hadith", "observe").collect { }
        }
        advanceUntilIdle()
        job.cancelAndJoin()

        assertThat(telemetry.calls).isEmpty()
    }
}
