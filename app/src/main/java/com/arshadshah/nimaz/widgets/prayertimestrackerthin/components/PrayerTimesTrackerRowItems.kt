package com.arshadshah.nimaz.widgets.prayertimestrackerthin.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.arshadshah.nimaz.activities.MainActivity
import kotlinx.coroutines.Job

@Composable
fun WidgetTogglableItem(
    text: String,
    timeText: String,
    checked: Boolean,
    onCheckedChange: (tracker: Boolean) -> Unit,
    modifier: GlanceModifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 6.dp)
            .background(
                if (checked)
                    GlanceTheme.colors.primaryContainer
                else
                    GlanceTheme.colors.surfaceVariant
            )
            .cornerRadius(12.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Prayer name
        Text(
            text = text,
            style = TextStyle(
                color = if (checked)
                    GlanceTheme.colors.onPrimaryContainer
                else
                    GlanceTheme.colors.onSurfaceVariant,
                fontSize = TextUnit(11F, TextUnitType.Sp),
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium
            )
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Time text
        Text(
            text = timeText.ifEmpty { "--:--" },
            style = TextStyle(
                color = if (checked)
                    GlanceTheme.colors.onPrimaryContainer
                else
                    GlanceTheme.colors.onSurface,
                fontSize = TextUnit(13F, TextUnitType.Sp),
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = GlanceModifier.height(6.dp))

        // Checkbox
        CheckBox(
            checked = checked,
            onCheckedChange = action { onCheckedChange(!checked) }
        )

        // Checked indicator pill
        if (checked) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            Box(
                modifier = GlanceModifier
                    .size(width = 16.dp, height = 3.dp)
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(2.dp),
                contentAlignment = Alignment.Center
            ) {}
        }
    }
}

@Composable
fun PrayerTimesTrackerRowItems(
    fajr: MutableState<Boolean>,
    dhuhr: MutableState<Boolean>,
    asr: MutableState<Boolean>,
    maghrib: MutableState<Boolean>,
    isha: MutableState<Boolean>,
    fajrTime: MutableState<String>,
    dhuhrTime: MutableState<String>,
    asrTime: MutableState<String>,
    maghribTime: MutableState<String>,
    ishaTime: MutableState<String>,
    onUpdateTracker: (String, Boolean) -> Job,
) {
    data class PrayerInfo(
        val name: String,
        val state: MutableState<Boolean>,
        val time: MutableState<String>,
    )

    val prayers = listOf(
        PrayerInfo("Fajr", fajr, fajrTime),
        PrayerInfo("Dhuhr", dhuhr, dhuhrTime),
        PrayerInfo("Asr", asr, asrTime),
        PrayerInfo("Maghrib", maghrib, maghribTime),
        PrayerInfo("Isha", isha, ishaTime)
    )

    // Main container with proper design system alignment
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(24.dp)
            .clickable(onClick = actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            prayers.forEachIndexed { index, prayer ->
                WidgetTogglableItem(
                    text = prayer.name,
                    timeText = prayer.time.value,
                    checked = prayer.state.value,
                    onCheckedChange = { checked ->
                        prayer.state.value = checked
                        onUpdateTracker(prayer.name, checked)
                    },
                    modifier = GlanceModifier.defaultWeight(),
                )

                // Add spacing between items (except after last)
                if (index < prayers.size - 1) {
                    Spacer(modifier = GlanceModifier.width(4.dp))
                }
            }
        }
    }
}