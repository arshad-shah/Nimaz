package com.arshadshah.nimaz.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.arshadshah.nimaz.data.local.dao.*
import com.arshadshah.nimaz.data.local.models.*

@TypeConverters(LocalPrayertimeConverter::class , TimestampTypeConvertor::class, TypeConvertorForListOfDuas::class)
@Database(
		entities = [LocalAya::class , LocalJuz::class , LocalSurah::class , LocalPrayerTimes::class, LocalDua::class, LocalChapter::class] ,
		version = 1 ,
		exportSchema = false
		 )
abstract class AppDatabase : RoomDatabase()
{

	abstract val ayaDao : AyaDao
	abstract val juz : JuzDao
	abstract val surah : SurahDao
	abstract val prayerTimes : PrayerTimesDao
	abstract val dua : DuaDao
}