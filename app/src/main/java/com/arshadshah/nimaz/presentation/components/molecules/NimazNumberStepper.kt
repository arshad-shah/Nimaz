package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButtonStyle
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * A reusable number stepper with label, decrement/increment buttons and a value display.
 *
 * @param label Text label shown on the left side
 * @param value Current integer value
 * @param onValueChange Callback when value changes
 * @param modifier Modifier for the root Row
 * @param formatValue Custom formatter for the displayed value. Defaults to showing "+" prefix for positive values.
 * @param minValue Minimum allowed value (inclusive). Decrement button disabled at this limit.
 * @param maxValue Maximum allowed value (inclusive). Increment button disabled at this limit.
 * @param step Amount to increment/decrement per click
 */
@Composable
fun NimazNumberStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    formatValue: ((Int) -> String)? = null,
    minValue: Int = Int.MIN_VALUE,
    maxValue: Int = Int.MAX_VALUE,
    step: Int = 1
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NimazIconButton(
                icon = Icons.Default.Remove,
                contentDescription = "Decrease",
                style = NimazIconButtonStyle.FILLED,
                size = NimazIconButtonSize.SMALL,
                enabled = value > minValue,
                onClick = {
                    onValueChange((value - step).coerceAtLeast(minValue))
                }
            )
            NimazCard(
                style = NimazCardStyle.OUTLINED,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = formatValue?.invoke(value)
                        ?: if (value > 0) "+$value" else "$value",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }
            NimazIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Increase",
                style = NimazIconButtonStyle.FILLED,
                size = NimazIconButtonSize.SMALL,
                enabled = value < maxValue,
                onClick = {
                    onValueChange((value + step).coerceAtMost(maxValue))
                }
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Number Stepper - Positive")
@Composable
private fun NimazNumberStepperPositivePreview() {
    NimazTheme {
        NimazNumberStepper(
            label = "Fajr",
            value = 3,
            onValueChange = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Number Stepper - Custom Format")
@Composable
private fun NimazNumberStepperCustomPreview() {
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
