package com.arshadshah.nimaz.widget.nextprayer

import kotlinx.serialization.Serializable

@Serializable
sealed interface NextPrayerWidgetState {

    @Serializable
    data object Loading : NextPrayerWidgetState

    @Serializable
    data class Success(val data: NextPrayerData) : NextPrayerWidgetState

    @Serializable
    data class Error(val message: String?) : NextPrayerWidgetState
}

@Serializable
data class NextPrayerData(
    val prayerName: String = "",
    val prayerTime: String = "",
    val countdown: String = "",
    val isValid: Boolean = true
)
