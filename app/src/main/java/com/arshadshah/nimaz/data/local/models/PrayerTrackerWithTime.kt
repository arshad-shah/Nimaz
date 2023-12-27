package com.arshadshah.nimaz.data.local.models

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class PrayerTrackerWithTime(

    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate = LocalDate.now(),
    val fajr: Boolean = false,

    @Serializable(with = LocalDateTimeSerializer::class)
    val fajrTime: LocalDateTime = LocalDateTime.now(),
    val dhuhr: Boolean = false,

    @Serializable(with = LocalDateTimeSerializer::class)
    val dhuhrTime: LocalDateTime = LocalDateTime.now(),
    val asr: Boolean = false,

    @Serializable(with = LocalDateTimeSerializer::class)
    val asrTime: LocalDateTime = LocalDateTime.now(),
    val maghrib: Boolean = false,

    @Serializable(with = LocalDateTimeSerializer::class)
    val maghribTime: LocalDateTime = LocalDateTime.now(),
    val isha: Boolean = false,

    @Serializable(with = LocalDateTimeSerializer::class)
    val ishaTime: LocalDateTime = LocalDateTime.now(),
)
