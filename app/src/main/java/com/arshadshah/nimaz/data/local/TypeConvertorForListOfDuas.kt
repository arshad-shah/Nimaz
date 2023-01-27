package com.arshadshah.nimaz.data.local

import androidx.room.TypeConverter
import com.arshadshah.nimaz.data.local.models.LocalDua
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TypeConvertorForListOfDuas
{
	@TypeConverter
	fun fromLocalDuaList(localDuaList: List<LocalDua>): String {
		val gson = Gson()
		return gson.toJson(localDuaList)
	}

	@TypeConverter
	fun toLocalDuaList(localDuaList: String): List<LocalDua> {
		val gson = Gson()
		val type = object : TypeToken<List<LocalDua>>() {}.type
		return gson.fromJson(localDuaList, type)
	}

}