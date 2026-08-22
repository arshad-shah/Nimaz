package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon

/**
 * The one field shell the whole app draws its inputs on.
 *
 * It is not a new idea — [NimazDropdownField] already drew it (label above, an outlined
 * [NimazCard] at 14dp with a 1.5dp border and 14/12 padding) and [NimazAmountInput] drew a
 * smaller second version of the same thing. What the app had no shell for was **text**, which
 * is why a dozen screens fell back to Material's `OutlinedTextField` and a form ended up
 * showing two different ideas of what a field is: a label above an outlined card next to a
 * notched border with a floating label.
 *
 * Extracting it here makes the dropdown, the text field and the amount field one family with
 * one geometry, and gives a new member somewhere to be added without inventing a fourth.
 *
 * Everything state-driven lives in this file, so no call site decides what a focused, errored,
 * over-limit, read-only or disabled field looks like:
 *
 * - **Border.** Neutral at rest *and when filled*; primary only while focused; error while
 *   there is a message or the counter is over. The dropdown used to turn its border primary
 *   whenever a value was set — in a form of eight completed fields that is eight primary boxes
 *   and the colour stops meaning "you are here". A value going from muted placeholder to full
 *   ink already says the field is filled.
 * - **Helper and error share one line**, so an error appearing moves nothing.
 * - **Read-only** fills rather than outlines: it is a value, not an empty input.
 * - **Disabled** is the whole shell at 55%, label included.
 */
enum class NimazFieldDensity {
    /** The form field: 14dp radius, 14/12 padding, ~50dp tall. */
    STANDARD,

    /**
     * The in-row field: 12dp radius, 12/10 padding, ~42dp tall — for a field that sits
     * *beside* its label rather than under one, which is the arrangement the Zakat asset rows
     * use and the reason [NimazAmountInput] looked different in the first place.
     */
    COMPACT,
}

/** Geometry and colour rules shared by every member of the field family. */
object NimazFieldDefaults {

    /** Border weight, at rest and in every state — the outline changes colour, never width. */
    val BorderWidth: Dp = 1.5.dp

    /** Gap between the label and the box. */
    val LabelGap: Dp = 8.dp

    /** Gap between the box and the helper/error line. */
    val HelperGap: Dp = 6.dp

    /** Vertical rhythm between two stacked fields. */
    val FieldGap: Dp = 20.dp

    /** How far a disabled field is faded. Matches what [NimazDropdownField] already did. */
    const val DisabledAlpha: Float = 0.55f

    fun shape(density: NimazFieldDensity): Shape = when (density) {
        NimazFieldDensity.STANDARD -> RoundedCornerShape(14.dp)
        NimazFieldDensity.COMPACT -> RoundedCornerShape(12.dp)
    }

    fun horizontalPadding(density: NimazFieldDensity): Dp = when (density) {
        NimazFieldDensity.STANDARD -> 14.dp
        NimazFieldDensity.COMPACT -> 12.dp
    }

    fun verticalPadding(density: NimazFieldDensity): Dp = when (density) {
        NimazFieldDensity.STANDARD -> 12.dp
        NimazFieldDensity.COMPACT -> 10.dp
    }

    /**
     * The floor that keeps a field the same height whether or not it carries a leading icon or
     * a trailing clear button — without it a bare text field is visibly shorter than the
     * dropdown beside it.
     */
    fun minHeight(density: NimazFieldDensity): Dp = when (density) {
        NimazFieldDensity.STANDARD -> 50.dp
        NimazFieldDensity.COMPACT -> 42.dp
    }

    /** The one place the outline colour is decided. */
    @Composable
    fun borderColor(
        enabled: Boolean,
        readOnly: Boolean,
        focused: Boolean,
        isError: Boolean,
    ): Color = when {
        !enabled -> MaterialTheme.colorScheme.outlineVariant
        readOnly -> Color.Transparent
        isError -> MaterialTheme.colorScheme.error
        focused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    /** The one place the field's fill is decided. */
    @Composable
    fun containerColor(readOnly: Boolean): Color =
        if (readOnly) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.surface
}

/**
 * The caption above a field.
 *
 * [NimazFieldShell] renders this for you, so a field never calls it. It is public for the case
 * the shell cannot cover: a control that answers a form question without being a text input —
 * the Khatam form's deadline (a button that opens a date picker) and its daily reminder (a
 * switch inside a card). Those rows need to wear the same label as the fields around them, and
 * reimplementing it per screen is exactly how `KhatamFormScreen` ended up with a private
 * `FieldLabel` at `labelMedium`/SemiBold/onSurfaceVariant sitting above fields whose real
 * labels were `bodyLarge`/Medium/onSurface.
 *
 * @param required appends the red asterisk. Decorative in the accessibility tree: the
 *   requirement belongs in the control's own semantics, and a screen reader announcing
 *   "asterisk" is noise.
 * @param optionalLabel a quiet "optional" marker, for the opposite case.
 */
@Composable
fun NimazFieldLabel(
    text: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    optionalLabel: String? = null,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (required) {
            Text(
                text = "*",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(start = 3.dp)
                    .clearAndSetSemantics { },
            )
        }
        if (!optionalLabel.isNullOrBlank()) {
            Text(
                text = optionalLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

/**
 * Label · box · helper — the frame every Nimaz field is drawn in.
 *
 * Internal on purpose: it is the family's private chassis, not a component to build a one-off
 * field out of. A new kind of field is a new [NimazFieldVariant] or a new caller in this
 * package, so the geometry can never be half-adopted.
 *
 * @param anchored content laid out in the same [Box] as the field box — the dropdown's popup
 *   menu, which has to anchor to the box and not to the whole shell.
 * @param boxModifier applied to the box itself (the dropdown measures its width here to size
 *   the popup).
 * @param onBoxTap makes the box's padding part of the tap target. A caret only occupies the
 *   middle ~26dp of a 50dp box, so without this a tap on the field's own padding does nothing
 *   and the field reads as broken. Rippleless and on an *inner* element, not a `.clickable`
 *   wrapped around the card — see `ARCHITECTURE.md` §8.
 * @param counter the trailing character count, already formatted; [counterIsOver] marks it and
 *   the border without truncating anything the person typed.
 */
@Composable
internal fun NimazFieldShell(
    modifier: Modifier = Modifier,
    label: String? = null,
    required: Boolean = false,
    optionalLabel: String? = null,
    helper: String? = null,
    error: String? = null,
    counter: String? = null,
    counterIsOver: Boolean = false,
    focused: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    density: NimazFieldDensity = NimazFieldDensity.STANDARD,
    onClick: (() -> Unit)? = null,
    onBoxTap: (() -> Unit)? = null,
    boxModifier: Modifier = Modifier,
    boxVerticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    anchored: @Composable () -> Unit = {},
    boxContent: @Composable RowScope.() -> Unit,
) {
    val isError = error != null || counterIsOver
    val borderColor by animateColorAsState(
        targetValue = NimazFieldDefaults.borderColor(
            enabled = enabled,
            readOnly = readOnly,
            focused = focused,
            isError = isError,
        ),
        label = "nimazFieldBorder",
    )

    Column(modifier = modifier.alpha(if (enabled) 1f else NimazFieldDefaults.DisabledAlpha)) {
        if (!label.isNullOrBlank()) {
            NimazFieldLabel(
                text = label,
                required = required,
                optionalLabel = optionalLabel,
            )
            Spacer(modifier = Modifier.height(NimazFieldDefaults.LabelGap))
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            NimazCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(boxModifier),
                style = NimazCardStyle.OUTLINED,
                onClick = onClick,
                enabled = enabled,
                shape = NimazFieldDefaults.shape(density),
                // Kept on `colors` rather than a tone: the border is state-driven
                // (focus / error / read-only) and tones do not carry borders.
                colors = NimazCardDefaults.colors(
                    container = NimazFieldDefaults.containerColor(readOnly),
                    border = borderColor,
                    borderWidth = NimazFieldDefaults.BorderWidth,
                ),
                elevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = NimazFieldDefaults.minHeight(density))
                        .then(
                            if (onBoxTap != null && enabled) {
                                Modifier.clickable(
                                    interactionSource = null,
                                    indication = null,
                                    onClick = onBoxTap,
                                )
                            } else Modifier
                        )
                        .padding(
                            horizontal = NimazFieldDefaults.horizontalPadding(density),
                            vertical = NimazFieldDefaults.verticalPadding(density),
                        ),
                    verticalAlignment = boxVerticalAlignment,
                    content = boxContent,
                )
            }
            anchored()
        }

        // One line for helper, error and counter. An error *replaces* the helper rather than
        // stacking under it, so nothing below the field moves when validation fires.
        if (error != null || !helper.isNullOrBlank() || counter != null) {
            Spacer(modifier = Modifier.height(NimazFieldDefaults.HelperGap))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (error != null) {
                    NimazIcon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        iconSize = 14.dp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    text = error ?: helper.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (counter != null) {
                    Text(
                        text = counter,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (counterIsOver) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
