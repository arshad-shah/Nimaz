package com.arshadshah.nimaz.widget.core

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.GlanceStateDefinition

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
