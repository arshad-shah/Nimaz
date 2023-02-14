package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "PrayersTracker")
data class LocalPrayersTracker(
	@PrimaryKey
	val date : String ,
	val fajr : Boolean ,
	val sunrise : Boolean ,
	val dhuhr : Boolean ,
	val asr : Boolean ,
	val maghrib : Boolean ,
	val isha : Boolean ,
							  )
