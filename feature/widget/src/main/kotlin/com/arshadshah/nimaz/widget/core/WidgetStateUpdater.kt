package com.arshadshah.nimaz.widget.core

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import com.arshadshah.nimaz.core.monitoring.CrashReporter

/**
 * Persist [newState] for every entry in [glanceIds] through [definition], then
 * trigger a recomposition of [widget].
 *
 * Replaces the identical private `setWidgetState` that lived in every widget
 * worker.
 */
suspend fun <T> updateWidgetState(
    context: Context,
    widget: GlanceAppWidget,
    definition: GlanceStateDefinition<T>,
    glanceIds: List<GlanceId>,
    newState: T,
) {
    glanceIds.forEach { glanceId ->
        updateAppWidgetState(
            context = context,
            definition = definition,
            glanceId = glanceId,
            updateState = { newState },
        )
    }
    widget.updateAll(context)
}

/**
 * The refresh every widget worker does: find the placed widgets, load, publish, and on failure
 * decide whether to keep what is on screen, and whether to retry.
 *
 * All six workers wrote this out — the same `getGlanceIds` / `isEmpty` / try / `CrashReporter`
 * / `runAttemptCount < 3` block, differing only in which widget, which state definition and
 * which data source (audit §1.5, which named two of the six; the shape is common to all).
 * The differences are the parameters.
 *
 * A worker whose widget is not on any home screen returns success without loading: there is
 * nothing to publish to, and doing the work anyway is what a periodic job must not do.
 *
 * @param success builds the state to publish from a freshly loaded payload.
 * @param error builds the state to publish when loading threw and there is nothing worth keeping.
 * @param hasData answers "is what is already on screen real data?" for a persisted state. It
 *   decides whether a failed refresh replaces the widget with its error frame or leaves the last
 *   good reading alone; see the failure branch below. The default answers no, which is the old
 *   behaviour — a worker that does not pass one always publishes the error.
 */
suspend fun <S> CoroutineWorker.refreshWidget(
    context: Context,
    widget: GlanceAppWidget,
    definition: GlanceStateDefinition<S>,
    widgetClass: Class<out GlanceAppWidget>,
    workerName: String,
    success: suspend () -> S,
    error: (String?) -> S,
    hasData: (S) -> Boolean = { false },
): ListenableWorker.Result {
    // Inside its own try: this reaches out to the AppWidget host, which is a process that can be
    // busy or not yet up (a refresh right after boot). It threw straight out of the worker
    // before, which is a failure with no retry — the widget then waited a whole period for its
    // next chance.
    val glanceIds = try {
        GlanceAppWidgetManager(context).getGlanceIds(widgetClass)
    } catch (e: Exception) {
        CrashReporter.log("$workerName could not list its widgets")
        CrashReporter.recordException(e)
        return retryUntilExhausted()
    }
    if (glanceIds.isEmpty()) return ListenableWorker.Result.success()

    return try {
        updateWidgetState(context, widget, definition, glanceIds, success())
        ListenableWorker.Result.success()
    } catch (e: Exception) {
        CrashReporter.log("$workerName failed")
        CrashReporter.recordException(e)
        publishErrorIfNothingToKeep(context, widget, definition, glanceIds, workerName, hasData) {
            error(e.message)
        }
        // Three attempts, then give up: the next periodic run is the real recovery, and a
        // worker that retries for ever holds a JobScheduler slot for a widget nobody is
        // looking at.
        retryUntilExhausted()
    }
}

private fun CoroutineWorker.retryUntilExhausted(): ListenableWorker.Result =
    if (runAttemptCount < 3) ListenableWorker.Result.retry() else ListenableWorker.Result.failure()

/**
 * Replace the widget with its error frame only when there is nothing better already showing.
 *
 * A failed refresh used to overwrite the state unconditionally, so one transient throw — a
 * DataStore read that lost a race, a database busy for a moment, a location flow that had not
 * settled — turned a widget that was showing correct prayer times into "tap to set up", and left
 * it that way until a later run happened to succeed. The data on screen was still perfectly good;
 * the refresh failing says nothing about it. Now the error frame is for widgets that have nothing
 * to show, which is what it was ever meant to communicate.
 */
private suspend fun <S> publishErrorIfNothingToKeep(
    context: Context,
    widget: GlanceAppWidget,
    definition: GlanceStateDefinition<S>,
    glanceIds: List<GlanceId>,
    workerName: String,
    hasData: (S) -> Boolean,
    error: () -> S,
) {
    try {
        // One state file backs every instance of a widget, so any id answers for all of them.
        val current = getAppWidgetState(context, definition, glanceIds.first())
        if (hasData(current)) {
            // Redraw anyway: the stored instants are absolute, so the countdowns and the
            // "next prayer" highlight still advance off data loaded earlier.
            widget.updateAll(context)
            return
        }
        updateWidgetState(context, widget, definition, glanceIds, error())
    } catch (e: Exception) {
        // The failure handler failing is not worth failing the worker over — the retry above
        // still stands.
        CrashReporter.log("$workerName could not publish its error state")
        CrashReporter.recordException(e)
    }
}
