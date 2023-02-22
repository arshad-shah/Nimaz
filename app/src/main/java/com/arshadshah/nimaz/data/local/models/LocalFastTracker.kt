package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "FastTracker")
data class LocalFastTracker(
	@PrimaryKey
	val date : String = LocalDate.now().toString() ,
	val isFasting : Boolean = false ,
						   )
