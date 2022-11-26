package com.arshadshah.nimaz.utils.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class PrayerTimeResponse(
	@SerialName("fajr")
	var fajr : String ,
	@SerialName("sunrise")
	val sunrise : String ,
	@SerialName("dhuhr")
	val dhuhr : String ,
	@SerialName("asr")
	val asr : String ,
	@SerialName("maghrib")
	val maghrib : String ,
	@SerialName("isha")
	val isha : String ,
	@SerialName("nextPrayer")
	val nextPrayer : Prayertime ,
	@SerialName("currentPrayer")
	val currentPrayer : Prayertime ,
							 )
{

	@Serializable
	data class Prayertime(
		@SerialName("name")
		val name : String ,
		@SerialName("time")
		val time : String ,
						 )
}
