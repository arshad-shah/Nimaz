package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Surah")
data class LocalSurah(
	@PrimaryKey
	val number : Int ,
	val numberOfAyahs : Int ,
	val startAya : Int ,
	val name : String ,
	val englishName : String ,
	val englishNameTranslation : String ,
	val revelationType : String ,
	val revelationOrder : Int ,
	val rukus : Int ,
					 )