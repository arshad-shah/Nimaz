package com.arshadshah.nimaz.widget.prayertimes

import kotlinx.serialization.Serializable

@Serializable
sealed interface PrayerTimesWidgetState {

    /**
     * Whether this state is a reading worth keeping on screen when a refresh fails — see
     * `refreshWidget`. The default state every widget starts on is `Success` with an empty
     * payload, so "is it Success" is not the question; carrying loaded values is.
     */
    val hasData: Boolean get() = false

    @Serializable
    data object Loading : PrayerTimesWidgetState

    @Serializable
    data class Success(val data: PrayerTimesData) : PrayerTimesWidgetState {
        override val hasData: Boolean get() = data.fajrEpochMillis > 0L
    }

    @Serializable
    data class Error(val message: String?) : PrayerTimesWidgetState
}

@Serializable
data class PrayerTimesData(
    val locationName: String = "",
    val hijriDate: String = "",
    val fajrTime: String = "",
    val dhuhrTime: String = "",
    val asrTime: String = "",
    val maghribTime: String = "",
    val ishaTime: String = "",
    // Absolute prayer instants. The "next prayer" highlight and countdown are derived
    // from these live at render time, so they stay correct between refresh-worker runs.
    val fajrEpochMillis: Long = 0L,
    val dhuhrEpochMillis: Long = 0L,
    val asrEpochMillis: Long = 0L,
    val maghribEpochMillis: Long = 0L,
    val ishaEpochMillis: Long = 0L,
)
