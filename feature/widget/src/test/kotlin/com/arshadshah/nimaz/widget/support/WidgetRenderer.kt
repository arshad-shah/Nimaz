package com.arshadshah.nimaz.widget.support

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.test.core.app.ApplicationProvider

/**
 * Renders a Glance widget the way the launcher does and hands back something assertable.
 *
 * Glance composables do not produce a semantics tree, so the `createComponentComposeRule()` /
 * `onNodeWithText` pattern the rest of the campaign uses does not reach them (see #608). What
 * does reach them is [GlanceAppWidget.compose]: it runs the widget's real `provideGlance` against
 * a state you supply and returns the `RemoteViews` the host would be handed. Inflating those under
 * Robolectric gives a real Android view tree, and the text a user would read is on its `TextView`s.
 *
 * This goes through the whole production path — `provideGlance`, `stateDefinition`, the state
 * `when`, and every private content composable behind it — so a widget that throws, or that draws
 * the wrong branch, fails here rather than silently freezing on a home screen.
 */
object WidgetRenderer {

    val context: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * Compose [widget] with [state] and flatten the resulting view tree.
     *
     * [state] is passed straight to Glance's `currentState`, so no DataStore file is written and
     * tests stay hermetic.
     */
    @OptIn(ExperimentalGlanceApi::class)
    suspend fun render(widget: GlanceAppWidget, state: Any?): RenderedWidget {
        val remoteViews = widget.compose(context = context, state = state)
        return RenderedWidget(remoteViews.apply(context, null))
    }
}

/** A widget's inflated view tree, queried the way a reader looks at it. */
class RenderedWidget(val root: View) {

    /** Every non-blank string the widget puts on screen, in tree order. */
    val texts: List<String> by lazy {
        buildList {
            walk(root) { view -> if (view is TextView) add(view.text.toString()) }
        }.filter { it.isNotBlank() }
    }

    /** Every content description in the tree — how a widget's icons name themselves. */
    val contentDescriptions: List<String> by lazy {
        buildList {
            walk(root) { view -> view.contentDescription?.let { add(it.toString()) } }
        }
    }

    /** How many views the tree holds. Used to compare one layout branch against another. */
    val viewCount: Int by lazy {
        var count = 0
        walk(root) { count++ }
        count
    }

    /**
     * How many drawables the widget paints. The tick-box tiles and the event rows carry no text
     * that distinguishes their two branches, so the icon is what says which one ran.
     */
    val imageCount: Int by lazy {
        var count = 0
        walk(root) { view -> if (view is ImageView && view.drawable != null) count++ }
        count
    }

    fun hasText(text: String): Boolean = texts.contains(text)

    fun containsText(fragment: String): Boolean = texts.any { it.contains(fragment) }

    private fun walk(view: View, visit: (View) -> Unit) {
        visit(view)
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) walk(view.getChildAt(index), visit)
        }
    }
}
