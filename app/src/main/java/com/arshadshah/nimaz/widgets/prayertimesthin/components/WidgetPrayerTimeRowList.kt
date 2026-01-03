package com.arshadshah.nimaz.widgets.prayertimesthin.components

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
        val displayTime: String,
    )

    val prayerTimes = listOf(
        PrayerInfo(
            "Fajr",
            data.fajr,
            data.fajr?.format(timeFormatter) ?: "--:--",
        ),
        PrayerInfo(
            "Dhuhr",
            data.dhuhr,
            data.dhuhr?.format(timeFormatter) ?: "--:--",
        ),
        PrayerInfo(
            "Asr",
            data.asr,
            data.asr?.format(timeFormatter) ?: "--:--",
        ),
        PrayerInfo(
            "Maghrib",
            data.maghrib,
            data.maghrib?.format(timeFormatter) ?: "--:--",
        ),
        PrayerInfo(
            "Isha",
            adjustedIshaTime,
            adjustedIshaTime?.format(timeFormatter) ?: "--:--",
        )
    )

    fun determineActivePrayer(currentTime: LocalDateTime, prayers: List<PrayerInfo>): String {
        val validPrayers = prayers.filter { it.time != null }
        if (validPrayers.isEmpty()) return ""

        if (currentTime.isBefore(validPrayers.first().time)) {
            return "Isha"
        }

        if (currentTime.isAfter(validPrayers.last().time)) {
            return validPrayers.last().name
        }

        for (i in 0 until validPrayers.size - 1) {
            val currentPrayer = validPrayers[i]
            val nextPrayer = validPrayers[i + 1]

            if (currentPrayer.time != null && nextPrayer.time != null) {
                if (currentTime.isAfter(currentPrayer.time) &&
                    currentTime.isBefore(nextPrayer.time)
                ) {
                    return currentPrayer.name
                }
            }
        }

        return ""
    }

    val activePrayer = determineActivePrayer(currentDateTime, prayerTimes)

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
            prayerTimes.forEachIndexed { index, prayerInfo ->
                WidgetPrayerTimeColumn(
                    name = prayerInfo.name,
                    time = prayerInfo.displayTime,
                    modifier = GlanceModifier.defaultWeight(),
                    isActive = prayerInfo.name == activePrayer
                )

                // Add spacing between items (except after last)
                if (index < prayerTimes.size - 1) {
                    Spacer(modifier = GlanceModifier.width(4.dp))
                }
            }
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
            .padding(vertical = 6.dp)
            .background(
                if (isActive)
                    GlanceTheme.colors.primaryContainer
                else
                    GlanceTheme.colors.surfaceVariant
            )
            .cornerRadius(12.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Prayer Name
        Text(
            text = name,
            style = TextStyle(
                color = if (isActive)
                    GlanceTheme.colors.onPrimaryContainer
                else
                    GlanceTheme.colors.onSurfaceVariant,
                fontSize = TextUnit(11F, TextUnitType.Sp),
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Prayer Time
        Text(
            text = time,
            style = TextStyle(
                color = if (isActive)
                    GlanceTheme.colors.onPrimaryContainer
                else
                    GlanceTheme.colors.onSurface,
                fontSize = TextUnit(13F, TextUnitType.Sp),
                fontWeight = FontWeight.Bold
            )
        )

        // Active Prayer Indicator
        if (isActive) {
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