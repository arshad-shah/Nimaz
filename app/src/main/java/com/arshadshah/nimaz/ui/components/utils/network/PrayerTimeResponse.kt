package com.arshadshah.nimaz.ui.components.utils.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class PrayerTimeResponse(
    @SerialName("fajr")
    var fajr: String,
    @SerialName("sunrise")
    val sunrise: String,
    @SerialName("dhuhr")
    val dhuhr: String,
    @SerialName("asr")
    val asr: String,
    @SerialName("maghrib")
    val maghrib: String,
    @SerialName("isha")
    val isha: String,
)
