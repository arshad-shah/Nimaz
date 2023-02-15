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
		entities = [LocalAya::class , LocalJuz::class , LocalSurah::class , LocalPrayerTimes::class , LocalDua::class , LocalChapter::class , LocalPrayersTracker::class] ,
		version = 3 ,
		exportSchema = false
		 )
abstract class AppDatabase : RoomDatabase()
{

	abstract val ayaDao : AyaDao
	abstract val juz : JuzDao
	abstract val surah : SurahDao
	abstract val prayerTimes : PrayerTimesDao
	abstract val dua : DuaDao
	abstract val prayersTracker : PrayerTrackerDao

	class Migration1To2 : Migration(1 , 2)
	{

		override fun migrate(database : SupportSQLiteDatabase)
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

	//migration from version 3 to 4
	class Migration2To3 : Migration(2 , 3)
	{

		override fun migrate(database : SupportSQLiteDatabase)
		{
			//1. Create the new table
			database.execSQL("CREATE TABLE IF NOT EXISTS `Dua_new` (`_id` INTEGER NOT NULL, `chapter_id` INTEGER NOT NULL, `favourite` INTEGER NOT NULL, `arabic_dua` TEXT NOT NULL, `english_translation` TEXT NOT NULL, `english_reference` TEXT NOT NULL, PRIMARY KEY(`_id`))")

			//2. Copy the data
			database.execSQL("INSERT INTO Dua_new SELECT _id, chapter_id, favourite, arabic_dua, english_translation, english_reference FROM Dua")

			//3. Remove the old table
			database.execSQL("DROP TABLE Dua")

			//4. Change the table name to the correct one
			database.execSQL("ALTER TABLE Dua_new RENAME TO Dua")
		}
	}
}