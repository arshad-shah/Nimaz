package com.arshadshah.nimaz.ui.models

import java.time.LocalDateTime

class PrayerTimes(
    val fajr: LocalDateTime?,
    val sunrise: LocalDateTime?,
    val dhuhr: LocalDateTime?,
    val asr: LocalDateTime?,
    val maghrib: LocalDateTime?,
    val isha: LocalDateTime?,
)