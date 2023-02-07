package com.arshadshah.nimaz.utils.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChaptersResponse(
	@SerialName("_id")
	val id : Int ,
	@SerialName("arabic_title")
	val arabicTitle : String ,
	@SerialName("english_title")
	val englishTitle : String ,
	@SerialName("duas")
	val duas : List<DuaResponse> ,
						   )
