package com.arshadshah.nimaz.core.monitoring

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Timing for the loads that gate a first paint.
 *
 * [Telemetry.trace] measures a suspend call from entry to return, which is the right shape for
 * a computation and the wrong one for a screen load. Nearly every load in this app is a Room
 * `Flow` that is collected for the lifetime of the screen: wrapping the collection in a trace
 * would produce a duration that ends when the user navigates away, which measures how long they
 * looked at the screen rather than how long they waited for it.
 *
 * What matters is the **first emission** — the moment the screen can stop showing a spinner.
 * That is a measurement taken across a boundary `trace` cannot span, which is exactly what
 * [Telemetry.traceValue] exists for.
 */

/** The metric name every first-emission timing is filed under. One name, so the dashboards line up. */
const val FIRST_EMISSION_METRIC: String = "first_emission_ms"

/**
 * The metric name a completed operation's duration is filed under, for the cases where the work
 * happens somewhere [Telemetry.trace] cannot wrap — a click handler that runs on the caller's
 * thread and reports back through an event, most of all.
 */
const val DURATION_METRIC: String = "duration_ms"

/**
 * Reports how long this flow took to produce its **first** value, as [FIRST_EMISSION_METRIC] on
 * a trace called [name].
 *
 * Transparent otherwise: every value passes through untouched, the timing is taken once however
 * many emissions follow, and a flow that never emits reports nothing rather than reporting zero.
 * A screen that is opened and closed before its data arrives should be absent from the numbers,
 * not recorded as instant.
 *
 * The clock starts when collection starts, not when the flow is built, because a cold Room flow
 * does no work until someone collects it.
 *
 * ```
 * quranUseCases.getAyahsByPage(page)
 *     .traceFirstEmission(telemetry, PerfMonitor.Traces.QURAN_PAGE_LOAD)
 *     .collect { ayahs -> _state.update { it.copy(ayahs = ayahs, isLoading = false) } }
 * ```
 */
fun <T> Flow<T>.traceFirstEmission(
    telemetry: Telemetry,
    name: String,
): Flow<T> {
    val upstream = this
    return flow {
        val startedAt = System.nanoTime()
        var reported = false
        upstream.collect { value ->
            if (!reported) {
                reported = true
                telemetry.traceValue(
                    name = name,
                    metric = FIRST_EMISSION_METRIC,
                    value = (System.nanoTime() - startedAt) / 1_000_000,
                )
            }
            emit(value)
        }
    }
}
