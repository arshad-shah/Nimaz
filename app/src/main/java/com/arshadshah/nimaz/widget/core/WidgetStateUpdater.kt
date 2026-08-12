package com.arshadshah.nimaz.widget.core

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
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
 * publish an error state and decide whether to retry.
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
 * @param error builds the state to publish when loading threw.
 */
suspend fun <S> CoroutineWorker.refreshWidget(
    context: Context,
    widget: GlanceAppWidget,
    definition: GlanceStateDefinition<S>,
    widgetClass: Class<out GlanceAppWidget>,
    workerName: String,
    success: suspend () -> S,
    error: (String?) -> S,
): ListenableWorker.Result {
    val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(widgetClass)
    if (glanceIds.isEmpty()) return ListenableWorker.Result.success()

    return try {
        updateWidgetState(context, widget, definition, glanceIds, success())
        ListenableWorker.Result.success()
    } catch (e: Exception) {
        CrashReporter.log("$workerName failed")
        CrashReporter.recordException(e)
        updateWidgetState(context, widget, definition, glanceIds, error(e.message))
        // Three attempts, then give up: the next periodic run is the real recovery, and a
        // worker that retries for ever holds a JobScheduler slot for a widget nobody is
        // looking at.
        if (runAttemptCount < 3) {
            ListenableWorker.Result.retry()
        } else {
            ListenableWorker.Result.failure()
        }
    }
}
