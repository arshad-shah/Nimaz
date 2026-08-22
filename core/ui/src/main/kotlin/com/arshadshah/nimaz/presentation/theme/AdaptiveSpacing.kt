package com.arshadshah.nimaz.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Provides responsive spacing and sizing values based on the current window size class.
 * Use these in screens that need to adapt their spacing and max widths for tablet layouts.
 */
object AdaptiveSpacing {

    /**
     * Returns responsive horizontal screen padding.
     * - Compact (phone): 20dp (default)
     * - Medium/Expanded (tablet): 32dp
     */
    @Composable
    fun screenPadding(): Dp {
        val windowSizeClass = currentWindowSizeClass()
        return if (windowSizeClass.isCompact) 20.dp else 32.dp
    }

    /**
     * Returns maximum content width to prevent overly wide text on large screens.
     * - Compact (phone): unrestricted (Dp.Unspecified)
     * - Medium (tablet portrait ~600-840dp): 700dp
     * - Expanded (tablet landscape / desktop): 800dp
     */
    @Composable
    fun maxContentWidth(): Dp {
        val windowSizeClass = currentWindowSizeClass()
        return when {
            windowSizeClass.isCompact -> Dp.Unspecified
            windowSizeClass.isExpandedWidth -> 800.dp
            else -> 700.dp // Medium
        }
    }

    /**
     * Returns maximum width for readable text content (Quran, Hadith, Dua readers).
     * Prevents text lines from becoming too long and hard to read.
     * - Compact: unrestricted
     * - Tablet: 700dp
     */
    @Composable
    fun maxReadableWidth(): Dp {
        val windowSizeClass = currentWindowSizeClass()
        return if (windowSizeClass.isCompact) Dp.Unspecified else 700.dp
    }

    /**
     * Returns maximum width for form content (Zakat calculator, settings forms).
     * - Compact: unrestricted
     * - Tablet: 600dp
     */
    @Composable
    fun maxFormWidth(): Dp {
        val windowSizeClass = currentWindowSizeClass()
        return if (windowSizeClass.isCompact) Dp.Unspecified else 600.dp
    }

    /**
     * Returns maximum width for search bars.
     * - Compact: unrestricted (fills width)
     * - Tablet: 600dp
     */
    @Composable
    fun maxSearchBarWidth(): Dp {
        val windowSizeClass = currentWindowSizeClass()
        return if (windowSizeClass.isCompact) Dp.Unspecified else 600.dp
    }

    /**
     * Returns the number of columns for stats grids.
     * - Compact: determined by data count (typically 2-3)
     * - Medium: 3-4 columns
     * - Expanded: 4+ columns
     */
    @Composable
    fun statsGridColumns(dataCount: Int): Int {
        val windowSizeClass = currentWindowSizeClass()
        return when {
            windowSizeClass.isCompact -> dataCount.coerceAtMost(3)
            windowSizeClass.isExpandedWidth -> dataCount.coerceAtMost(6)
            else -> dataCount.coerceAtMost(4)
        }
    }

    /**
     * Returns responsive vertical spacing between sections.
     * - Compact: 16dp
     * - Tablet: 20dp
     */
    @Composable
    fun sectionSpacing(): Dp {
        val windowSizeClass = currentWindowSizeClass()
        return if (windowSizeClass.isCompact) 16.dp else 20.dp
    }

    /**
     * Returns responsive card corner radius.
     * - Compact: 14dp
     * - Tablet: 16dp
     */
    @Composable
    fun cardCornerRadius(): Dp {
        val windowSizeClass = currentWindowSizeClass()
        return if (windowSizeClass.isCompact) 14.dp else 16.dp
    }
}
