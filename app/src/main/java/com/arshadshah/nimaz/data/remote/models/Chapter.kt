package com.arshadshah.nimaz.data.remote.models

data class Chapter(
	val _id : Int ,
	val arabic_title : String ,
	val english_title : String ,
	val duas : ArrayList<Dua> ,
				  )
