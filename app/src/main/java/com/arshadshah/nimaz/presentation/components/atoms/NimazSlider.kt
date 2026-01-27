package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Primary slider component.
 */
@Composable
fun NimazSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors()
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors
    )
}

/**
 * Slider with label and value display.
 */
@Composable
fun LabeledSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    valueFormatter: (Float) -> String = { it.roundToInt().toString() },
    showValue: Boolean = true,
    colors: SliderColors = SliderDefaults.colors()
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            if (showValue) {
                Text(
                    text = valueFormatter(value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        NimazSlider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            colors = colors
        )
    }
}

/**
 * Font size slider for Arabic text customization.
 */
@Composable
fun FontSizeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    minSize: Float = 16f,
    maxSize: Float = 40f,
    steps: Int = 5,
    onValueChangeFinished: (() -> Unit)? = null
) {
    LabeledSlider(
        value = value,
        onValueChange = onValueChange,
        label = "Font Size",
        modifier = modifier,
        valueRange = minSize..maxSize,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        valueFormatter = { "${it.roundToInt()}sp" },
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

/**
 * Volume/notification slider.
 */
@Composable
fun VolumeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Volume",
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null
) {
    LabeledSlider(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
        valueRange = 0f..100f,
        onValueChangeFinished = onValueChangeFinished,
        valueFormatter = { "${it.roundToInt()}%" }
    )
}

/**
 * Time offset slider (for prayer time adjustments).
 */
@Composable
fun TimeOffsetSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Time Adjustment",
    minMinutes: Float = -30f,
    maxMinutes: Float = 30f,
    onValueChangeFinished: (() -> Unit)? = null
) {
    LabeledSlider(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        valueRange = minMinutes..maxMinutes,
        steps = ((maxMinutes - minMinutes) / 5).toInt() - 1,
        onValueChangeFinished = onValueChangeFinished,
        valueFormatter = { minutes ->
            val rounded = minutes.roundToInt()
            when {
                rounded > 0 -> "+$rounded min"
                rounded < 0 -> "$rounded min"
                else -> "0 min"
            }
        }
    )
}

/**
 * Range slider for selecting a range of values.
 */
@Composable
fun NimazRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors()
) {
    RangeSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors
    )
}

/**
 * Labeled range slider.
 */
@Composable
fun LabeledRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    valueFormatter: (Float) -> String = { it.roundToInt().toString() }
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${valueFormatter(value.start)} - ${valueFormatter(value.endInclusive)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        NimazRangeSlider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished
        )
    }
}

/**
 * Discrete step slider with tick marks.
 */
@Composable
fun StepSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    steps: List<String>,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        if (label != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = steps.getOrElse(value) { "" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        NimazSlider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            enabled = enabled,
            valueRange = 0f..(steps.size - 1).toFloat(),
            steps = steps.size - 2
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEach { step ->
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Basic Slider")
@Composable
private fun NimazSliderPreview() {
    MaterialTheme {
        var value by remember { mutableFloatStateOf(0.5f) }
        Box(modifier = Modifier.padding(16.dp)) {
            NimazSlider(value = value, onValueChange = { value = it })
        }
    }
}

@Preview(showBackground = true, name = "Labeled Slider")
@Composable
private fun LabeledSliderPreview() {
    MaterialTheme {
        var value by remember { mutableFloatStateOf(50f) }
        Box(modifier = Modifier.padding(16.dp)) {
            LabeledSlider(
                value = value,
                onValueChange = { value = it },
                label = "Brightness",
                valueRange = 0f..100f
            )
        }
    }
}

@Preview(showBackground = true, name = "Font Size Slider")
@Composable
private fun FontSizeSliderPreview() {
    MaterialTheme {
        var fontSize by remember { mutableFloatStateOf(24f) }
        Box(modifier = Modifier.padding(16.dp)) {
            FontSizeSlider(value = fontSize, onValueChange = { fontSize = it })
        }
    }
}

@Preview(showBackground = true, name = "Volume Slider")
@Composable
private fun VolumeSliderPreview() {
    MaterialTheme {
        var volume by remember { mutableFloatStateOf(75f) }
        Box(modifier = Modifier.padding(16.dp)) {
            VolumeSlider(value = volume, onValueChange = { volume = it })
        }
    }
}

@Preview(showBackground = true, name = "Time Offset Slider")
@Composable
private fun TimeOffsetSliderPreview() {
    MaterialTheme {
        var offset by remember { mutableFloatStateOf(0f) }
        Box(modifier = Modifier.padding(16.dp)) {
            TimeOffsetSlider(value = offset, onValueChange = { offset = it })
        }
    }
}

@Preview(showBackground = true, name = "Range Slider")
@Composable
private fun LabeledRangeSliderPreview() {
    MaterialTheme {
        var range by remember { mutableStateOf(20f..80f) }
        Box(modifier = Modifier.padding(16.dp)) {
            LabeledRangeSlider(
                value = range,
                onValueChange = { range = it },
                label = "Price Range"
            )
        }
    }
}

@Preview(showBackground = true, name = "Step Slider")
@Composable
private fun StepSliderPreview() {
    MaterialTheme {
        var step by remember { mutableIntStateOf(1) }
        Box(modifier = Modifier.padding(16.dp)) {
            StepSlider(
                value = step,
                onValueChange = { step = it },
                steps = listOf("Low", "Medium", "High"),
                label = "Quality"
            )
        }
    }
}
