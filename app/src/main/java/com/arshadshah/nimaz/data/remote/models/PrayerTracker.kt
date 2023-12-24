package com.arshadshah.nimaz.data.remote.models

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class PrayerTracker(
    @Serializable(with = LocalDateSerializer::class)
    val date: String = LocalDate.now().toString(),
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false,
    val progress: Int = 0,
    val isMenstruating: Boolean = false,
)
