package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The app's directional glyphs, named by what they *mean* rather than by which Material icon
 * happens to draw them.
 *
 * Before this existed, "this row opens something" was drawn four different ways —
 * `ArrowForward` on the Qur'an home rows and the browse jump card, `KeyboardArrowRight` on every
 * settings row, `ChevronRight` on the prayer card, `ArrowForwardIos` in Help — so two rows doing
 * the same thing on two screens did not look like the same thing. There is one chevron now, and
 * a screen that wants a different one has to change it here, for everybody.
 *
 * All of these are auto-mirrored where direction is about *reading order* (forward, back,
 * previous, next), so an RTL locale flips them without a call site knowing. [Expand] and
 * [Collapse] are not: down is down in every locale.
 */
object NimazIcons {

    /**
     * Disclosure: "this opens something." The trailing chevron on a [NimazMenuItem]-style row,
     * on a banner that navigates, on a card whose whole body is a link, and — rotated 90° — the
     * expander on a collapsible row.
     *
     * @see com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
     */
    val Forward: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight

    /** Up one level. The top-app-bar back button. */
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack

    /**
     * Step back through a sequence — a page, a month, a lesson. Paired with [Next]; prefer
     * [NimazNavArrowButton] over drawing this directly.
     */
    val Previous: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft

    /** Step forward through a sequence. The same glyph as [Forward] — see [Previous]. */
    val Next: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight

    /** Reveal a collapsed section. */
    val Expand: ImageVector = Icons.Default.KeyboardArrowDown

    /** Fold a revealed section away. */
    val Collapse: ImageVector = Icons.Default.KeyboardArrowUp
}
