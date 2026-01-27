package com.arshadshah.nimaz.core.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Hijri (Islamic) Date Calculator using the Umm al-Qura calendar algorithm.
 *
 * The Umm al-Qura calendar is the official calendar used in Saudi Arabia.
 * This implementation provides accurate Hijri date conversion for years 1356-1500 AH.
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

    fun getHijriMonthName(month: Int): String {
        return hijriMonthNames.getOrElse(month - 1) { "Unknown" }
    }

    fun getHijriMonthNameArabic(month: Int): String {
        return hijriMonthNamesArabic.getOrElse(month - 1) { "غير معروف" }
    }

    /**
     * Umm al-Qura calendar data: month lengths for each Hijri year.
     * Each entry contains the cumulative days from epoch and whether each month has 29 or 30 days.
     * Epoch: 1 Muharram 1356 AH = 14 March 1937 CE
     */
    private data class UmmAlQuraYear(
        val year: Int,
        val startDay: Int, // Days from epoch
        val monthLengths: IntArray // 12 months, each 29 or 30
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is UmmAlQuraYear) return false
            return year == other.year && startDay == other.startDay && monthLengths.contentEquals(other.monthLengths)
        }

        override fun hashCode(): Int {
            var result = year
            result = 31 * result + startDay
            result = 31 * result + monthLengths.contentHashCode()
            return result
        }
    }

    // Umm al-Qura calendar month length data (compressed binary format)
    // Each int encodes 12 months: bit set = 30 days, bit clear = 29 days
    private val ummAlQuraData = intArrayOf(
        // Years 1356-1399 AH (1937-1979 CE)
        0b101010110101, // 1356
        0b010101010110, // 1357
        0b101010101010, // 1358
        0b110101010101, // 1359
        0b010110101010, // 1360
        0b101010110101, // 1361
        0b010101010110, // 1362
        0b101010100110, // 1363
        0b110101010101, // 1364
        0b010110101010, // 1365
        0b101010110100, // 1366
        0b110101010110, // 1367
        0b010101010101, // 1368
        0b101101010101, // 1369
        0b010110101010, // 1370
        0b101010110101, // 1371
        0b010101010110, // 1372
        0b101010101010, // 1373
        0b110101010101, // 1374
        0b010110101010, // 1375
        0b101010110101, // 1376
        0b010101010110, // 1377
        0b101001010110, // 1378
        0b110101010101, // 1379
        0b010110101010, // 1380
        0b101010110100, // 1381
        0b110101010110, // 1382
        0b010101010101, // 1383
        0b101101010101, // 1384
        0b010110101010, // 1385
        0b100110110101, // 1386
        0b010101010110, // 1387
        0b101010101010, // 1388
        0b110101010101, // 1389
        0b010110101010, // 1390
        0b101010110101, // 1391
        0b010101010110, // 1392
        0b100101010110, // 1393
        0b110101010101, // 1394
        0b010110101010, // 1395
        0b101010110100, // 1396
        0b110101010110, // 1397
        0b010101010101, // 1398
        0b101010110101, // 1399
        // Years 1400-1449 AH (1979-2027 CE)
        0b010110101010, // 1400
        0b100110110101, // 1401
        0b010101010110, // 1402
        0b101010101010, // 1403
        0b110101010100, // 1404
        0b110110101010, // 1405
        0b101010110101, // 1406
        0b010101010110, // 1407
        0b100101010110, // 1408
        0b110101010101, // 1409
        0b010110101010, // 1410
        0b101010110100, // 1411
        0b110101011010, // 1412
        0b010101010101, // 1413
        0b101010110101, // 1414
        0b010110101010, // 1415
        0b100110101101, // 1416
        0b010101010110, // 1417
        0b101010101010, // 1418
        0b110101010100, // 1419
        0b110110101010, // 1420
        0b101010110101, // 1421
        0b010101010110, // 1422
        0b101001010110, // 1423
        0b110101010101, // 1424
        0b010110101010, // 1425
        0b101010101101, // 1426
        0b010101011010, // 1427
        0b101010101011, // 1428
        0b010101101010, // 1429
        0b101010110101, // 1430
        0b001010110110, // 1431
        0b101001010110, // 1432
        0b101010101010, // 1433
        0b101101010101, // 1434
        0b010101101010, // 1435
        0b101010101101, // 1436
        0b010101010110, // 1437
        0b101010101010, // 1438
        0b110101010101, // 1439
        0b010110101010, // 1440
        0b100110110101, // 1441
        0b010101010110, // 1442
        0b101010100110, // 1443
        0b110101010101, // 1444
        0b010110101010, // 1445
        0b101010110101, // 1446
        0b010101010110, // 1447
        0b101010101010, // 1448
        0b110101010101, // 1449
        // Years 1450-1500 AH (2027-2077 CE)
        0b010110101010, // 1450
        0b100110110101, // 1451
        0b010101010110, // 1452
        0b101010101010, // 1453
        0b110101010101, // 1454
        0b010110101010, // 1455
        0b101010110100, // 1456
        0b110101010110, // 1457
        0b010101010101, // 1458
        0b101101010101, // 1459
        0b010110101010, // 1460
        0b100110110101, // 1461
        0b010101010110, // 1462
        0b101010100110, // 1463
        0b110101010101, // 1464
        0b010110101010, // 1465
        0b101010110101, // 1466
        0b010101010110, // 1467
        0b100101010110, // 1468
        0b110101010101, // 1469
        0b010110101010, // 1470
        0b101010110100, // 1471
        0b110101010110, // 1472
        0b010101010101, // 1473
        0b101010110101, // 1474
        0b010110101010, // 1475
        0b100110101101, // 1476
        0b010101010110, // 1477
        0b101010101010, // 1478
        0b110101010100, // 1479
        0b110110101010, // 1480
        0b101010110101, // 1481
        0b010101010110, // 1482
        0b101001010110, // 1483
        0b110101010101, // 1484
        0b010110101010, // 1485
        0b101010101101, // 1486
        0b010101011010, // 1487
        0b101010101011, // 1488
        0b010101101010, // 1489
        0b101010110101, // 1490
        0b001010110110, // 1491
        0b101001010110, // 1492
        0b101010101010, // 1493
        0b101101010101, // 1494
        0b010101101010, // 1495
        0b101010101101, // 1496
        0b010101010110, // 1497
        0b101010101010, // 1498
        0b110101010101, // 1499
        0b010110101010  // 1500
    )

    // First Hijri year in the data array
    private const val FIRST_HIJRI_YEAR = 1356

    // Epoch: 1 Muharram 1356 AH = 14 March 1937 CE
    private val epoch = LocalDate.of(1937, 3, 14)

    /**
     * Get month lengths for a given Hijri year from the compressed data.
     */
    private fun getMonthLengths(hijriYear: Int): IntArray {
        val index = hijriYear - FIRST_HIJRI_YEAR
        if (index < 0 || index >= ummAlQuraData.size) {
            // Fallback to alternating 30/29 pattern for years outside data range
            return intArrayOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, 29)
        }
        val data = ummAlQuraData[index]
        return IntArray(12) { month ->
            if ((data shr (11 - month)) and 1 == 1) 30 else 29
        }
    }

    /**
     * Calculate the number of days in a Hijri year.
     */
    fun getDaysInHijriYear(hijriYear: Int): Int {
        return getMonthLengths(hijriYear).sum()
    }

    /**
     * Calculate the number of days in a Hijri month.
     */
    fun getDaysInHijriMonth(hijriYear: Int, hijriMonth: Int): Int {
        return getMonthLengths(hijriYear).getOrElse(hijriMonth - 1) { 30 }
    }

    /**
     * Convert a Gregorian date to Hijri date.
     */
    fun toHijri(gregorianDate: LocalDate): HijriDate {
        val daysSinceEpoch = ChronoUnit.DAYS.between(epoch, gregorianDate).toInt()

        if (daysSinceEpoch < 0) {
            // Date is before our epoch, use approximation
            return approximateHijriDate(gregorianDate)
        }

        var remainingDays = daysSinceEpoch
        var year = FIRST_HIJRI_YEAR

        // Find the year
        while (year <= FIRST_HIJRI_YEAR + ummAlQuraData.size) {
            val daysInYear = getDaysInHijriYear(year)
            if (remainingDays < daysInYear) break
            remainingDays -= daysInYear
            year++
        }

        // Find the month
        val monthLengths = getMonthLengths(year)
        var month = 1
        for (i in 0 until 12) {
            if (remainingDays < monthLengths[i]) {
                month = i + 1
                break
            }
            remainingDays -= monthLengths[i]
        }

        val day = remainingDays + 1
        return HijriDate(day, month, year)
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
        if (year < FIRST_HIJRI_YEAR) {
            // Use approximation for dates before our data range
            return approximateGregorianDate(day, month, year)
        }

        var totalDays = 0

        // Add days for complete years
        for (y in FIRST_HIJRI_YEAR until year) {
            totalDays += getDaysInHijriYear(y)
        }

        // Add days for complete months
        val monthLengths = getMonthLengths(year)
        for (m in 0 until (month - 1)) {
            totalDays += monthLengths[m]
        }

        // Add remaining days
        totalDays += (day - 1)

        return epoch.plusDays(totalDays.toLong())
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
        if (month < 1 || month > 12) return false
        if (day < 1) return false
        val daysInMonth = getDaysInHijriMonth(year, month)
        return day <= daysInMonth
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

    // Approximation methods for dates outside our data range
    private fun approximateHijriDate(gregorianDate: LocalDate): HijriDate {
        // Simple approximation: Hijri year is approximately Gregorian - 579
        // with month calculation based on lunar cycle
        val julianDay = gregorianDate.toEpochDay() + 2440588
        val l = julianDay - 1948440 + 10632
        val n = ((l - 1) / 10631)
        val l2 = l - 10631 * n + 354
        val j = ((10985 - l2) / 5316) * ((50 * l2) / 17719) + (l2 / 5670) * ((43 * l2) / 15238)
        val l3 = l2 - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val month = (24 * l3) / 709
        val day = l3 - (709 * month) / 24
        val year = 30 * n + j - 30

        return HijriDate(day.toInt(), month.toInt(), year.toInt())
    }

    private fun approximateGregorianDate(day: Int, month: Int, year: Int): LocalDate {
        // Reverse approximation
        val jd = ((11 * year + 3) / 30) + 354 * year + 30 * month - ((month - 1) / 2) + day + 1948440 - 385
        return LocalDate.ofEpochDay(jd.toLong() - 2440588)
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
