package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Juz")
data class LocalJuz(
	@PrimaryKey
	val number : Int ,
	val name : String ,
	val tname : String ,
	val juzStartAyaInQuran : Int ,
				   )