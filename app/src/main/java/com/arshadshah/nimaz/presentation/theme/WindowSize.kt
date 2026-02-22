package com.arshadshah.nimaz.presentation.theme

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass

/**
 * Convenience extensions for checking window width breakpoints.
 */
val WindowSizeClass.isCompact: Boolean
    get() = windowWidthSizeClass == WindowWidthSizeClass.COMPACT

val WindowSizeClass.isExpandedWidth: Boolean
    get() = windowWidthSizeClass == WindowWidthSizeClass.EXPANDED

/**
 * Returns true for Medium or Expanded width — i.e. tablet-sized windows.
 */
val WindowSizeClass.isTablet: Boolean
    get() = windowWidthSizeClass != WindowWidthSizeClass.COMPACT

/**
 * Helper to get the current WindowSizeClass inside any composable.
 */
@Composable
fun currentWindowSizeClass(): WindowSizeClass =
    currentWindowAdaptiveInfo().windowSizeClass
