package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.PrayerType
import kotlinx.coroutines.flow.first

/**
 * Reads the per-prayer notification preferences into the shape the scheduler wants.
 *
 * Three places arm today's alarms — app start, boot, and a settings change — and each of
 * them needs the same two answers. Keeping the reads here means the three cannot drift, and
 * that adding a prayer is one edit rather than three.
 *
 * They live beside [SettingsRepository] rather than in `core/util` because that is all they
 * are: pure extensions on a domain interface, reading domain flags into domain types. Sitting
 * in `core/util` they were an outward import from `RescheduleNotificationsUseCase` for no
 * reason other than where the file happened to be.
 */

/** The five prayers a user can be reminded about. Sunrise gets no pre-reminder. */
private val REMINDABLE_PRAYERS = mapOf(
    "fajr" to PrayerType.FAJR,
    "dhuhr" to PrayerType.DHUHR,
    "asr" to PrayerType.ASR,
    "maghrib" to PrayerType.MAGHRIB,
    "isha" to PrayerType.ISHA,
)

/**
 * The lead time for each prayer whose reminder is on, in minutes. A prayer missing from the
 * map gets no reminder — which is how "off" is expressed, rather than a zero offset.
 */
suspend fun SettingsRepository.preReminderMinutesByPrayer(): Map<PrayerType, Int> =
    REMINDABLE_PRAYERS.mapNotNull { (key, type) ->
        if (prayerReminderEnabled(key).first()) type to prayerReminderMinutes(key).first()
        else null
    }.toMap()

/** The prayers whose notification is switched on at all, including sunrise. */
suspend fun SettingsRepository.enabledPrayerTypes(): Set<PrayerType> = buildSet {
    if (fajrNotificationEnabled.first()) add(PrayerType.FAJR)
    if (sunriseNotificationEnabled.first()) add(PrayerType.SUNRISE)
    if (dhuhrNotificationEnabled.first()) add(PrayerType.DHUHR)
    if (asrNotificationEnabled.first()) add(PrayerType.ASR)
    if (maghribNotificationEnabled.first()) add(PrayerType.MAGHRIB)
    if (ishaNotificationEnabled.first()) add(PrayerType.ISHA)
}
