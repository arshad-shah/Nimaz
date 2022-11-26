package com.arshadshah.nimaz.data.remote.models

data class Aya(
	val ayaNumber : Int ,
	val ayaArabic : String ,
	val translation : String ,
	val ayaType : String , //surah or juz
	val numberOfType : Int , //surah number or juz number
			  )