package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * The sanctioned screen root — a [Scaffold] whose container is **transparent**.
 *
 * A bare `Scaffold` defaults its `containerColor` to
 * `MaterialTheme.colorScheme.background` and paints it opaquely, which covers the
 * app-wide ornament drawn by [NimazPatternBackground] in `MainActivity`. Any
 * screen using a bare `Scaffold` therefore shows no pattern at all.
 *
 * Screens should use this instead. The background colour is painted once, at the
 * root, underneath the ornament — not re-painted per screen.
 *
 * Escape hatch: pass [containerColor] explicitly for a screen that genuinely owns
 * its own backdrop (the Quran reader page, an immersive media surface). Those
 * screens are opting out of the ornament deliberately, which is fine — the point
 * is that it becomes a visible decision rather than an accident.
 */
@Composable
fun NimazScreenScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = Color.Transparent,
    contentColor: Color = if (containerColor == Color.Transparent) {
        MaterialTheme.colorScheme.onBackground
    } else {
        contentColorFor(containerColor)
    },
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}
