package com.arshadshah.nimaz.widgets.prayertimestrackerthin

import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import kotlinx.serialization.Serializable

@Serializable
sealed interface PrayerTimesTrackerWidget
{

	@Serializable
	object Loading : PrayerTimesTrackerWidget

	@Serializable
	data class Success(val data : PrayerTracker) : PrayerTimesTrackerWidget

	@Serializable
	data class Error(val message : String?) : PrayerTimesTrackerWidget
}