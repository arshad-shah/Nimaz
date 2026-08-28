package com.arshadshah.nimaz.core.util

import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The wall-clock arithmetic behind a scheduled prayer alarm, kept apart from
 * [PrayerNotificationScheduler] so it can be tested without an `AlarmManager`.
 *
 * Two cases make this worth stating explicitly. A reminder before an early Fajr belongs to
 * the previous day, and a wall-clock time inside a daylight-saving change is either missing
 * or repeated — neither of which may quietly drop an alarm.
 */
object PrayerAlarmTimes {

    /** When a reminder [minutesBefore] a prayer at [prayerTime] lands. Crosses midnight. */
    fun preReminderAt(prayerTime: LocalDateTime, minutesBefore: Int): LocalDateTime =
        prayerTime.minusMinutes(minutesBefore.toLong())

    /**
     * The instant to arm an alarm for, resolving [time] in [zone].
     *
     * Where the clock jumped forward the wall-clock time does not exist; `atZone` resolves it
     * forward by the size of the gap, which is what we want — an alarm slightly late beats an
     * alarm that never fires. Where the clock went back the time happens twice, and we take
     * the earlier of the two so a reminder cannot land after the prayer it precedes.
     */
    fun triggerMillis(time: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): Long =
        time.atZone(zone).withEarlierOffsetAtOverlap().toInstant().toEpochMilli()
}
