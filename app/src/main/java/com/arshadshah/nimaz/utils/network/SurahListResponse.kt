package com.arshadshah.nimaz.utils.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class SurahResponse(
	@SerialName("suraNumber")
	val number : Int ,
	@SerialName("ayaAmount")
	val numberOfAyahs : Int ,
	@SerialName("start")
	val startAya : Int ,
	@SerialName("name")
	val name : String ,
	@SerialName("tname")
	val englishName : String ,
	@SerialName("ename")
	val englishNameTranslation : String ,
	@SerialName("type")
	val revelationType : String ,
	@SerialName("orderOfNuzool")
	val revelationOrder : Int ,
	@SerialName("rukus")
	val rukus : Int ,
						)
