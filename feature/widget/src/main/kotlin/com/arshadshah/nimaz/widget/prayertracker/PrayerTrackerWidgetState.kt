package com.arshadshah.nimaz.widget.prayertracker

import kotlinx.serialization.Serializable

@Serializable
sealed interface PrayerTrackerWidgetState {

    /**
     * Whether this state is a reading worth keeping on screen when a refresh fails — see
     * `refreshWidget`. The date label is the test rather than the count: a day with nothing
     * prayed yet is a real reading, and only a completed load fills the label in.
     */
    val hasData: Boolean get() = false

    @Serializable
    data object Loading : PrayerTrackerWidgetState

    @Serializable
    data class Success(val data: PrayerTrackerData) : PrayerTrackerWidgetState {
        override val hasData: Boolean get() = data.dateLabel.isNotEmpty()
    }

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
