package com.arshadshah.nimaz.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

class LocalDateTimestampTypeConvertor {

    @TypeConverter
    fun fromTimestamp(value: LocalDate?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toTimestamp(value: String?): LocalDate? {
        return if (value != null) LocalDate.parse(value) else null
    }
}