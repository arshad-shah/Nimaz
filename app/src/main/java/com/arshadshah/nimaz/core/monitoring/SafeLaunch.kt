package com.arshadshah.nimaz.core.monitoring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * The failure path for ViewModel coroutines.
 *
 * `viewModelScope` is a `SupervisorJob` on `Dispatchers.Main.immediate`: an exception
 * thrown inside a child `launch` is **not** contained. It reaches the thread's
 * uncaught handler and kills the app. A bare
 * `viewModelScope.launch { useCase.doThing() }` is therefore one Room error away
 * from a crash with no breadcrumb — and the audit in #352 found 293 such launches
 * against 41 catch sites.
 *
 * Use [launchSafely] instead of `viewModelScope.launch` for anything that touches a
 * use case, and [catchAndReport] on every collected flow.
 */

/**
 * Launches [block] in `viewModelScope`, reporting any failure to both monitoring
 * channels and running [onFailure] so the ViewModel can clear `isLoading` or set an
 * error on its state.
 *
 * Cancellation is re-thrown untouched and never reported — a load abandoned because
 * the user navigated away is normal control flow.
 *
 * Returns the [Job] so callers that need cancel-and-replace semantics (one handle per
 * surface, per §4.1) can hold it:
 *
 * ```kotlin
 * private var readerJob: Job? = null
 *
 * private fun loadChapter(id: String) {
 *     readerJob?.cancel()
 *     readerJob = launchSafely(telemetry, "hadith", "load_chapter",
 *         onFailure = { _state.update { it.copy(isLoading = false, error = …) } },
 *     ) { … }
 * }
 * ```
 */
fun ViewModel.launchSafely(
    telemetry: Telemetry,
    domain: String,
    type: String,
    onFailure: (Throwable) -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
): Job = viewModelScope.launch {
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        telemetry.failure(domain, type, throwable)
        onFailure(throwable)
    }
}

/**
 * Reports an upstream failure to both channels and lets [fallback] decide what the
 * collector sees.
 *
 * Note this **terminates** the flow, exactly as [catch] does — that is Kotlin's
 * semantics, not a choice made here. Where an outer stream must survive an inner
 * failure (a `flatMapLatest` over a settings flow, say), apply this to the **inner**
 * flow, inside the operator:
 *
 * ```kotlin
 * language.flatMapLatest { lang ->
 *     useCases.getTopics(lang).catchAndReport(telemetry, "help", "load_topics") { emit(emptyList()) }
 * }.collect { … }
 * ```
 *
 * Applying it outside the `flatMapLatest` instead ends the whole chain on the first
 * transient error and never recovers — the defect this helper exists to make hard to
 * write.
 */
fun <T> Flow<T>.catchAndReport(
    telemetry: Telemetry,
    domain: String,
    type: String,
    fallback: suspend FlowCollector<T>.(Throwable) -> Unit = {},
): Flow<T> = catch { throwable ->
    // `Flow.catch` already declines to catch CancellationException, but failure()
    // guards it too so the contract holds however this is called.
    telemetry.failure(domain, type, throwable)
    fallback(throwable)
}
