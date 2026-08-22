package com.arshadshah.nimaz.widget.prayertimes

import com.arshadshah.nimaz.domain.time.TodayProvider
import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.PrayerTime
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.core.common.formatWidgetTime
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
 * **Prayer times come from [PrayerRepository], not from `PrayerTimeCalculator` directly.** This
 * used to call `getPrayerTimes(latitude, longitude)` and take all four calculation defaults —
 * Muslim World League, Shafi asr, no high-latitude rule, no per-prayer adjustments — so the five
 * times on the widget disagreed with the five in the app for every user who had changed any of
 * them, and the "next prayer" countdown beside them was wrong by the same margin. The repository
 * resolves the user's settings once and applies all of them.
 *
 * One documented oddity is preserved: `HijriDateCalculator.today()` is called with **no offset**,
 * so this widget ignores `hijriDayOffset` — the inverse of the Hijri-date widget, which applies
 * it to too much (#509). Two widgets on the same home screen can therefore disagree about the
 * Hijri date.
 */
class PrayerTimesWidgetDataSource @Inject constructor(
    private val prayerRepository: PrayerRepository,
    private val settingsRepository: SettingsRepository,
    private val todayProvider: TodayProvider,
) {

    suspend fun load(): PrayerTimesData {
        val use24Hour = settingsRepository.use24HourFormat.first()
        val settings = prayerRepository.observeCalculationSettings().first()

        // The resolved name is "City, Region, Country"; the widget has room for the city. It is
        // already the fallback location's name when the user has no position stored, so there is
        // no separate hardcoded default here.
        val locationName = settings.location.name
            .takeIf { it.isNotBlank() }
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            .orEmpty()

        val timeZone = TimeZone.currentSystemDefault()
        val byType = prayerRepository
            .getDaySchedule(todayProvider.today(), settings)
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
    }
}
