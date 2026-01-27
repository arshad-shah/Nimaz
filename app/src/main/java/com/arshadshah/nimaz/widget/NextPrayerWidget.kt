package com.arshadshah.nimaz.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arshadshah.nimaz.MainActivity
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration

class NextPrayerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetData = getNextPrayerData(context)

        provideContent {
            GlanceTheme {
                NextPrayerWidgetContent(widgetData)
            }
        }
    }

    private suspend fun getNextPrayerData(context: Context): NextPrayerData {
        return try {
            val prefs = PreferencesDataStore(context)
            val userPrefs = prefs.userPreferences.first()

            val latitude = userPrefs.latitude.takeIf { it != 0.0 } ?: 53.3498
            val longitude = userPrefs.longitude.takeIf { it != 0.0 } ?: -6.2603

            val calculator = PrayerTimeCalculator()
            val prayerTimes = calculator.getPrayerTimes(latitude, longitude)

            val currentTime = kotlin.time.Clock.System.now()
            val timeZone = TimeZone.currentSystemDefault()
            val localTime = currentTime.toLocalDateTime(timeZone)

            // Find next prayer
            val nextPrayer = prayerTimes.firstOrNull { prayerTime ->
                val prayerLocalTime = prayerTime.time.toLocalDateTime(timeZone)
                prayerLocalTime.time > localTime.time
            }

            if (nextPrayer != null) {
                val prayerLocalTime = nextPrayer.time.toLocalDateTime(timeZone)
                val diff: Duration = nextPrayer.time - currentTime
                val totalSeconds = diff.inWholeSeconds
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60

                val countdown = when {
                    hours > 0 -> "${hours}h ${minutes}m"
                    minutes > 0 -> "${minutes}m ${seconds}s"
                    else -> "${seconds}s"
                }

                NextPrayerData(
                    prayerName = nextPrayer.type.displayName,
                    prayerTime = formatTime(prayerLocalTime.hour, prayerLocalTime.minute),
                    countdown = countdown,
                    isValid = true
                )
            } else {
                // All prayers passed, show Fajr for tomorrow
                NextPrayerData(
                    prayerName = "Fajr",
                    prayerTime = "Tomorrow",
                    countdown = "—",
                    isValid = true
                )
            }
        } catch (e: Exception) {
            NextPrayerData(
                prayerName = "—",
                prayerTime = "—",
                countdown = "Tap to setup",
                isValid = false
            )
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val h = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
        val amPm = if (hour >= 12) "PM" else "AM"
        return String.format("%d:%02d %s", h, minute, amPm)
    }
}

data class NextPrayerData(
    val prayerName: String,
    val prayerTime: String,
    val countdown: String,
    val isValid: Boolean
)

@Composable
private fun NextPrayerWidgetContent(data: NextPrayerData) {
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
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "Next Prayer" label
            Text(
                text = "Next Prayer",
                style = TextStyle(
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Prayer name
            Text(
                text = data.prayerName,
                style = TextStyle(
                    color = primaryColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            // Prayer time
            Text(
                text = data.prayerTime,
                style = TextStyle(
                    color = textColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Countdown badge
            Box(
                modifier = GlanceModifier
                    .background(ColorProvider(R.color.widget_primary_dim))
                    .cornerRadius(8.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (data.isValid) "in " else "",
                        style = TextStyle(
                            color = primaryColor,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = data.countdown,
                        style = TextStyle(
                            color = primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

class NextPrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextPrayerWidget()
}
