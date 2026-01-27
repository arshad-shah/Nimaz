package com.arshadshah.nimaz.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arshadshah.nimaz.MainActivity
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Duration

class PrayerTimesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Get prayer times data
        val widgetData = getWidgetData(context)

        provideContent {
            GlanceTheme {
                PrayerTimesWidgetContent(widgetData)
            }
        }
    }

    private suspend fun getWidgetData(context: Context): WidgetDisplayData {
        return try {
            val prefs = PreferencesDataStore(context)
            val userPrefs = prefs.userPreferences.first()

            val latitude = userPrefs.latitude.takeIf { it != 0.0 } ?: 53.3498
            val longitude = userPrefs.longitude.takeIf { it != 0.0 } ?: -6.2603
            val locationName = userPrefs.locationName.takeIf { it.isNotBlank() }?.split(",")?.firstOrNull()?.trim()
                ?: "Dublin"

            val calculator = PrayerTimeCalculator()
            val prayerTimes = calculator.getPrayerTimes(latitude, longitude)

            val currentTime = kotlin.time.Clock.System.now()
            val timeZone = TimeZone.currentSystemDefault()
            val localTime = currentTime.toLocalDateTime(timeZone)

            val prayers = prayerTimes.map { prayerTime ->
                val prayerLocalTime = prayerTime.time.toLocalDateTime(timeZone)
                val isPassed = prayerLocalTime.time < localTime.time

                PrayerDisplay(
                    name = getShortName(prayerTime.type.displayName),
                    time = formatTime(prayerLocalTime.hour, prayerLocalTime.minute),
                    isPassed = isPassed
                )
            }

            // Find next prayer
            val nextPrayerIndex = prayers.indexOfFirst { !it.isPassed }
            val nextPrayer = if (nextPrayerIndex >= 0) prayers[nextPrayerIndex] else null
            val nextPrayerTimeInstant = prayerTimes.getOrNull(nextPrayerIndex)?.time

            val timeUntilNext = if (nextPrayerTimeInstant != null) {
                val diff: Duration = nextPrayerTimeInstant - currentTime
                val totalMinutes = diff.inWholeMinutes
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                when {
                    hours > 0 -> "${hours}h ${minutes}m"
                    else -> "${minutes}m"
                }
            } else "—"

            val hijriDate = HijriDateCalculator.today()

            WidgetDisplayData(
                prayers = prayers,
                nextPrayerName = nextPrayer?.name ?: "—",
                timeUntilNext = timeUntilNext,
                hijriDate = "${hijriDate.day} ${hijriDate.monthName}",
                locationName = locationName
            )
        } catch (e: Exception) {
            WidgetDisplayData(
                prayers = listOf(
                    PrayerDisplay("Fajr", "—", false),
                    PrayerDisplay("Dhuhr", "—", false),
                    PrayerDisplay("Asr", "—", false),
                    PrayerDisplay("Mgrb", "—", false),
                    PrayerDisplay("Isha", "—", false)
                ),
                nextPrayerName = "—",
                timeUntilNext = "—",
                hijriDate = "—",
                locationName = "Tap to setup"
            )
        }
    }

    private fun getShortName(name: String): String {
        return when (name.lowercase()) {
            "fajr" -> "Fajr"
            "sunrise" -> "Rise"
            "dhuhr" -> "Dhuhr"
            "asr" -> "Asr"
            "maghrib" -> "Mgrb"
            "isha" -> "Isha"
            else -> name.take(4)
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val h = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
        return String.format("%d:%02d", h, minute)
    }
}

data class PrayerDisplay(
    val name: String,
    val time: String,
    val isPassed: Boolean
)

data class WidgetDisplayData(
    val prayers: List<PrayerDisplay>,
    val nextPrayerName: String,
    val timeUntilNext: String,
    val hijriDate: String,
    val locationName: String
)

@Composable
private fun PrayerTimesWidgetContent(data: WidgetDisplayData) {
    val backgroundColor = ColorProvider(R.color.widget_background)
    val textColor = ColorProvider(R.color.widget_text)
    val textSecondary = ColorProvider(R.color.widget_text_secondary)
    val primaryColor = ColorProvider(R.color.widget_primary)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(12.dp)
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = data.locationName,
                        style = TextStyle(
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = data.hijriDate,
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 11.sp
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = data.nextPrayerName,
                        style = TextStyle(
                            color = primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "in ${data.timeUntilNext}",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Prayer times row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                data.prayers.filter { it.name != "Rise" }.forEach { prayer ->
                    PrayerTimeItem(
                        name = prayer.name,
                        time = prayer.time,
                        isPassed = prayer.isPassed,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        primaryColor = primaryColor,
                        modifier = GlanceModifier.defaultWeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerTimeItem(
    name: String,
    time: String,
    isPassed: Boolean,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = name,
            style = TextStyle(
                color = if (isPassed) textSecondary else textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Text(
            text = time,
            style = TextStyle(
                color = if (isPassed) textSecondary else primaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

class PrayerTimesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerTimesWidget()
}
