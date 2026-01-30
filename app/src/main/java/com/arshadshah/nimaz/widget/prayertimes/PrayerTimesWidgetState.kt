package com.arshadshah.nimaz.widget.prayertimes

import kotlinx.serialization.Serializable

@Serializable
sealed interface PrayerTimesWidgetState {

    @Serializable
    data object Loading : PrayerTimesWidgetState

    @Serializable
    data class Success(val data: PrayerTimesData) : PrayerTimesWidgetState

    @Serializable
    data class Error(val message: String?) : PrayerTimesWidgetState
}

@Serializable
data class PrayerTimesData(
    val locationName: String = "",
    val hijriDate: String = "",
    val nextPrayerName: String = "",
    val timeUntilNext: String = "",
    val fajrTime: String = "",
    val dhuhrTime: String = "",
    val asrTime: String = "",
    val maghribTime: String = "",
    val ishaTime: String = "",
    val fajrPassed: Boolean = false,
    val dhuhrPassed: Boolean = false,
    val asrPassed: Boolean = false,
    val maghribPassed: Boolean = false,
    val ishaPassed: Boolean = false,
    val nextPrayerEpochMillis: Long = 0L
)
