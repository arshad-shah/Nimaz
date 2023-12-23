package com.arshadshah.nimaz.widgets.prayertimestrackerthin.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
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
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import com.arshadshah.nimaz.data.remote.repositories.PrayerTrackerRepository
import kotlinx.coroutines.launch

@Composable
fun WidgetTogglableItem(
    text: String,
    timeText: String,
    checked: Boolean,
    onCheckedChange: (tracker: Boolean) -> Unit,
    modifier: GlanceModifier,
) {
    Column(
        modifier = modifier.fillMaxHeight()
            .padding(top = 8.dp, bottom = 2.dp, start = 4.dp, end = 4.dp)
            .background(GlanceTheme.colors.secondaryContainer)
            .clickable {
                onCheckedChange(!checked)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val modifierInternal = GlanceModifier.defaultWeight().padding(2.dp)
        CheckBox(
            checked = checked, action(
                key = "${text}-CheckBox",
            ) {
                onCheckedChange(!checked)
            }, modifier = modifierInternal
        )
        Text(
            text = text, modifier = modifierInternal, style = TextStyle(
                color = GlanceTheme.colors.onSecondaryContainer,
                fontSize = TextUnit(
                    14F, TextUnitType.Sp
                ),
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = timeText, modifier = modifierInternal, style = TextStyle(
                color = GlanceTheme.colors.onSecondaryContainer, fontSize = TextUnit(
                    15F, TextUnitType.Sp
                ),
                fontWeight = FontWeight.Bold
            )
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
    progress: MutableFloatState,
    fajrTime: MutableState<String>,
    dhuhrTime: MutableState<String>,
    asrTime: MutableState<String>,
    maghribTime: MutableState<String>,
    ishaTime: MutableState<String>,
) {

    val scope = rememberCoroutineScope()

    val prayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

    Row(
        modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
            .background(GlanceTheme.colors.secondaryContainer).clickable(
                onClick = actionStartActivity<MainActivity>()
            )
    ) {
        val modifier = GlanceModifier.defaultWeight()
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
                onCheckedChange = { b: Boolean ->
                    when (prayer) {
                        "Fajr" -> fajr.value = b
                        "Dhuhr" -> dhuhr.value = b
                        "Asr" -> asr.value = b
                        "Maghrib" -> maghrib.value = b
                        "Isha" -> isha.value = b
                    }
                    progress.value = when (prayer) {
                        "Fajr" -> if (b) progress.value + 20 else progress.value - 20
                        "Dhuhr" -> if (b) progress.value + 20 else progress.value - 20
                        "Asr" -> if (b) progress.value + 20 else progress.value - 20
                        "Maghrib" -> if (b) progress.value + 20 else progress.value - 20
                        "Isha" -> if (b) progress.value + 20 else progress.value - 20
                        else -> 0f
                    }
                    scope.launch {
                        PrayerTrackerRepository.updateTracker(
                            tracker = PrayerTracker(
                                fajr = fajr.value,
                                dhuhr = dhuhr.value,
                                asr = asr.value,
                                maghrib = maghrib.value,
                                isha = isha.value,
                                progress = progress.value.toInt()
                            )
                        )
                    }
                },
                modifier = modifier
            )
        }

    }
}