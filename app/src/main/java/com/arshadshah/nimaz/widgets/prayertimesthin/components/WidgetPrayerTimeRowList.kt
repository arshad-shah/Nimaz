


package com.arshadshah.nimaz.widgets.prayertimesthin.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.data.local.models.LocalPrayerTimes
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun WidgetPrayerTimeRowList(data: LocalPrayerTimes) {
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm")
    val currentDateTime = LocalDateTime.now()

    // Adjust Isha time if needed
    val adjustedIshaTime = if ((data.isha?.hour ?: 0) >= 22) {
        data.maghrib?.plusMinutes(60)
    } else {
        data.isha
    }

    data class PrayerInfo(
        val name: String,
        val time: LocalDateTime?,
        val displayTime: String
    )

    val prayerTimes = listOf(
        PrayerInfo(
            "Fajr",
            data.fajr,
            data.fajr?.format(timeFormatter) ?: ""
        ),
        PrayerInfo(
            "Dhuhr",
            data.dhuhr,
            data.dhuhr?.format(timeFormatter) ?: ""
        ),
        PrayerInfo(
            "Asr",
            data.asr,
            data.asr?.format(timeFormatter) ?: ""
        ),
        PrayerInfo(
            "Maghrib",
            data.maghrib,
            data.maghrib?.format(timeFormatter) ?: ""
        ),
        PrayerInfo(
            "Isha",
            adjustedIshaTime,
            adjustedIshaTime?.format(timeFormatter) ?: ""
        )
    )

    fun determineActivePrayer(currentTime: LocalDateTime, prayers: List<PrayerInfo>): String {
        val validPrayers = prayers.filter { it.time != null }
        if (validPrayers.isEmpty()) return ""

        // Handle case before first prayer of the day
        if (currentTime.isBefore(validPrayers.first().time)) {
            // If before Fajr, consider Isha from previous day as active
            return "Isha"
        }

        // Handle case after last prayer of the day
        if (currentTime.isAfter(validPrayers.last().time)) {
            return validPrayers.last().name
        }

        // Find the next prayer time
        for (i in 0 until validPrayers.size - 1) {
            val currentPrayer = validPrayers[i]
            val nextPrayer = validPrayers[i + 1]

            if (currentPrayer.time != null && nextPrayer.time != null) {
                if (currentTime.isAfter(currentPrayer.time) &&
                    currentTime.isBefore(nextPrayer.time)) {
                    return currentPrayer.name
                }
            }
        }

        Log.d("WidgetPrayerTimeRowList", "No active prayer found for current time: $currentTime")
        return ""
    }

    val activePrayer = determineActivePrayer(currentDateTime, prayerTimes)
    Log.d("WidgetPrayerTimeRowList", "Active prayer: $activePrayer")

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .padding(4.dp)
            .cornerRadius(20.dp)
            .clickable(onClick = actionStartActivity<MainActivity>()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        prayerTimes.forEach { prayerInfo ->
            WidgetPrayerTimeColumn(
                name = prayerInfo.name,
                time = prayerInfo.displayTime,
                modifier = GlanceModifier.defaultWeight(),
                isActive = prayerInfo.name == activePrayer
            )
        }
    }
}
@Composable
fun WidgetPrayerTimeColumn(
    name: String,
    time: String,
    modifier: GlanceModifier,
    isActive: Boolean
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 2.dp, vertical = 4.dp)
            .background(
                if (isActive)
                    GlanceTheme.colors.primaryContainer
                else
                    GlanceTheme.colors.secondaryContainer
            )
            .cornerRadius(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = name,
            style = TextStyle(
                color = if (isActive)
                    GlanceTheme.colors.onPrimaryContainer
                else
                    GlanceTheme.colors.onSecondaryContainer,
                fontSize = TextUnit(14F, TextUnitType.Sp),
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.padding(top = 8.dp)
        )

        Text(
            text = time,
            style = TextStyle(
                color = if (isActive)
                    GlanceTheme.colors.onPrimaryContainer
                else
                    GlanceTheme.colors.onSecondaryContainer,
                fontSize = TextUnit(16F, TextUnitType.Sp),
                fontWeight = FontWeight.Medium
            ),
            modifier = GlanceModifier.padding(vertical = 4.dp)
        )

        if (isActive) {
            Box(
                modifier = GlanceModifier
                    .size(4.dp)
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(2.dp)
                    .padding(bottom = 4.dp),
                contentAlignment = Alignment.Center,
                content ={
                    Text(
                        text = "Now",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimary,
                            fontSize = TextUnit(10F, TextUnitType.Sp),
                            fontWeight = FontWeight.Medium
                        ),
                    )
                }
            )
        }
    }
}