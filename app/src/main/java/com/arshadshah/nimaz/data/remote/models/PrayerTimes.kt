package com.arshadshah.nimaz.data.remote.models

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class PrayerTimes(
	@Serializable(with = LocalDateSerializer::class)
	var date : LocalDate? ,
	@Serializable(with = LocalDateTimeSerializer::class)
	var fajr : LocalDateTime? ,
	@Serializable(with = LocalDateTimeSerializer::class)
	var sunrise : LocalDateTime? ,
	@Serializable(with = LocalDateTimeSerializer::class)
	var dhuhr : LocalDateTime? ,
	@Serializable(with = LocalDateTimeSerializer::class)
	var asr : LocalDateTime? ,
	@Serializable(with = LocalDateTimeSerializer::class)
	var maghrib : LocalDateTime? ,
	@Serializable(with = LocalDateTimeSerializer::class)
	var isha : LocalDateTime? ,
					  )
