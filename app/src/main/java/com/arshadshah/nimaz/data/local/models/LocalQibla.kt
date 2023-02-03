package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "Qibla")
data class LocalQibla(
	@PrimaryKey(autoGenerate = true)
	val _id : Int ,
	val timestamp : Long,
	val direction : Double
					 )
