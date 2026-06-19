package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * Visual states a lesson node (medallion) can be in on the Qaida course map.
 * Mirrors [com.arshadshah.nimaz.domain.model.LessonStatus] but collapses the
 * unlocked/in-progress statuses into a single "current/available" look.
 */
enum class QaidaMedallionState { DONE, CURRENT, LOCKED }

/**
 * Theme-aware colour set for the Qaida Reader, derived entirely from the app's
 * Material3 [androidx.compose.material3.ColorScheme] so the feature reads
 * correctly in both light and dark themes (no hard-coded "parchment").
 *
 * The Qaida "illuminated" warmth is expressed as gold (the app's `secondary`)
 * accents on top of standard surfaces, while teal (`primary`) marks the active /
 * current step. Canvas drawing (the course trail) can't read [MaterialTheme]
 * inside a `DrawScope`, so colours are resolved here and passed in.
 */
@Immutable
data class QaidaPalette(
    val done: Color,           // completed accent (gold)
    val onDone: Color,
    val current: Color,        // active accent (teal)
    val onCurrent: Color,
    val locked: Color,         // muted fill
    val onLocked: Color,
    val trail: Color,          // walked path stroke
    val trailLocked: Color,    // locked path stroke
    val gold: Color,           // stars / illuminated accents
    val surface: Color,
    val surfaceContainer: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
)

/** Build a [QaidaPalette] from the current Material theme. */
@Composable
fun rememberQaidaPalette(): QaidaPalette {
    val c = MaterialTheme.colorScheme
    return remember(c) {
        QaidaPalette(
            done = c.secondary,
            onDone = c.onSecondary,
            current = c.primary,
            onCurrent = c.onPrimary,
            locked = c.surfaceVariant,
            onLocked = c.onSurfaceVariant,
            trail = c.primary,
            trailLocked = c.outline,
            gold = c.secondary,
            surface = c.surface,
            surfaceContainer = c.surfaceContainerHigh,
            onSurface = c.onSurface,
            onSurfaceVariant = c.onSurfaceVariant,
            outline = c.outline,
        )
    }
}
