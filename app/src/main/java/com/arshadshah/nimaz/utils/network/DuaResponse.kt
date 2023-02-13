package com.arshadshah.nimaz.utils.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DuaResponse(
	@SerialName("_id")
	val id : Int ,
	@SerialName("chapter_id")
	val chapterId : Int ,
	@SerialName("favourite")
	val favourite : Int ,
	@SerialName("arabic_dua")
	val arabicDua : String ,
	@SerialName("english_translation")
	val englishTranslation : String ,
	@SerialName("english_reference")
	val englishReference : String ,
					  )
