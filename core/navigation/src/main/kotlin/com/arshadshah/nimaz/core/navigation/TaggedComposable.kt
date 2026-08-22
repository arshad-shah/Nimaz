package com.arshadshah.nimaz.core.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Like [composable], but wraps the destination content in a full-size [Box] carrying a stable
 * [testTag] (see [ScreenTags]). This gives the instrumented UI tests a locale- and
 * copy-independent way to assert which screen is currently shown. The wrapper is otherwise
 * transparent: it forwards the [AnimatedContentScope] receiver and the [NavBackStackEntry] so
 * existing destination bodies (including `toRoute()` argument extraction and any shared-element
 * usage) behave exactly as before.
 *
 * ## Why it is here, and no longer `private`
 *
 * It was declared `private` inside `NavGraph.kt`, which #562 lists as staying in `:app` until the
 * NavGraph decomposition in PR 12 — so the helper was simultaneously required by this module and
 * invisible outside a file that was not moving. Extracting it is the fix, and it is the outcome
 * the issue wanted anyway: the stated reason `:core:navigation` exists is that *every feature
 * module needs it to declare its destinations*, and a `private` helper in `:app` cannot serve
 * that.
 *
 * `CLAUDE.md` requires every destination to be wired with `taggedComposable<Route.X>(ScreenTags.X)`
 * rather than a bare `composable`, and `check_docs.py`'s NAV-04 enforces it. Both now point here.
 */
inline fun <reified T : Any> NavGraphBuilder.taggedComposable(
    tag: String,
    crossinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable<T> { entry ->
        val scope = this
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(tag)
        ) {
            scope.content(entry)
        }
    }
}
