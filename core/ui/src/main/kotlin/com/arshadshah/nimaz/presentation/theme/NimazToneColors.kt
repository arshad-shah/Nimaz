package com.arshadshah.nimaz.presentation.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazToneColors.foreground

/**
 * The one place [com.arshadshah.nimaz.presentation.components.atoms.NimazTone] turns into colour for the atom layer.
 *
 * [com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults] grew its own private copies of these `when` blocks, and the atoms added
 * for the fasting redesign would have been five more. Tone is a vocabulary; a vocabulary with six
 * private dialects is not one — the failure mode is a `WARNING` that is amber in a badge and
 * orange in a dot, on the same screen, with nothing in either file admitting the other exists.
 *
 * `internal` on purpose: this is how atoms paint themselves, not a public colour API. Screens
 * pass a [com.arshadshah.nimaz.presentation.components.atoms.NimazTone] to a component and let the component resolve it.
 */
// Public, not `internal`: the tone ramp is consumed by feature molecules that stayed in `:app`
// (`AyahActionSheet` among them) and will move on to their own feature modules. `internal` here
// meant "not part of the app's public API", which said nothing while the app was one module; once
// the boundary exists, a symbol read from the other side of it *is* public API. Widened because
// the consumer legitimately lives elsewhere — never to paper over a file that landed in the wrong
// module. See #551 PR 10.
object NimazToneColors {

    /** How far a tone's own colour is knocked back to become the bed it sits on. */
    private const val ContainerAlpha = 0.16f


    /**
     * Text, icon or fill colour carrying the tone's meaning — the part the eye reads as
     * "this is good" or "this needs attention".
     */
    @Composable
    fun foreground(tone: NimazTone): Color = when (tone) {
        NimazTone.NEUTRAL, NimazTone.MUTED -> MaterialTheme.colorScheme.onSurfaceVariant
        NimazTone.ACCENT, NimazTone.PROMINENT -> MaterialTheme.colorScheme.primary
        // `NimazColors.Success` (green), **not** `colorScheme.tertiary`.
        //
        // The scheme's tertiary is `DeepPurple`, and a "Fasted" control painted purple next to a
        // calendar legend painting the same word green is how this was caught — on an emulator,
        // after the tests passed. The app is already split on this: `NimazSwitch`'s SUCCESS
        // variant uses the green token, while `NimazBadgeDefaults` uses tertiary. Green is the
        // side that means what the tone says, so the atom layer takes it. See ARCHITECTURE §9.
        NimazTone.SUCCESS -> NimazColors.Success
        // Amber, for the same reason: the scheme's secondary is the brand gold, which reads as
        // decoration rather than as "needs attention".
        NimazTone.WARNING -> NimazColors.Warning
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
        // Tinted from the foregrounds above rather than from the scheme's tertiary/secondary
        // containers, which are purple and gold and would put a green fill on a purple bed.
        //
        // Composited rather than left translucent: a container is something content sits *on*,
        // and contrast helpers that pick a text colour from it read alpha as darkness.
        NimazTone.SUCCESS -> NimazColors.Success
            .copy(alpha = ContainerAlpha)
            .compositeOver(MaterialTheme.colorScheme.surface)

        NimazTone.WARNING -> NimazColors.Warning
            .copy(alpha = ContainerAlpha)
            .compositeOver(MaterialTheme.colorScheme.surface)

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