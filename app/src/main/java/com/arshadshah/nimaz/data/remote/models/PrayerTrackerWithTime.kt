package com.arshadshah.nimaz.data.remote.models

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class PrayerTrackerWithTime(
    @Serializable(with = LocalDateSerializer::class)
    val date: String = LocalDate.now().toString(),
    val fajr: Boolean = false,
    val fajrTime: String = "",
    val dhuhr: Boolean = false,
    val dhuhrTime: String = "",
    val asr: Boolean = false,
    val asrTime: String = "",
    val maghrib: Boolean = false,
    val maghribTime: String = "",
    val isha: Boolean = false,
    val ishaTime: String = "",
    val progress: Int = 0,
)
