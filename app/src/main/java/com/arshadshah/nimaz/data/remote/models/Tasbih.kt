package com.arshadshah.nimaz.data.remote.models

import java.time.LocalDate

data class Tasbih(
	val id : Int = 0 ,
	val date : String = LocalDate.now().toString() ,
	val arabicName : String ,
	val englishName : String ,
	val translationName : String ,
	val goal : Int = 0 ,
	val completed : Int = 0 ,
	val isCompleted : Boolean = false ,
				 )
