package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Category")
data class LocalCategory(
	@PrimaryKey
	val id : Int ,
	val name : String ,
						)