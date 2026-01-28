package com.arshadshah.nimaz.widget.prayertracker

import kotlinx.serialization.Serializable

@Serializable
sealed interface PrayerTrackerWidgetState {

    @Serializable
    data object Loading : PrayerTrackerWidgetState

    @Serializable
    data class Success(val data: PrayerTrackerData) : PrayerTrackerWidgetState

    @Serializable
    data class Error(val message: String?) : PrayerTrackerWidgetState
}

@Serializable
data class PrayerTrackerData(
    val dateLabel: String = "",
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false,
    val prayedCount: Int = 0,
    val totalCount: Int = 5
)
