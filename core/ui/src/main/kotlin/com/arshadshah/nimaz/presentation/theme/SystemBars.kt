package com.arshadshah.nimaz.presentation.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light status-bar icons for as long as the calling screen is composed, whatever the theme — for
 * a screen that paints something dark under the status bar in light mode too (the onboarding's
 * illustrations, Learn to Pray's posture stage). Leaving the screen restores what was there.
 *
 * It has to be a `SideEffect`, not a one-off effect. [NimazTheme] sets the icon contrast from the
 * theme in a `SideEffect` on every recomposition, and a remembered effect runs *before* side
 * effects in the same pass — so a one-off override loses as soon as the theme recomposes
 * (switching to light mode with the screen open, for one). A caller's `SideEffect` is registered
 * after the theme's and so applies last.
 */
@Composable
fun LightStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (view.context as? Activity)?.window ?: return
    val controller = remember(window, view) { WindowCompat.getInsetsController(window, view) }
    DisposableEffect(controller) {
        val previous = controller.isAppearanceLightStatusBars
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
    SideEffect { controller.isAppearanceLightStatusBars = false }
}
