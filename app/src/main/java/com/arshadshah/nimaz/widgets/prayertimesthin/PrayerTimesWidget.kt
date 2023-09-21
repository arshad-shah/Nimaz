package com.arshadshah.nimaz.widgets.prayertimesthin

import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import kotlinx.serialization.Serializable

@Serializable
sealed interface PrayerTimesWidget
{

	@Serializable
	object Loading : PrayerTimesWidget

	@Serializable
	data class Success(val data : PrayerTimes) : PrayerTimesWidget

	@Serializable
	data class Error(val message : String?) : PrayerTimesWidget
}