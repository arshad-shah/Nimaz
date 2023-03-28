package com.arshadshah.nimaz.data.remote.models

import java.time.LocalDate
import java.time.LocalDateTime

data class PrayerTimes(
	var date : LocalDate? ,
	var fajr : LocalDateTime? ,
	var sunrise : LocalDateTime? ,
	var dhuhr : LocalDateTime? ,
	var asr : LocalDateTime? ,
	var maghrib : LocalDateTime? ,
	var isha : LocalDateTime? ,
					  )
