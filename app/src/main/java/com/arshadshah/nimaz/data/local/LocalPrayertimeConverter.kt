package com.arshadshah.nimaz.data.local

import androidx.room.TypeConverter
import com.arshadshah.nimaz.data.local.models.LocalPrayertime

class LocalPrayertimeConverter
{

	@TypeConverter
	fun fromLocalPrayertime(localPrayertime : LocalPrayertime) : String
	{
		return "${localPrayertime.name},${localPrayertime.time}"
	}

	@TypeConverter
	fun toLocalPrayertime(string : String) : LocalPrayertime
	{
		val split = string.split(",")
		return LocalPrayertime(split[0] , split[1])
	}
}