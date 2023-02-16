package com.arshadshah.nimaz.data.remote.models

data class Aya(
	val ayaNumberInQuran : Int ,
	var ayaNumber : Int ,
	var ayaArabic : String ,
	var ayaTranslation : String ,
	var suraNumber : Int ,
	var ayaNumberInSurah : Int ,
	var bookmark : Boolean ,
	var favorite : Boolean ,
	var note : String ,
	var audioFileLocation : String ,
	var sajda : Boolean ,
	var sajdaType : String ,
	var ruku : Int ,
	var juzNumber : Int ,
	val TranslationLanguage : String ,
			  )