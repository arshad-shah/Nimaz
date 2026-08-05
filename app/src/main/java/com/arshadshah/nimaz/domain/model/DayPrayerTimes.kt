package com.arshadshah.nimaz.domain.model

import kotlin.time.Instant
import java.time.LocalDate

/** One day's prayer instants — a fact about the day, not about the table that renders it. */
/**
 * One row of the month table. Times are **instants**; the clock format is applied at the leaf from
 * `LocalUse24HourFormat`, so flipping the 12/24-hour toggle is a recomposition rather than a full
 * month recompute.
 */
data class DayPrayerTimes(
    val date: LocalDate,
    val fajr: kotlin.time.Instant?,
    val sunrise: kotlin.time.Instant?,
    val dhuhr: kotlin.time.Instant?,
    val asr: kotlin.time.Instant?,
    val maghrib: kotlin.time.Instant?,
    val isha: kotlin.time.Instant?,
    /** Fasting length (Fajr → Maghrib) in minutes; null if unavailable. */
    val fastMinutes: Int? = null
)
