package com.arshadshah.nimaz.core.util

import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.WorshipReminderOccurrence
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the single **nearest upcoming enabled** worship reminder for the Home "Next Worship"
 * card (spec §4). Reads all prayer/worship settings itself, computes each enabled reminder's next
 * occurrence via [WorshipReminderCalculator], and returns the earliest one whose event is within a
 * "near" window (so a Tahajjud 20 hours away doesn't sit on Home all day). Returns null when
 * nothing is enabled, no location is set, or nothing is near.
 */
@Singleton
class NextWorshipResolver @Inject constructor(
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val settingsRepository: SettingsRepository
) {
    private val calculator = WorshipReminderCalculator()

    /** Only surface a reminder whose event is within this many hours of now. */
    private val nearWindowHours = 14L

    suspend fun nearest(now: LocalDateTime = LocalDateTime.now()): WorshipReminderOccurrence? {
        val latitude = settingsRepository.latitude.first()
        val longitude = settingsRepository.longitude.first()
        if (latitude == 0.0 && longitude == 0.0) return null

        val enabledTypes = WorshipReminderType.entries.filter {
            settingsRepository.worshipReminderEnabled(it.key).first()
        }
        if (enabledTypes.isEmpty()) return null

        val method = CalculationMethod.fromString(settingsRepository.calculationMethod.first())
        val asr = AsrCalculation.fromString(settingsRepository.asrCalculation.first())
        val highLat = HighLatitudeRule.fromString(settingsRepository.highLatitudeRule.first())
        val hijriOffset = settingsRepository.hijriDayOffset.first()
        val adjustments = mapOf(
            PrayerType.FAJR to settingsRepository.fajrAdjustment.first(),
            PrayerType.SUNRISE to settingsRepository.sunriseAdjustment.first(),
            PrayerType.DHUHR to settingsRepository.dhuhrAdjustment.first(),
            PrayerType.ASR to settingsRepository.asrAdjustment.first(),
            PrayerType.MAGHRIB to settingsRepository.maghribAdjustment.first(),
            PrayerType.ISHA to settingsRepository.ishaAdjustment.first()
        )
        val offsets = enabledTypes.associateWith {
            settingsRepository.worshipReminderOffset(it.key, it.defaultOffsetMinutes).first()
        }
        val witrBeforeFajr = settingsRepository.worshipReminderMode(
            WorshipReminderType.WITR.key, WorshipReminderCalculator.WITR_MODE_AFTER_ISHA
        ).first() == WorshipReminderCalculator.WITR_MODE_BEFORE_FAJR

        val zone = ZoneId.systemDefault()
        fun toLocal(instant: kotlin.time.Instant): LocalDateTime =
            java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds()).atZone(zone).toLocalDateTime()

        val timesFor: (LocalDate) -> DayWorshipTimes? = { date ->
            val byType = prayerTimeCalculator.getPrayerTimes(
                latitude, longitude, date, method, asr, highLat, adjustments
            ).associate { it.type to toLocal(it.time) }
            val sunnah = prayerTimeCalculator.getSunnahTimes(latitude, longitude, date, method, asr, highLat)
            val f = byType[PrayerType.FAJR]; val sr = byType[PrayerType.SUNRISE]
            val d = byType[PrayerType.DHUHR]; val a = byType[PrayerType.ASR]
            val m = byType[PrayerType.MAGHRIB]; val i = byType[PrayerType.ISHA]
            if (f != null && sr != null && d != null && a != null && m != null && i != null) {
                DayWorshipTimes(f, sr, d, a, m, i, toLocal(sunnah.lastThirdOfTheNight))
            } else null
        }
        val hijriFor: (LocalDate) -> HijriDayInfo = { date ->
            val h = HijriDateCalculator.toHijri(date.plusDays(hijriOffset.toLong()))
            HijriDayInfo(h.month, h.day)
        }

        return enabledTypes
            .mapNotNull { type ->
                // Bound the search: anything "near" (≤ nearWindowHours) resolves within ~2 days,
                // so the frequently-ticking Home path never scans the full 40-day horizon.
                calculator.nextOccurrence(
                    type, now, offsets.getValue(type), timesFor, hijriFor,
                    maxSearchDays = 2, witrBeforeFajr = witrBeforeFajr
                )
            }
            .filter { Duration.between(now, it.eventAt) <= Duration.ofHours(nearWindowHours) }
            .minByOrNull { it.triggerAt }
    }
}
