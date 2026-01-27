package com.arshadshah.nimaz.widget

import android.content.Context
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.PrayerType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration

data class WidgetPrayerTime(
    val name: String,
    val time: String,
    val isPassed: Boolean,
    val isCurrent: Boolean,
    val isNext: Boolean
)

data class WidgetData(
    val prayerTimes: List<WidgetPrayerTime>,
    val nextPrayerName: String,
    val nextPrayerTime: String,
    val timeUntilNext: String,
    val hijriDay: Int,
    val hijriMonth: String,
    val hijriYear: Int,
    val gregorianDay: String,
    val locationName: String
)

@Singleton
class WidgetDataProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val preferencesDataStore: PreferencesDataStore
) {
    companion object {
        private const val DEFAULT_LATITUDE = 53.3498
        private const val DEFAULT_LONGITUDE = -6.2603
        private const val DEFAULT_LOCATION_NAME = "Dublin"
    }

    suspend fun getWidgetData(): WidgetData {
        val prefs = try {
            preferencesDataStore.userPreferences.first()
        } catch (e: Exception) {
            null
        }

        val latitude = prefs?.latitude?.takeIf { it != 0.0 } ?: DEFAULT_LATITUDE
        val longitude = prefs?.longitude?.takeIf { it != 0.0 } ?: DEFAULT_LONGITUDE
        val locationName = prefs?.locationName?.takeIf { it.isNotBlank() }?.split(",")?.firstOrNull()?.trim()
            ?: DEFAULT_LOCATION_NAME

        val prayerTimes = try {
            prayerTimeCalculator.getPrayerTimes(latitude, longitude)
        } catch (e: Exception) {
            emptyList()
        }

        val currentTime = kotlin.time.Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        val localTime = currentTime.toLocalDateTime(timeZone)

        val widgetPrayerTimes = prayerTimes.map { prayerTime ->
            val prayerLocalTime = prayerTime.time.toLocalDateTime(timeZone)
            val isPassed = prayerLocalTime.time < localTime.time

            WidgetPrayerTime(
                name = prayerTime.type.displayName,
                time = formatTime(prayerLocalTime.hour, prayerLocalTime.minute),
                isPassed = isPassed,
                isCurrent = false,
                isNext = false
            )
        }

        // Find next prayer
        val sortedPrayers = widgetPrayerTimes.sortedBy { prayer ->
            prayerTimes.find { it.type.displayName == prayer.name }?.time
        }

        val nextPrayerIndex = sortedPrayers.indexOfFirst { !it.isPassed }
        val currentPrayerIndex = if (nextPrayerIndex > 0) nextPrayerIndex - 1 else sortedPrayers.lastIndex

        val updatedPrayers = sortedPrayers.mapIndexed { index, prayer ->
            prayer.copy(
                isCurrent = index == currentPrayerIndex,
                isNext = index == nextPrayerIndex
            )
        }

        val nextPrayer = if (nextPrayerIndex >= 0) sortedPrayers[nextPrayerIndex] else null
        val nextPrayerTimeInstant = prayerTimes.find { it.type.displayName == nextPrayer?.name }?.time

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

        // Hijri date
        val hijriDate = HijriDateCalculator.today()
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

        return WidgetData(
            prayerTimes = updatedPrayers,
            nextPrayerName = nextPrayer?.name ?: "—",
            nextPrayerTime = nextPrayer?.time ?: "—",
            timeUntilNext = timeUntilNext,
            hijriDay = hijriDate.day,
            hijriMonth = hijriDate.monthName,
            hijriYear = hijriDate.year,
            gregorianDay = dayOfWeek,
            locationName = locationName
        )
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val h = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
        val amPm = if (hour >= 12) "PM" else "AM"
        return String.format("%d:%02d %s", h, minute, amPm)
    }
}
