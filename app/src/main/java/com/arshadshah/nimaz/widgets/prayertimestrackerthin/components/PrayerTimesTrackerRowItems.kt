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
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
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
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .background(
                if (checked)
                    GlanceTheme.colors.primaryContainer
                else
                    GlanceTheme.colors.secondaryContainer
            )
            .cornerRadius(16.dp)
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
                    GlanceTheme.colors.onSecondaryContainer,
                fontSize = TextUnit(12F, TextUnitType.Sp),
                fontWeight = FontWeight.Medium
            ),
            modifier = GlanceModifier.padding(top = 4.dp)
        )

        // Time text
        Text(
            text = timeText,
            style = TextStyle(
                color = if (checked)
                    GlanceTheme.colors.onPrimaryContainer
                else
                    GlanceTheme.colors.onSecondaryContainer,
                fontSize = TextUnit(14F, TextUnitType.Sp),
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.padding(vertical = 2.dp)
        )

        // Custom styled checkbox
        CheckBox(
            checked = checked,
            action {
                onCheckedChange(!checked)
            },
            modifier = GlanceModifier.padding(top = 4.dp)
        )

        // Optional: Checked indicator dot
        if (checked) {
            Box(
                modifier = GlanceModifier
                    .padding(top = 4.dp)
                    .size(4.dp)
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

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(8.dp)
            .cornerRadius(24.dp)
            .clickable(onClick = actionStartActivity<MainActivity>()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        prayers.forEach { prayer ->
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
        }
    }
}