package com.arshadshah.nimaz.presentation.theme

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND

/**
 * Convenience extensions for checking window width breakpoints.
 */
val WindowSizeClass.isCompact: Boolean
    get() = !isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)

val WindowSizeClass.isExpandedWidth: Boolean
    get() = isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)

/**
 * Returns true for Medium or Expanded width — i.e. tablet-sized windows.
 */
val WindowSizeClass.isTablet: Boolean
    get() = isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)

/**
 * Helper to get the current WindowSizeClass inside any composable.
 */
@Composable
fun currentWindowSizeClass(): WindowSizeClass =
    currentWindowAdaptiveInfo().windowSizeClass
