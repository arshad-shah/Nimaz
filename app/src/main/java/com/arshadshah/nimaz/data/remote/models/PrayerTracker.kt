package com.arshadshah.nimaz.data.remote.models

import java.time.LocalDate

data class PrayerTracker(
	val date : String = LocalDate.now().toString() ,
	val fajr : Boolean = false ,
	val dhuhr : Boolean = false ,
	val asr : Boolean = false ,
	val maghrib : Boolean = false ,
	val isha : Boolean = false ,
						)
