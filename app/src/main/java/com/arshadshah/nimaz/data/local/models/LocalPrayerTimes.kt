package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_times")
data class LocalPrayerTimes(
	@PrimaryKey
	val timeStamp : String = " " ,
	val fajr : String? = null ,
	val sunrise : String? = null ,
	val dhuhr : String? = null ,
	val asr : String? = null ,
	val maghrib : String? = null ,
	val isha : String? = null ,
	val nextPrayer : LocalPrayertime = LocalPrayertime("" , "") ,
	val currentPrayer : LocalPrayertime = LocalPrayertime("" , "") ,
						   )