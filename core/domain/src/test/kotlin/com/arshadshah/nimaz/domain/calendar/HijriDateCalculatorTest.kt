package com.arshadshah.nimaz.domain.calendar

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class HijriDateCalculatorTest {

    // ── toHijri / toGregorian round-trip ────────────────────────────

    @Test
    fun `toHijri converts known Gregorian date correctly`() {
        // 1 Ramadan 1446 AH ≈ 1 March 2025
        val hijri = HijriDateCalculator.toHijri(LocalDate.of(2025, 3, 1))
        assertThat(hijri.month).isEqualTo(9) // Ramadan
        assertThat(hijri.year).isEqualTo(1446)
        assertThat(hijri.day).isEqualTo(1)
    }

    @Test
    fun `toGregorian converts known Hijri date correctly`() {
        val gregorian = HijriDateCalculator.toGregorian(1, 9, 1446)
        assertThat(gregorian).isEqualTo(LocalDate.of(2025, 3, 1))
    }

    @Test
    fun `toHijri and toGregorian are inverse operations`() {
        val original = LocalDate.of(2026, 4, 4)
        val hijri = HijriDateCalculator.toHijri(original)
        val roundTripped = HijriDateCalculator.toGregorian(hijri)
        assertThat(roundTripped).isEqualTo(original)
    }

    @Test
    fun `toHijri and toGregorian round-trip for multiple dates`() {
        val dates = listOf(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 6, 15),
            LocalDate.of(2025, 12, 31),
            LocalDate.of(2026, 7, 20)
        )
        for (date in dates) {
            val hijri = HijriDateCalculator.toHijri(date)
            val roundTripped = HijriDateCalculator.toGregorian(hijri)
            assertThat(roundTripped).isEqualTo(date)
        }
    }

    // ── HijriDate formatting ────────────────────────────────────────

    @Test
    fun `HijriDate formatted produces expected string`() {
        val hijri = HijriDateCalculator.HijriDate(1, 9, 1446)
        assertThat(hijri.formatted()).isEqualTo("1 Ramadan 1446 AH")
    }

    @Test
    fun `HijriDate formattedShort produces expected string`() {
        val hijri = HijriDateCalculator.HijriDate(15, 3, 1446)
        assertThat(hijri.formattedShort()).isEqualTo("15/3/1446")
    }

    @Test
    fun `HijriDate formattedArabic produces expected string`() {
        val hijri = HijriDateCalculator.HijriDate(1, 9, 1446)
        assertThat(hijri.formattedArabic()).isEqualTo("1 رمضان 1446 هـ")
    }

    // ── Month names ─────────────────────────────────────────────────

    @Test
    fun `getHijriMonthName returns correct names for all 12 months`() {
        val expected = listOf(
            "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
            "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
            "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
        )
        for (i in 1..12) {
            assertThat(HijriDateCalculator.getHijriMonthName(i)).isEqualTo(expected[i - 1])
        }
    }

    @Test
    fun `getHijriMonthName returns Unknown for invalid month`() {
        assertThat(HijriDateCalculator.getHijriMonthName(0)).isEqualTo("Unknown")
        assertThat(HijriDateCalculator.getHijriMonthName(13)).isEqualTo("Unknown")
    }

    @Test
    fun `getHijriMonthNameArabic returns correct Arabic name for Ramadan`() {
        assertThat(HijriDateCalculator.getHijriMonthNameArabic(9)).isEqualTo("رمضان")
    }

    @Test
    fun `getHijriMonthNameArabic returns fallback for invalid month`() {
        assertThat(HijriDateCalculator.getHijriMonthNameArabic(0)).isEqualTo("غير معروف")
    }

    // ── Ramadan detection ───────────────────────────────────────────

    @Test
    fun `isRamadan returns true for Hijri month 9`() {
        val hijri = HijriDateCalculator.HijriDate(15, 9, 1446)
        assertThat(HijriDateCalculator.isRamadan(hijri)).isTrue()
    }

    @Test
    fun `isRamadan returns false for non-Ramadan month`() {
        val hijri = HijriDateCalculator.HijriDate(15, 10, 1446)
        assertThat(HijriDateCalculator.isRamadan(hijri)).isFalse()
    }

    @Test
    fun `isRamadan with Gregorian date detects Ramadan correctly`() {
        // 1 March 2025 = 1 Ramadan 1446
        assertThat(HijriDateCalculator.isRamadan(LocalDate.of(2025, 3, 1))).isTrue()
    }

    @Test
    fun `isRamadan with Gregorian date returns false outside Ramadan`() {
        // 1 January 2025 is well before Ramadan 1446
        assertThat(HijriDateCalculator.isRamadan(LocalDate.of(2025, 1, 1))).isFalse()
    }

    // ── Ramadan boundaries ──────────────────────────────────────────

    @Test
    fun `getFirstDayOfRamadan returns correct Gregorian date`() {
        val first = HijriDateCalculator.getFirstDayOfRamadan(1446)
        assertThat(first).isEqualTo(LocalDate.of(2025, 3, 1))
    }

    @Test
    fun `getLastDayOfRamadan returns correct Gregorian date`() {
        val last = HijriDateCalculator.getLastDayOfRamadan(1446)
        val hijri = HijriDateCalculator.toHijri(last)
        assertThat(hijri.month).isEqualTo(9)
        // The next day should be Shawwal
        val nextDay = HijriDateCalculator.toHijri(last.plusDays(1))
        assertThat(nextDay.month).isEqualTo(10)
    }

    // ── Days in month / year ────────────────────────────────────────

    @Test
    fun `getDaysInHijriMonth returns 29 or 30 for any month`() {
        for (month in 1..12) {
            val days = HijriDateCalculator.getDaysInHijriMonth(1446, month)
            assertThat(days).isAnyOf(29, 30)
        }
    }

    @Test
    fun `getDaysInHijriYear returns 354 or 355`() {
        val days = HijriDateCalculator.getDaysInHijriYear(1446)
        assertThat(days).isIn(354..355)
    }

    // ── Date validation ─────────────────────────────────────────────

    @Test
    fun `isValidHijriDate returns true for valid date`() {
        assertThat(HijriDateCalculator.isValidHijriDate(1, 1, 1446)).isTrue()
        assertThat(HijriDateCalculator.isValidHijriDate(15, 9, 1446)).isTrue()
    }

    @Test
    fun `isValidHijriDate returns false for invalid month`() {
        assertThat(HijriDateCalculator.isValidHijriDate(1, 0, 1446)).isFalse()
        assertThat(HijriDateCalculator.isValidHijriDate(1, 13, 1446)).isFalse()
    }

    @Test
    fun `isValidHijriDate returns false for day zero or negative`() {
        assertThat(HijriDateCalculator.isValidHijriDate(0, 1, 1446)).isFalse()
        assertThat(HijriDateCalculator.isValidHijriDate(-1, 1, 1446)).isFalse()
    }

    @Test
    fun `isValidHijriDate returns false for day exceeding month length`() {
        assertThat(HijriDateCalculator.isValidHijriDate(31, 1, 1446)).isFalse()
    }

    // ── Islamic events ──────────────────────────────────────────────

    @Test
    fun `getIslamicEvents returns 13 events for a year`() {
        val events = HijriDateCalculator.getIslamicEvents(1446)
        assertThat(events).hasSize(13)
    }

    @Test
    fun `getIslamicEvents includes Eid al-Fitr on 1 Shawwal`() {
        val events = HijriDateCalculator.getIslamicEvents(1446)
        val eidAlFitr = events.find { it.name == "Eid al-Fitr" }
        assertThat(eidAlFitr).isNotNull()
        assertThat(eidAlFitr!!.day).isEqualTo(1)
        assertThat(eidAlFitr.month).isEqualTo(10)
        assertThat(eidAlFitr.type).isEqualTo(HijriDateCalculator.EventType.EID)
    }

    @Test
    fun `getIslamicEvents includes Eid al-Adha on 10 Dhul Hijjah`() {
        val events = HijriDateCalculator.getIslamicEvents(1446)
        val eidAlAdha = events.find { it.name == "Eid al-Adha" }
        assertThat(eidAlAdha).isNotNull()
        assertThat(eidAlAdha!!.day).isEqualTo(10)
        assertThat(eidAlAdha.month).isEqualTo(12)
        assertThat(eidAlAdha.type).isEqualTo(HijriDateCalculator.EventType.EID)
    }

    @Test
    fun `getIslamicEvents includes Day of Ashura as recommended fast`() {
        val events = HijriDateCalculator.getIslamicEvents(1446)
        val ashura = events.find { it.name == "Day of Ashura" }
        assertThat(ashura).isNotNull()
        assertThat(ashura!!.day).isEqualTo(10)
        assertThat(ashura.month).isEqualTo(1)
        assertThat(ashura.type).isEqualTo(HijriDateCalculator.EventType.RECOMMENDED_FAST)
    }

    @Test
    fun `IslamicEvent toGregorianDate converts correctly`() {
        val events = HijriDateCalculator.getIslamicEvents(1446)
        val newYear = events.first { it.name == "Islamic New Year" }
        val gregorian = newYear.toGregorianDate()
        val hijri = HijriDateCalculator.toHijri(gregorian)
        assertThat(hijri.day).isEqualTo(1)
        assertThat(hijri.month).isEqualTo(1)
        assertThat(hijri.year).isEqualTo(1446)
    }

    // ── HijriDate properties ────────────────────────────────────────

    @Test
    fun `HijriDate monthName property returns correct English name`() {
        val hijri = HijriDateCalculator.HijriDate(1, 9, 1446)
        assertThat(hijri.monthName).isEqualTo("Ramadan")
    }

    @Test
    fun `HijriDate monthNameArabic property returns correct Arabic name`() {
        val hijri = HijriDateCalculator.HijriDate(1, 12, 1446)
        assertThat(hijri.monthNameArabic).isEqualTo("ذو الحجة")
    }
}
