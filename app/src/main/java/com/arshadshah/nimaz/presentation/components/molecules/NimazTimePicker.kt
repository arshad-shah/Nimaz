package com.arshadshah.nimaz.presentation.components.molecules

import android.text.format.DateFormat
import com.arshadshah.nimaz.presentation.foundation.time.NimazTime
import com.arshadshah.nimaz.presentation.foundation.time.rememberTimeFormatter
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import com.arshadshah.nimaz.presentation.theme.rememberNimazHaptics
import kotlinx.coroutines.flow.distinctUntilChanged

private val ItemHeight = 46.dp
private const val VISIBLE_ITEMS = 3

/**
 * The app's own scrolling time picker.
 *
 * Snapping wheels in slotted containers, matching how the rest of the app frames
 * selectable values. Exists because Material3's `TimePicker` brings its own visual
 * language — a clock dial with its own typography and shapes — that matches nothing else here.
 *
 * Defaults to the **device's** 12/24-hour preference; [is24Hour] overrides it. The value
 * is always a 24-hour [NimazTime] regardless of how it is displayed.
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
    is24Hour: Boolean = DateFormat.is24HourFormat(LocalContext.current),
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val hours = remember(is24Hour) { if (is24Hour) (0..23).toList() else (1..12).toList() }
    val minutes = remember(minuteStep) { (0..59 step minuteStep).toList() }

    val amLabel = stringResource(R.string.time_period_am)
    val pmLabel = stringResource(R.string.time_period_pm)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WheelSlot(
            items = hours,
            selected = if (is24Hour) value.hour else value.hour12,
            label = { "%02d".format(it) },
            onSelected = { h ->
                onValueChange(
                    if (is24Hour) value.copy(hour = h) else value.withHour12(h, value.isPm)
                )
            },
            accentColor = accentColor,
            modifier = Modifier.width(76.dp),
        )

        Text(
            text = ":",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.padding(horizontal = NimazSpacing.Small),
        )

        WheelSlot(
            items = minutes,
            selected = value.minute,
            label = { "%02d".format(it) },
            onSelected = { onValueChange(value.copy(minute = it)) },
            accentColor = accentColor,
            modifier = Modifier.width(76.dp),
        )

        if (!is24Hour) {
            Box(Modifier.width(NimazSpacing.Small))
            WheelSlot(
                items = listOf(0, 1),
                selected = if (value.isPm) 1 else 0,
                label = { if (it == 1) pmLabel else amLabel },
                onSelected = { onValueChange(value.withHour12(value.hour12, pm = it == 1)) },
                accentColor = accentColor,
                modifier = Modifier.width(76.dp),
            )
        }
    }
}

/**
 * One wheel inside its own slot.
 *
 * The slot is the container; the centre band inside it marks the selected row. The list
 * is padded by one item top and bottom so the item at `firstVisibleItemIndex` is the one
 * sitting in that band.
 */
@Composable
private fun WheelSlot(
    items: List<Int>,
    selected: Int,
    label: (Int) -> String,
    onSelected: (Int) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val initialIndex = items.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val haptics = rememberNimazHaptics()
    var lastReported by remember { mutableIntStateOf(initialIndex) }

    // Report while scrolling rather than only when settled, so a caller's preview text
    // tracks the wheel instead of jumping at the end of the fling.
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

    // Nested surface (the picker lives inside a dialog/sheet/settings card), so the
    // slot is outlined and flat rather than a hand-rolled tonal fill.
    _root_ide_package_.com.arshadshah.nimaz.presentation.components.atoms.NimazCard(
        modifier = modifier.height(ItemHeight * VISIBLE_ITEMS),
        style = com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle.OUTLINED,
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // The selection band — "selected among peers", so the fill is correct
            // here; it just routes through the design system instead of raw alpha.
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.atoms.NimazCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .height(ItemHeight),
                style = com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle.FILLED,
                shape = RoundedCornerShape(12.dp),
                selected = true,
                colors = com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults.selectable(
                    activeContainer = accentColor.copy(alpha = 0.16f),
                    activeContent = accentColor,
                ),
                elevation = 0.dp,
                content = {},
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                contentPadding = PaddingValues(vertical = ItemHeight),
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
                            text = label(itemValue),
                            style = if (isSelected) MaterialTheme.typography.titleLarge
                            else MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) accentColor
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        )
                    }
                }
            }
        }
    }
}

// ---- Previews ----

@Composable
private fun TimePickerShowcase(is24Hour: Boolean) {
    var time by remember { mutableStateOf(NimazTime(14, 30)) }
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NimazTimePicker(value = time, onValueChange = { time = it }, is24Hour = is24Hour)
    }
}

@Preview(showBackground = true, widthDp = 360, name = "TimePicker 24h — Light")
@Composable
private fun NimazTimePicker24LightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { TimePickerShowcase(is24Hour = true) }
}

@Preview(showBackground = true, widthDp = 360, name = "TimePicker 24h — Dark")
@Composable
private fun NimazTimePicker24DarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { TimePickerShowcase(is24Hour = true) }
}

@Preview(showBackground = true, widthDp = 360, name = "TimePicker 12h — Light")
@Composable
private fun NimazTimePicker12LightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { TimePickerShowcase(is24Hour = false) }
}

@Preview(showBackground = true, widthDp = 360, name = "TimePicker 12h — Dark")
@Composable
private fun NimazTimePicker12DarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { TimePickerShowcase(is24Hour = false) }
}
