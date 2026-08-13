package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The one place [NimazTone] turns into colour for the atom layer.
 *
 * [NimazBadgeDefaults] grew its own private copies of these `when` blocks, and the atoms added
 * for the fasting redesign would have been five more. Tone is a vocabulary; a vocabulary with six
 * private dialects is not one — the failure mode is a `WARNING` that is amber in a badge and
 * orange in a dot, on the same screen, with nothing in either file admitting the other exists.
 *
 * `internal` on purpose: this is how atoms paint themselves, not a public colour API. Screens
 * pass a [NimazTone] to a component and let the component resolve it.
 */
internal object NimazToneColors {

    /**
     * Text, icon or fill colour carrying the tone's meaning — the part the eye reads as
     * "this is good" or "this needs attention".
     */
    @Composable
    fun foreground(tone: NimazTone): Color = when (tone) {
        NimazTone.NEUTRAL, NimazTone.MUTED -> MaterialTheme.colorScheme.onSurfaceVariant
        NimazTone.ACCENT, NimazTone.PROMINENT -> MaterialTheme.colorScheme.primary
        NimazTone.SUCCESS -> MaterialTheme.colorScheme.tertiary
        NimazTone.WARNING -> MaterialTheme.colorScheme.secondary
        NimazTone.ERROR -> MaterialTheme.colorScheme.error
        // Inherits whatever the enclosing content colour is, which is the whole point of a
        // transparent tone: it takes the surface's word for it.
        NimazTone.TRANSPARENT -> LocalContentColor.current
    }

    /**
     * The low-emphasis bed a tone's [foreground] sits on — an unfilled progress track, a chip
     * background, the dim half of a band.
     */
    @Composable
    fun container(tone: NimazTone): Color = when (tone) {
        NimazTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHighest
        NimazTone.MUTED -> MaterialTheme.colorScheme.surfaceContainer
        NimazTone.ACCENT, NimazTone.PROMINENT -> MaterialTheme.colorScheme.primaryContainer
        NimazTone.SUCCESS -> MaterialTheme.colorScheme.tertiaryContainer
        NimazTone.WARNING -> MaterialTheme.colorScheme.secondaryContainer
        NimazTone.ERROR -> MaterialTheme.colorScheme.errorContainer
        NimazTone.TRANSPARENT -> Color.Transparent
    }

    /**
     * Hairline colour for outlined treatments.
     *
     * The two grey tones borrow `outlineVariant` rather than their own foreground: a hairline
     * drawn in body-text grey reads as a border that is trying to be a divider.
     */
    @Composable
    fun outline(tone: NimazTone): Color = when (tone) {
        NimazTone.NEUTRAL, NimazTone.MUTED -> MaterialTheme.colorScheme.outlineVariant
        NimazTone.TRANSPARENT -> Color.Transparent
        else -> foreground(tone)
    }
}
