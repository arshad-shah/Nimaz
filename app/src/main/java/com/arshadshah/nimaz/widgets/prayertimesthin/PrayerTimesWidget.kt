package com.arshadshah.nimaz.widgets.prayertimesthin

import com.arshadshah.nimaz.data.local.models.LocalPrayerTimes
import kotlinx.serialization.Serializable

@Serializable
sealed interface PrayerTimesWidget {

    @Serializable
    object Loading : PrayerTimesWidget

    @Serializable
    data class Success(val data: LocalPrayerTimes) : PrayerTimesWidget

    @Serializable
    data class Error(val message: String?) : PrayerTimesWidget
}