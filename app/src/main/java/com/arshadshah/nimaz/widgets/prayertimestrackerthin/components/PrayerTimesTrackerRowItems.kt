package com.arshadshah.nimaz.widgets.prayertimestrackerthin.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.repositories.PrayerTrackerRepository
import com.arshadshah.nimaz.widgets.prayertimestrackerthin.PrayerTimesTrackerWorker
import kotlinx.coroutines.launch
import java.time.LocalDate

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
            .padding(4.dp)
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
                fontSize = TextUnit(16F, TextUnitType.Sp),
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.padding(top = 8.dp)
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
                fontWeight = FontWeight.Normal
            ),
            modifier = GlanceModifier.padding(vertical = 4.dp)
        )

        // Checkbox with custom styling

        CheckBox(
            checked = checked,
            action {
                onCheckedChange(!checked)
            },
            modifier = GlanceModifier.padding(bottom = 8.dp)
        )
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
    context: Context,
) {
    val scope = rememberCoroutineScope()
    val prayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.background)
            .padding(4.dp)
            .cornerRadius(16.dp)
            .clickable(onClick = actionStartActivity<MainActivity>()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        prayers.forEach { prayer ->
            WidgetTogglableItem(
                text = prayer,
                timeText = when (prayer) {
                    "Fajr" -> fajrTime.value
                    "Dhuhr" -> dhuhrTime.value
                    "Asr" -> asrTime.value
                    "Maghrib" -> maghribTime.value
                    "Isha" -> ishaTime.value
                    else -> ""
                },
                checked = when (prayer) {
                    "Fajr" -> fajr.value
                    "Dhuhr" -> dhuhr.value
                    "Asr" -> asr.value
                    "Maghrib" -> maghrib.value
                    "Isha" -> isha.value
                    else -> false
                },
                onCheckedChange = { checked: Boolean ->
                    when (prayer) {
                        "Fajr" -> fajr.value = checked
                        "Dhuhr" -> dhuhr.value = checked
                        "Asr" -> asr.value = checked
                        "Maghrib" -> maghrib.value = checked
                        "Isha" -> isha.value = checked
                    }
                    scope.launch {
                        PrayerTrackerRepository.updateSpecificPrayer(
                            LocalDate.now(),
                            prayer,
                            checked
                        )
                        PrayerTimesTrackerWorker.enqueue(context, true)
                    }
                },
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }
}