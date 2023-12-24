package com.arshadshah.nimaz.widgets.prayertimestrackerthin

import com.arshadshah.nimaz.data.remote.models.PrayerTrackerWithTime
import kotlinx.serialization.Serializable

@Serializable
sealed interface PrayerTimesTrackerWidget {

    @Serializable
    object Loading : PrayerTimesTrackerWidget

    @Serializable
    data class Success(val data: PrayerTrackerWithTime) : PrayerTimesTrackerWidget

    @Serializable
    data class Error(val message: String?) : PrayerTimesTrackerWidget
}