package com.arshadshah.nimaz.widget

import com.arshadshah.nimaz.widget.hijricalendar.HijriCalendarData
import com.arshadshah.nimaz.widget.hijricalendar.HijriCalendarEventData
import com.arshadshah.nimaz.widget.hijricalendar.HijriCalendarWidgetState
import com.arshadshah.nimaz.widget.hijridate.HijriDateData
import com.arshadshah.nimaz.widget.hijridate.HijriDateWidgetState
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerData
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerWidgetState
import com.arshadshah.nimaz.widget.prayertimes.PrayerTimesData
import com.arshadshah.nimaz.widget.prayertimes.PrayerTimesWidgetState
import com.arshadshah.nimaz.widget.prayertracker.PrayerTrackerData
import com.arshadshah.nimaz.widget.prayertracker.PrayerTrackerWidgetState
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Serialization round-trip tests for the Glance widget state sealed
 * interfaces. Each widget persists its state as JSON via its StateDefinition,
 * so the polymorphic discrimination (Loading vs Success vs Error) must survive
 * an encode/decode cycle — otherwise a widget loses or mis-restores its state.
 */
class WidgetStateSerializationTest {

    private val json = Json

    @Test
    fun `next prayer state round-trips all variants`() {
        val states = listOf(
            NextPrayerWidgetState.Loading,
            NextPrayerWidgetState.Success(
                NextPrayerData(
                    prayerName = "Asr", prayerTime = "3:45 PM", countdown = "1h 20m",
                    isValid = true, nextPrayerEpochMillis = 1_700_000_000_000
                )
            ),
            NextPrayerWidgetState.Error("boom"),
            NextPrayerWidgetState.Error(null)
        )
        for (state in states) {
            val encoded = json.encodeToString(NextPrayerWidgetState.serializer(), state)
            val decoded = json.decodeFromString(NextPrayerWidgetState.serializer(), encoded)
            assertThat(decoded).isEqualTo(state)
        }
    }

    @Test
    fun `prayer times state round-trips all variants`() {
        val states = listOf(
            PrayerTimesWidgetState.Loading,
            PrayerTimesWidgetState.Success(
                PrayerTimesData(
                    locationName = "Dublin", fajrTime = "5:00", ishaTime = "22:00",
                    fajrPassed = true, ishaPassed = false, nextPrayerEpochMillis = 123
                )
            ),
            PrayerTimesWidgetState.Error("err")
        )
        for (state in states) {
            val encoded = json.encodeToString(PrayerTimesWidgetState.serializer(), state)
            assertThat(json.decodeFromString(PrayerTimesWidgetState.serializer(), encoded))
                .isEqualTo(state)
        }
    }

    @Test
    fun `prayer tracker state round-trips all variants`() {
        val states = listOf(
            PrayerTrackerWidgetState.Loading,
            PrayerTrackerWidgetState.Success(
                PrayerTrackerData(
                    dateLabel = "Mon 16", fajr = true, dhuhr = true, asr = false,
                    maghrib = false, isha = false, prayedCount = 2, totalCount = 5
                )
            ),
            PrayerTrackerWidgetState.Error(null)
        )
        for (state in states) {
            val encoded = json.encodeToString(PrayerTrackerWidgetState.serializer(), state)
            assertThat(json.decodeFromString(PrayerTrackerWidgetState.serializer(), encoded))
                .isEqualTo(state)
        }
    }

    @Test
    fun `hijri date state round-trips all variants`() {
        val states = listOf(
            HijriDateWidgetState.Loading,
            HijriDateWidgetState.Success(
                HijriDateData(
                    hijriDay = 15, hijriMonth = "Ramadan", hijriYear = 1446,
                    gregorianDayOfWeek = "Monday", gregorianDate = "16 Jun 2026"
                )
            ),
            HijriDateWidgetState.Error("x")
        )
        for (state in states) {
            val encoded = json.encodeToString(HijriDateWidgetState.serializer(), state)
            assertThat(json.decodeFromString(HijriDateWidgetState.serializer(), encoded))
                .isEqualTo(state)
        }
    }

    @Test
    fun `hijri calendar state round-trips including nested events`() {
        val states = listOf(
            HijriCalendarWidgetState.Loading,
            HijriCalendarWidgetState.Success(
                HijriCalendarData(
                    hijriMonth = 9, hijriMonthName = "Ramadan", hijriYear = 1446,
                    gregorianDate = "Jun 2026", daysInMonth = 30, firstDayOfWeekOffset = 2,
                    todayHijriDay = 15,
                    events = listOf(
                        HijriCalendarEventData(name = "Laylat al-Qadr", nameArabic = "ليلة القدر", type = "night"),
                        HijriCalendarEventData(name = "Eid", nameArabic = "عيد", type = "holiday")
                    )
                )
            ),
            HijriCalendarWidgetState.Error(null)
        )
        for (state in states) {
            val encoded = json.encodeToString(HijriCalendarWidgetState.serializer(), state)
            val decoded = json.decodeFromString(HijriCalendarWidgetState.serializer(), encoded)
            assertThat(decoded).isEqualTo(state)
        }
    }

    @Test
    fun `loading and success are discriminated as distinct subtypes`() {
        // A Loading payload must not decode into Success (and vice versa); this
        // guards the sealed-interface type discriminator.
        val loadingJson = json.encodeToString(
            NextPrayerWidgetState.serializer(), NextPrayerWidgetState.Loading
        )
        val decoded = json.decodeFromString(NextPrayerWidgetState.serializer(), loadingJson)
        assertThat(decoded).isInstanceOf(NextPrayerWidgetState.Loading::class.java)
        assertThat(decoded).isNotInstanceOf(NextPrayerWidgetState.Success::class.java)
    }
}
