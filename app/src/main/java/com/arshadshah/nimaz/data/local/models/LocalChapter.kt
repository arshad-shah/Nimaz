package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Chapter")
data class LocalChapter(
	@PrimaryKey
	val _id : Int ,
	val arabic_title : String ,
	val english_title : String ,
	val duas : List<LocalDua> ,
					   )
