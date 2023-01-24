package com.arshadshah.nimaz.data.local

import androidx.room.TypeConverter
import java.time.LocalDateTime

class TimestampTypeConvertor
{

	@TypeConverter
	fun fromTimestamp(value : LocalDateTime?) : String?
	{
		return value?.toString()
	}

	@TypeConverter
	fun toTimestamp(value : String?) : LocalDateTime?
	{
		return if (value != null) LocalDateTime.parse(value) else null
	}
}