package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import kotlinx.coroutines.flow.distinctUntilChanged

private val ItemHeight = 44.dp
private const val VISIBLE_ITEMS = 3

/**
 * A time of day as hour-of-day (0-23) and minute.
 *
 * Kept as a value type so callers do not pass two loose ints in the wrong order.
 */
data class NimazTime(val hour: Int, val minute: Int) {

    /** Zero-padded 24-hour "HH:mm", the form persisted in preferences. */
    fun toStorageString(): String = "%02d:%02d".format(hour, minute)

    companion object {
        /** Parses "HH:mm", falling back to [fallback] for malformed or out-of-range input. */
        fun parse(value: String?, fallback: NimazTime = NimazTime(6, 0)): NimazTime {
            val parts = value?.split(":") ?: return fallback
            val h = parts.getOrNull(0)?.toIntOrNull() ?: return fallback
            val m = parts.getOrNull(1)?.toIntOrNull() ?: return fallback
            if (h !in 0..23 || m !in 0..59) return fallback
            return NimazTime(h, m)
        }
    }
}

/**
 * The app's own scrolling time picker.
 *
 * Two snapping wheels with a highlighted centre band, styled from the app's colour
 * scheme. Exists because Material3's `TimePicker` brings its own visual language
 * (clock dial, its own typography and shapes) that does not match anything else here.
 *
 * @param minuteStep granularity of the minute wheel; 5 keeps the wheel short for
 *   reminder-style times where per-minute precision is not meaningful.
 */
@Composable
fun NimazTimePicker(
    value: NimazTime,
    onValueChange: (NimazTime) -> Unit,
    modifier: Modifier = Modifier,
    minuteStep: Int = 5,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val hours = remember { (0..23).toList() }
    val minutes = remember(minuteStep) { (0..59 step minuteStep).toList() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ItemHeight * VISIBLE_ITEMS),
        contentAlignment = Alignment.Center,
    ) {
        // Selection band sits behind both wheels so they read as one control.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ItemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.12f))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WheelColumn(
                items = hours,
                selected = value.hour,
                onSelected = { onValueChange(value.copy(hour = it)) },
                accentColor = accentColor,
                modifier = Modifier.width(72.dp),
            )
            Text(
                text = ":",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.padding(horizontal = NimazSpacing.Small),
            )
            WheelColumn(
                items = minutes,
                selected = value.minute,
                onSelected = { onValueChange(value.copy(minute = it)) },
                accentColor = accentColor,
                modifier = Modifier.width(72.dp),
            )
        }
    }
}

/**
 * One snapping wheel.
 *
 * The list is padded by one item top and bottom so the item at
 * `firstVisibleItemIndex` is the one sitting in the centre band.
 */
@Composable
private fun WheelColumn(
    items: List<Int>,
    selected: Int,
    onSelected: (Int) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val initialIndex = items.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val haptics = LocalHapticFeedback.current
    var lastReported by remember { mutableIntStateOf(initialIndex) }

    // Report while scrolling, not only when settled, so the caller's preview text
    // tracks the wheel instead of jumping at the end.
    LaunchedEffect(listState, items) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (index != lastReported && index in items.indices) {
                    lastReported = index
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelected(items[index])
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.height(ItemHeight * VISIBLE_ITEMS),
        flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = ItemHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(items.size) { index ->
            val itemValue = items[index]
            val isSelected = itemValue == selected
            Box(
                modifier = Modifier
                    .height(ItemHeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "%02d".format(itemValue),
                    style = if (isSelected) MaterialTheme.typography.headlineSmall
                    else MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) accentColor
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

/**
 * A read-only field showing a time, for opening [NimazTimePicker] from a settings row.
 */
@Composable
fun NimazTimeDisplay(
    time: NimazTime,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .padding(horizontal = NimazSpacing.Medium, vertical = NimazSpacing.Small)
            .then(
                if (contentDescription != null) {
                    Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = time.toStorageString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = accentColor,
        )
    }
}

// ---- Previews ----

@Composable
private fun TimePickerShowcase(dp: Dp = 0.dp) {
    var time by remember { androidx.compose.runtime.mutableStateOf(NimazTime(6, 30)) }
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NimazTimeDisplay(time = time)
        NimazTimePicker(value = time, onValueChange = { time = it })
    }
}

@Preview(showBackground = true, widthDp = 340, name = "NimazTimePicker — Light")
@Composable
private fun NimazTimePickerLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { TimePickerShowcase() }
}

@Preview(showBackground = true, widthDp = 340, name = "NimazTimePicker — Dark")
@Composable
private fun NimazTimePickerDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { TimePickerShowcase() }
}

@Preview(showBackground = true, widthDp = 340, name = "NimazTimePicker — Per-minute")
@Composable
private fun NimazTimePickerFineGrainedPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        Column(Modifier.padding(16.dp)) {
            NimazTimePicker(
                value = NimazTime(21, 47),
                onValueChange = {},
                minuteStep = 1,
            )
        }
    }
}
