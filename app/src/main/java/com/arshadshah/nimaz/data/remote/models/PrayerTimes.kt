package com.arshadshah.nimaz.data.remote.models

import java.time.LocalDate
import java.time.LocalDateTime

data class PrayerTimes(
	val date : LocalDate? ,
	val fajr : LocalDateTime? ,
	val sunrise : LocalDateTime? ,
	val dhuhr : LocalDateTime? ,
	val asr : LocalDateTime? ,
	val maghrib : LocalDateTime? ,
	val isha : LocalDateTime? ,
					  )
