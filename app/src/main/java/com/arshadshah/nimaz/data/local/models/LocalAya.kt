package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Aya")
data class LocalAya(
		//a primary key that is auto generated
	@PrimaryKey(autoGenerate = true)
	val id : Int = 0 ,
	val ayaNumber : Int ,
	val ayaArabic : String ,
	val translation : String ,
	val ayaType : String , //surah or juz
	val numberOfType : Int , //surah number or juz number
	val translationLanguage : String ,
				   )