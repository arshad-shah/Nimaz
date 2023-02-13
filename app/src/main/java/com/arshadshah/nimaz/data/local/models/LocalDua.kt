package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Dua")
data class LocalDua(
	@PrimaryKey
	val _id : Int ,
	val chapter_id : Int ,
	val favourite : Int ,
	val arabic_dua : String ,
	val english_translation : String ,
	val english_reference : String ,
				   )
