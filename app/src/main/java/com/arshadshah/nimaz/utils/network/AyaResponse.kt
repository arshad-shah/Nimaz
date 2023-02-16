package com.arshadshah.nimaz.utils.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AyaResponse(
	@SerialName("ayaNumberInQuran")
	val ayaNumberInQuran : Int ,
	@SerialName("ayaNumber")
	val number : Int ,
	@SerialName("ayaArabic")
	val arabic : String ,
	@SerialName("ayaTranslation")
	val translation : String ,
	@SerialName("suraNumber")
	val surahNumber : Int ,
	@SerialName("ayaNumberInSurah")
	val ayaNumberInSurah : Int ,
	@SerialName("bookmark")
	val bookmark : Boolean ,
	@SerialName("favorite")
	val favorite : Boolean ,
	@SerialName("note")
	val note : String ,
	@SerialName("audioFileLocation")
	val audioFileLocation : String ,
	@SerialName("sajda")
	val sajda : Boolean ,
	@SerialName("sajdaType")
	val sajdaType : String ,
	@SerialName("ruku")
	val ruku : Int ,
	@SerialName("juzNumber")
	val juzNumber : Int ,
					  )
