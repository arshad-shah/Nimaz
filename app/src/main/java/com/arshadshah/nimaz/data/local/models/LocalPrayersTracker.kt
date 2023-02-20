package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "PrayersTracker")
data class LocalPrayersTracker(
	@PrimaryKey
	val date : String = LocalDate.now().toString() ,
	val fajr : Boolean = false ,
	val dhuhr : Boolean = false ,
	val asr : Boolean = false ,
	val maghrib : Boolean = false ,
	val isha : Boolean = false ,
	val progress : Int = 0
							  )
