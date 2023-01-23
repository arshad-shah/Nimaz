package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Aya")
data class LocalAya(
	@PrimaryKey
	val ayaNumber : Int ,
	val ayaArabic : String ,
	val translation : String ,
	val ayaType : String , //surah or juz
	val numberOfType : Int , //surah number or juz number
				   )