package com.arshadshah.nimaz.data.remote.models

import java.time.LocalDateTime

data class PrayerTimes(
	val timestamp : LocalDateTime? = null ,
	val fajr : LocalDateTime? ,
	val sunrise : LocalDateTime? ,
	val dhuhr : LocalDateTime? ,
	val asr : LocalDateTime? ,
	val maghrib : LocalDateTime? ,
	val isha : LocalDateTime? ,
	val nextPrayer : Prayertime? ,
	val currentPrayer : Prayertime? ,
					  )
