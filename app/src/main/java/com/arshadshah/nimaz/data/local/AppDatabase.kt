package com.arshadshah.nimaz.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arshadshah.nimaz.data.local.dao.*
import com.arshadshah.nimaz.data.local.models.*

@TypeConverters(
		LocalPrayertimeConverter::class ,
		TimestampTypeConvertor::class ,
		TypeConvertorForListOfDuas::class
			   )
@Database(
		entities = [LocalAya::class , LocalJuz::class , LocalSurah::class , LocalPrayerTimes::class , LocalDua::class , LocalChapter::class] ,
		version = 2 ,
		exportSchema = false
		 )
abstract class AppDatabase : RoomDatabase()
{

	abstract val ayaDao : AyaDao
	abstract val juz : JuzDao
	abstract val surah : SurahDao
	abstract val prayerTimes : PrayerTimesDao
	abstract val dua : DuaDao

	//migration from version 1 to 2
	class Migration1To2 : Migration(1 , 2)
	{
		override fun migrate(database: SupportSQLiteDatabase)
		{
			database.execSQL("ALTER TABLE Aya ADD COLUMN suraNumber INTEGER NOT NULL DEFAULT 0")
			database.execSQL("ALTER TABLE Aya ADD COLUMN ayaNumberInSurah INTEGER NOT NULL DEFAULT 0")
			database.execSQL("ALTER TABLE Aya ADD COLUMN bookmark INTEGER NOT NULL DEFAULT 0")
			database.execSQL("ALTER TABLE Aya ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
			database.execSQL("ALTER TABLE Aya ADD COLUMN note TEXT NOT NULL DEFAULT ''")
			database.execSQL("ALTER TABLE Aya ADD COLUMN audioFileLocation TEXT NOT NULL DEFAULT ''")
			database.execSQL("ALTER TABLE Aya ADD COLUMN sajda INTEGER NOT NULL DEFAULT 0")
			database.execSQL("ALTER TABLE Aya ADD COLUMN sajdaType TEXT NOT NULL DEFAULT ''")
			database.execSQL("ALTER TABLE Aya ADD COLUMN ruku INTEGER NOT NULL DEFAULT 0")
			database.execSQL("ALTER TABLE Aya ADD COLUMN juzNumber INTEGER NOT NULL DEFAULT 0")
		}
	}
}