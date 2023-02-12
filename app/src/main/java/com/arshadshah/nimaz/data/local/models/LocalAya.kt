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
	val suraNumber : Int ,
	val ayaNumberInSurah : Int ,
	val bookmark : Boolean ,
	val favorite : Boolean ,
	val note : String ,
	val audioFileLocation : String ,
	val sajda : Boolean ,
	val sajdaType : String ,
	val ruku : Int ,
	val juzNumber : Int ,
	val ayaType : String , //surah or juz
	val numberOfType : Int , //surah number or juz number
	val translationLanguage : String ,
				   )