package com.arshadshah.nimaz.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The arithmetic behind a pre-adhan reminder: when it lands, and what instant the alarm is
 * armed for. Both are easy to get subtly wrong — a reminder before an early Fajr belongs to
 * the previous day, and a wall-clock time inside a spring-forward gap does not exist at all.
 */
class PrayerAlarmTimesTest {

    private val dublin = ZoneId.of("Europe/Dublin")

    @Test
    fun `a reminder lands the given number of minutes before the prayer`() {
        val fajr = LocalDateTime.of(2026, 8, 4, 5, 12)

        assertThat(PrayerAlarmTimes.preReminderAt(fajr, 10))
            .isEqualTo(LocalDateTime.of(2026, 8, 4, 5, 2))
    }

    @Test
    fun `a reminder before an early prayer crosses back over midnight`() {
        // Fajr at 00:05 with a 10-minute lead belongs to the previous day, not to 00:55.
        val fajr = LocalDateTime.of(2026, 6, 21, 0, 5)

        assertThat(PrayerAlarmTimes.preReminderAt(fajr, 10))
            .isEqualTo(LocalDateTime.of(2026, 6, 20, 23, 55))
    }

    @Test
    fun `arming an alarm resolves the wall clock in the given zone`() {
        val time = LocalDateTime.of(2026, 8, 4, 5, 12)

        assertThat(PrayerAlarmTimes.triggerMillis(time, dublin))
            .isEqualTo(time.atZone(dublin).toInstant().toEpochMilli())
    }

    @Test
    fun `a reminder falling inside the spring-forward gap still arms`() {
        // Europe/Dublin jumps 01:00 to 02:00 on 2026-03-29, so 01:30 never happens.
        // A prayer at 01:35 with a 10-minute lead computes 01:25 — a wall-clock time with
        // no instant. It must resolve forward rather than throw or silently drop the alarm.
        val prayer = LocalDateTime.of(2026, 3, 29, 1, 35)
        val reminder = PrayerAlarmTimes.preReminderAt(prayer, 10)
        assertThat(reminder).isEqualTo(LocalDateTime.of(2026, 3, 29, 1, 25))

        val armed = PrayerAlarmTimes.triggerMillis(reminder, dublin)
        // 01:25 IST-gap resolves to 02:25 local, which is 01:25 UTC.
        assertThat(armed).isEqualTo(
            LocalDateTime.of(2026, 3, 29, 2, 25).atZone(dublin).toInstant().toEpochMilli()
        )
    }

    @Test
    fun `a reminder in the autumn overlap picks the earlier of the two instants`() {
        // Dublin repeats 01:00-02:00 on 2026-10-25. A reminder at 01:30 is ambiguous;
        // taking the earlier offset means the reminder never fires after its prayer.
        val reminder = LocalDateTime.of(2026, 10, 25, 1, 30)

        assertThat(PrayerAlarmTimes.triggerMillis(reminder, dublin)).isEqualTo(
            reminder.atZone(dublin).withEarlierOffsetAtOverlap().toInstant().toEpochMilli()
        )
    }
}
