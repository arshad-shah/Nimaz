package com.arshadshah.nimaz.widget.prayertimes

import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.domain.model.PrayerTime
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.resolveLocation
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.widget.core.formatWidgetTime
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/**
 * Computes what the prayer-times widget shows.
 *
 * Split out of [PrayerTimesWorker] so it can be tested — `doWork()` returns early when no widget
 * is placed, which is always true on a test device (#474).
 *
 * **Behaviour preserved exactly, including two oddities**, because a refactor that changes what a
 * widget displays is not reviewable:
 *
 *  - `HijriDateCalculator.today()` is called with **no offset**, so this widget ignores
 *    `hijriDayOffset` — the inverse of the Hijri-date widget, which applies it to too much
 *    (#509). Two widgets on the same home screen can therefore disagree about the Hijri date.
 *  - The location name falls back to a hardcoded `"Dublin"` when the stored one is blank.
 */
class PrayerTimesWidgetDataSource @Inject constructor(
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun load(): PrayerTimesData {
        val resolved = resolveLocation(
            settingsRepository.latitude.first(),
            settingsRepository.longitude.first(),
        )
        val use24Hour = settingsRepository.use24HourFormat.first()

        // The stored name is "City, Region, Country"; the widget has room for the city.
        val locationName = settingsRepository.locationName.first()
            .takeIf { it.isNotBlank() }
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?: DEFAULT_LOCATION_NAME

        val timeZone = TimeZone.currentSystemDefault()
        val byType = prayerTimeCalculator
            .getPrayerTimes(resolved.latitude, resolved.longitude)
            .associateBy { it.type }

        fun format(prayer: PrayerTime?): String = prayer?.let {
            val local = it.time.toLocalDateTime(timeZone)
            formatWidgetTime(local.hour, local.minute, use24Hour = use24Hour)
        } ?: MISSING

        fun epochOf(prayer: PrayerTime?): Long = prayer?.time?.toEpochMilliseconds() ?: 0L

        // See the class KDoc: no offset, unlike the Hijri-date widget. Preserved.
        val hijriDate = HijriDateCalculator.today()

        return PrayerTimesData(
            locationName = locationName,
            hijriDate = "${hijriDate.day} ${hijriDate.monthName}",
            fajrTime = format(byType[PrayerType.FAJR]),
            dhuhrTime = format(byType[PrayerType.DHUHR]),
            asrTime = format(byType[PrayerType.ASR]),
            maghribTime = format(byType[PrayerType.MAGHRIB]),
            ishaTime = format(byType[PrayerType.ISHA]),
            fajrEpochMillis = epochOf(byType[PrayerType.FAJR]),
            dhuhrEpochMillis = epochOf(byType[PrayerType.DHUHR]),
            asrEpochMillis = epochOf(byType[PrayerType.ASR]),
            maghribEpochMillis = epochOf(byType[PrayerType.MAGHRIB]),
            ishaEpochMillis = epochOf(byType[PrayerType.ISHA]),
        )
    }

    private companion object {
        /** Shown when a prayer cannot be computed. An em dash, not a blank, so the row still reads. */
        const val MISSING = "—"
        const val DEFAULT_LOCATION_NAME = "Dublin"
    }
}
