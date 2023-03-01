package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "Tasbih")
data class LocalTasbih(
	@PrimaryKey
	val id : Int = 0,
	val date : String = LocalDate.now().toString() ,
	val arabicName : String ,
	val englishName : String ,
	val translationName : String ,
	val goal : Int = 0,
	val completed : Int = 0,
	val isCompleted : Boolean = false,
					  )
