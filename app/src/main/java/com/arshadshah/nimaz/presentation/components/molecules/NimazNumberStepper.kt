package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButtonStyle
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Layout shape of a [NimazNumberStepper].
 */
enum class NimazNumberStepperVariant {
    /**
     * A label on the left, with the decrement / value / increment controls grouped
     * compactly on the right. No surrounding container. The settings-row look
     * (prayer-time adjustments, daily targets).
     */
    INLINE,

    /**
     * A full-width tonal container with the decrement and increment buttons pushed
     * to the edges and a large, prominent value centred between them. The "dial in
     * a number" look (tasbih target counters).
     */
    SPREAD
}

/**
 * Size preset for a [NimazNumberStepper]: the decrement/increment button size, the
 * container corner radius, and the inner padding. The value typography scales with
 * size (resolved against [MaterialTheme.typography] at composition).
 */
enum class NimazNumberStepperSize(
    val iconButtonSize: NimazIconButtonSize,
    val corner: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp
) {
    SMALL(NimazIconButtonSize.SMALL, 8.dp, 8.dp, 6.dp),
    MEDIUM(NimazIconButtonSize.MEDIUM, 14.dp, 8.dp, 6.dp),
    LARGE(NimazIconButtonSize.LARGE, 14.dp, 12.dp, 8.dp)
}

/**
 * Colour emphasis of the displayed value.
 */
enum class NimazNumberStepperType {
    /** Neutral value. `onSurface`. */
    DEFAULT,

    /** Brand-accent value, drawing the eye to the number being set.
     *  `NimazColors.TasbihColors.Milestone`. */
    ACCENT
}

/**
 * A reusable number stepper: a decrement button, a value display, and an increment
 * button, with optional min/max clamping and a configurable step.
 *
 * The single component covers two looks via [variant]:
 * - [NimazNumberStepperVariant.INLINE] — a [label] on the left and compact controls on
 *   the right (settings rows).
 * - [NimazNumberStepperVariant.SPREAD] — a full-width tonal container with a large centred
 *   value (target dials). [label] is ignored in this variant.
 *
 * @param value Current integer value
 * @param onValueChange Callback when value changes (already clamped to [minValue]..[maxValue])
 * @param modifier Modifier for the root element
 * @param variant Layout shape — see [NimazNumberStepperVariant]
 * @param size Control + typography size preset
 * @param type Value colour emphasis
 * @param label Text label shown on the left in [NimazNumberStepperVariant.INLINE]
 * @param formatValue Custom formatter for the displayed value. Defaults to showing a "+"
 *   prefix for positive values in INLINE, and the plain number in SPREAD.
 * @param minValue Minimum allowed value (inclusive). Decrement button disabled at this limit.
 * @param maxValue Maximum allowed value (inclusive). Increment button disabled at this limit.
 * @param step Amount to increment/decrement per click
 */
@Composable
fun NimazNumberStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    variant: NimazNumberStepperVariant = NimazNumberStepperVariant.INLINE,
    size: NimazNumberStepperSize = NimazNumberStepperSize.SMALL,
    type: NimazNumberStepperType = NimazNumberStepperType.DEFAULT,
    label: String? = null,
    formatValue: ((Int) -> String)? = null,
    minValue: Int = Int.MIN_VALUE,
    maxValue: Int = Int.MAX_VALUE,
    step: Int = 1
) {
    val valueColor = when (type) {
        NimazNumberStepperType.DEFAULT -> MaterialTheme.colorScheme.onSurface
        NimazNumberStepperType.ACCENT -> NimazColors.TasbihColors.Milestone
    }
    val valueStyle = when (size) {
        NimazNumberStepperSize.SMALL -> MaterialTheme.typography.bodyMedium
        NimazNumberStepperSize.MEDIUM -> MaterialTheme.typography.titleMedium
        NimazNumberStepperSize.LARGE -> MaterialTheme.typography.headlineSmall
    }

    val decrement = { onValueChange((value - step).coerceAtLeast(minValue)) }
    val increment = { onValueChange((value + step).coerceAtMost(maxValue)) }

    when (variant) {
        NimazNumberStepperVariant.INLINE -> InlineStepper(
            value = value,
            label = label,
            valueText = formatValue?.invoke(value) ?: if (value > 0) "+$value" else "$value",
            valueStyle = valueStyle,
            valueColor = valueColor,
            size = size,
            canDecrement = value > minValue,
            canIncrement = value < maxValue,
            onDecrement = decrement,
            onIncrement = increment,
            modifier = modifier
        )

        NimazNumberStepperVariant.SPREAD -> SpreadStepper(
            value = value,
            valueText = formatValue?.invoke(value) ?: "$value",
            valueStyle = valueStyle,
            valueColor = valueColor,
            size = size,
            canDecrement = value > minValue,
            canIncrement = value < maxValue,
            onDecrement = decrement,
            onIncrement = increment,
            modifier = modifier
        )
    }
}

@Composable
private fun InlineStepper(
    value: Int,
    label: String?,
    valueText: String,
    valueStyle: TextStyle,
    valueColor: Color,
    size: NimazNumberStepperSize,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NimazIconButton(
                icon = Icons.Default.Remove,
                contentDescription = stringResource(R.string.cd_decrease),
                style = NimazIconButtonStyle.FILLED,
                size = size.iconButtonSize,
                enabled = canDecrement,
                onClick = onDecrement
            )
            NimazCard(
                style = NimazCardStyle.OUTLINED,
                shape = RoundedCornerShape(8.dp),
                colors = NimazCardDefaults.colors(
                    border = MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Text(
                    text = valueText,
                    style = valueStyle,
                    fontWeight = FontWeight.SemiBold,
                    color = valueColor,
                    modifier = Modifier.padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }
            NimazIconButton(
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_increase),
                style = NimazIconButtonStyle.FILLED,
                size = size.iconButtonSize,
                enabled = canIncrement,
                onClick = onIncrement
            )
        }
    }
}

@Composable
private fun SpreadStepper(
    value: Int,
    valueText: String,
    valueStyle: TextStyle,
    valueColor: Color,
    size: NimazNumberStepperSize,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.FILLED,
        shape = RoundedCornerShape(size.corner),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = size.horizontalPadding, vertical = size.verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NimazIconButton(
                icon = Icons.Default.Remove,
                contentDescription = stringResource(R.string.cd_decrease),
                style = NimazIconButtonStyle.FILLED_TONAL,
                size = size.iconButtonSize,
                enabled = canDecrement,
                onClick = onDecrement
            )
            Text(
                text = valueText,
                style = valueStyle,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                textAlign = TextAlign.Center
            )
            NimazIconButton(
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_increase),
                style = NimazIconButtonStyle.FILLED_TONAL,
                size = size.iconButtonSize,
                enabled = canIncrement,
                onClick = onIncrement
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Inline - Positive")
@Composable
private fun NimazNumberStepperInlinePreview() {
    NimazTheme {
        NimazNumberStepper(
            label = "Fajr",
            value = 3,
            onValueChange = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Inline - Custom Format")
@Composable
private fun NimazNumberStepperInlineCustomPreview() {
    NimazTheme {
        NimazNumberStepper(
            label = "Daily Target",
            value = 20,
            onValueChange = {},
            formatValue = { "$it ayahs" },
            minValue = 1,
            maxValue = 200
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Spread - Default Medium")
@Composable
private fun NimazNumberStepperSpreadPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            NimazNumberStepper(
                value = 33,
                onValueChange = {},
                variant = NimazNumberStepperVariant.SPREAD,
                size = NimazNumberStepperSize.MEDIUM,
                minValue = 1,
                maxValue = 9999
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Spread - Accent Large")
@Composable
private fun NimazNumberStepperSpreadAccentPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            NimazNumberStepper(
                value = 100,
                onValueChange = {},
                variant = NimazNumberStepperVariant.SPREAD,
                size = NimazNumberStepperSize.LARGE,
                type = NimazNumberStepperType.ACCENT,
                minValue = 1
            )
        }
    }
}