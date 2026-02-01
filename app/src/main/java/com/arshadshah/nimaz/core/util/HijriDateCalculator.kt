package com.arshadshah.nimaz.core.util

import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoUnit

/**
 * Hijri (Islamic) Date Calculator using the official java.time HijrahChronology (Umm al-Qura calendar).
 *
 * The Umm al-Qura calendar is the official calendar used in Saudi Arabia.
 * This implementation delegates all date conversion to the platform's HijrahChronology,
 * which is based on ICU data and is kept up-to-date with the platform.
 */
object HijriDateCalculator {

    /**
     * Represents a Hijri (Islamic) date.
     */
    data class HijriDate(
        val day: Int,
        val month: Int,
        val year: Int
    ) {
        val monthName: String get() = getHijriMonthName(month)
        val monthNameArabic: String get() = getHijriMonthNameArabic(month)

        fun formatted(): String = "$day $monthName $year AH"
        fun formattedArabic(): String = "$day $monthNameArabic $year هـ"
        fun formattedShort(): String = "$day/${month}/$year"
    }

    // Hijri month names in English
    private val hijriMonthNames = listOf(
        "Muharram",
        "Safar",
        "Rabi' al-Awwal",
        "Rabi' al-Thani",
        "Jumada al-Awwal",
        "Jumada al-Thani",
        "Rajab",
        "Sha'ban",
        "Ramadan",
        "Shawwal",
        "Dhu al-Qi'dah",
        "Dhu al-Hijjah"
    )

    // Hijri month names in Arabic
    private val hijriMonthNamesArabic = listOf(
        "محرم",
        "صفر",
        "ربيع الأول",
        "ربيع الثاني",
        "جمادى الأولى",
        "جمادى الثانية",
        "رجب",
        "شعبان",
        "رمضان",
        "شوال",
        "ذو القعدة",
        "ذو الحجة"
    )

    private val hijrahChronology = HijrahChronology.INSTANCE

    fun getHijriMonthName(month: Int): String {
        return hijriMonthNames.getOrElse(month - 1) { "Unknown" }
    }

    fun getHijriMonthNameArabic(month: Int): String {
        return hijriMonthNamesArabic.getOrElse(month - 1) { "غير معروف" }
    }

    /**
     * Calculate the number of days in a Hijri year.
     */
    fun getDaysInHijriYear(hijriYear: Int): Int {
        return (1..12).sumOf { month -> getDaysInHijriMonth(hijriYear, month) }
    }

    /**
     * Calculate the number of days in a Hijri month.
     */
    fun getDaysInHijriMonth(hijriYear: Int, hijriMonth: Int): Int {
        val hijrahDate = hijrahChronology.date(hijriYear, hijriMonth, 1)
        return hijrahDate.lengthOfMonth()
    }

    /**
     * Convert a Gregorian date to Hijri date.
     */
    fun toHijri(gregorianDate: LocalDate): HijriDate {
        val hijrahDate = HijrahDate.from(gregorianDate)
        return HijriDate(
            day = hijrahDate.get(java.time.temporal.ChronoField.DAY_OF_MONTH),
            month = hijrahDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR),
            year = hijrahDate.get(java.time.temporal.ChronoField.YEAR)
        )
    }

    /**
     * Convert a Hijri date to Gregorian date.
     */
    fun toGregorian(hijriDate: HijriDate): LocalDate {
        return toGregorian(hijriDate.day, hijriDate.month, hijriDate.year)
    }

    /**
     * Convert a Hijri date to Gregorian date.
     */
    fun toGregorian(day: Int, month: Int, year: Int): LocalDate {
        val hijrahDate = hijrahChronology.date(year, month, day)
        return LocalDate.from(hijrahDate)
    }

    /**
     * Get today's Hijri date.
     */
    fun today(): HijriDate {
        return toHijri(LocalDate.now())
    }

    /**
     * Check if a given Hijri date falls in Ramadan.
     */
    fun isRamadan(hijriDate: HijriDate): Boolean = hijriDate.month == 9

    /**
     * Check if a given Hijri date falls in Ramadan.
     */
    fun isRamadan(gregorianDate: LocalDate): Boolean = toHijri(gregorianDate).month == 9

    /**
     * Check if today is in Ramadan.
     */
    fun isTodayRamadan(): Boolean = isRamadan(LocalDate.now())

    /**
     * Get the first day of Ramadan for a given Hijri year.
     */
    fun getFirstDayOfRamadan(hijriYear: Int): LocalDate {
        return toGregorian(1, 9, hijriYear)
    }

    /**
     * Get the last day of Ramadan for a given Hijri year.
     */
    fun getLastDayOfRamadan(hijriYear: Int): LocalDate {
        val daysInRamadan = getDaysInHijriMonth(hijriYear, 9)
        return toGregorian(daysInRamadan, 9, hijriYear)
    }

    /**
     * Check if a Hijri date is valid.
     */
    fun isValidHijriDate(day: Int, month: Int, year: Int): Boolean {
        if (month !in 1..12) return false
        if (day < 1) return false
        return try {
            val daysInMonth = getDaysInHijriMonth(year, month)
            day <= daysInMonth
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Get days until next Ramadan from today.
     */
    fun daysUntilNextRamadan(): Int {
        val today = LocalDate.now()
        val hijriToday = toHijri(today)

        val targetYear = if (hijriToday.month >= 9) hijriToday.year + 1 else hijriToday.year
        val firstRamadan = getFirstDayOfRamadan(targetYear)

        return ChronoUnit.DAYS.between(today, firstRamadan).toInt()
    }

    /**
     * Get days remaining in Ramadan (if currently in Ramadan).
     * Returns -1 if not in Ramadan.
     */
    fun daysRemainingInRamadan(): Int {
        val today = LocalDate.now()
        val hijriToday = toHijri(today)

        if (hijriToday.month != 9) return -1

        val daysInRamadan = getDaysInHijriMonth(hijriToday.year, 9)
        return daysInRamadan - hijriToday.day + 1
    }

    /**
     * Get all Islamic events for a given Hijri year.
     */
    fun getIslamicEvents(hijriYear: Int): List<IslamicEvent> {
        return listOf(
            IslamicEvent(1, 1, hijriYear, "Islamic New Year", "رأس السنة الهجرية", EventType.HOLIDAY),
            IslamicEvent(10, 1, hijriYear, "Day of Ashura", "يوم عاشوراء", EventType.RECOMMENDED_FAST),
            IslamicEvent(12, 3, hijriYear, "Mawlid al-Nabi", "المولد النبوي", EventType.COMMEMORATION),
            IslamicEvent(27, 7, hijriYear, "Isra and Mi'raj", "الإسراء والمعراج", EventType.COMMEMORATION),
            IslamicEvent(15, 8, hijriYear, "Mid-Sha'ban", "ليلة النصف من شعبان", EventType.COMMEMORATION),
            IslamicEvent(1, 9, hijriYear, "First Day of Ramadan", "أول أيام رمضان", EventType.RAMADAN),
            IslamicEvent(27, 9, hijriYear, "Laylat al-Qadr (estimated)", "ليلة القدر", EventType.SPECIAL_NIGHT),
            IslamicEvent(1, 10, hijriYear, "Eid al-Fitr", "عيد الفطر", EventType.EID),
            IslamicEvent(9, 12, hijriYear, "Day of Arafah", "يوم عرفة", EventType.RECOMMENDED_FAST),
            IslamicEvent(10, 12, hijriYear, "Eid al-Adha", "عيد الأضحى", EventType.EID),
            IslamicEvent(11, 12, hijriYear, "Tashreeq Day 1", "أيام التشريق", EventType.HOLIDAY),
            IslamicEvent(12, 12, hijriYear, "Tashreeq Day 2", "أيام التشريق", EventType.HOLIDAY),
            IslamicEvent(13, 12, hijriYear, "Tashreeq Day 3", "أيام التشريق", EventType.HOLIDAY)
        )
    }

    /**
     * Get upcoming Islamic events from today.
     */
    fun getUpcomingEvents(limit: Int = 5): List<IslamicEvent> {
        val today = toHijri(LocalDate.now())
        val thisYearEvents = getIslamicEvents(today.year)
        val nextYearEvents = getIslamicEvents(today.year + 1)

        val allEvents = (thisYearEvents + nextYearEvents)
            .filter { event ->
                val eventHijri = HijriDate(event.day, event.month, event.year)
                when {
                    eventHijri.year > today.year -> true
                    eventHijri.year < today.year -> false
                    eventHijri.month > today.month -> true
                    eventHijri.month < today.month -> false
                    else -> eventHijri.day >= today.day
                }
            }
            .sortedWith(compareBy({ it.year }, { it.month }, { it.day }))
            .take(limit)

        return allEvents
    }

    data class IslamicEvent(
        val day: Int,
        val month: Int,
        val year: Int,
        val name: String,
        val nameArabic: String,
        val type: EventType
    ) {
        fun toGregorianDate(): LocalDate = toGregorian(day, month, year)
        fun toHijriDate(): HijriDate = HijriDate(day, month, year)
    }

    enum class EventType {
        EID,
        HOLIDAY,
        RAMADAN,
        SPECIAL_NIGHT,
        RECOMMENDED_FAST,
        COMMEMORATION
    }
}
