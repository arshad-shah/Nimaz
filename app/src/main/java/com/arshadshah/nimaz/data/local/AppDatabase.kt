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
		version = 5 ,
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

			database.execSQL("CREATE TABLE IF NOT EXISTS `Aya_new` (`ayaNumber` INTEGER NOT NULL, `ayaArabic` TEXT NOT NULL, `translation` TEXT NOT NULL, `suraNumber` INTEGER NOT NULL, `ayaNumberInSurah` INTEGER NOT NULL, `bookmark` INTEGER NOT NULL, `favorite` INTEGER NOT NULL, `note` TEXT NOT NULL, `audioFileLocation` TEXT NOT NULL, `sajda` INTEGER NOT NULL, `sajdaType` TEXT NOT NULL, `ruku` INTEGER NOT NULL, `juzNumber` INTEGER NOT NULL, `translationLanguage` TEXT NOT NULL, PRIMARY KEY(`ayaNumber`))")
			//copy data
			database.execSQL("INSERT INTO Aya_new SELECT ayaNumber, ayaArabic, translation, suraNumber, ayaNumberInSurah, bookmark, favorite, note, audioFileLocation, sajda, sajdaType, ruku, juzNumber, translationLanguage FROM Aya")
			//remove old table
			database.execSQL("DROP TABLE Aya")
			//rename new table
			database.execSQL("ALTER TABLE Aya_new RENAME TO Aya")
		}
	}

	//migration from version 3 to 4
	class Migration3To4 : Migration(3 , 4)
	{

		override fun migrate(database : SupportSQLiteDatabase)
		{
			database.execSQL("CREATE TABLE IF NOT EXISTS `Aya_new` (`ayaNumber` INTEGER NOT NULL, `ayaArabic` TEXT NOT NULL, `translation` TEXT NOT NULL, `suraNumber` INTEGER NOT NULL, `ayaNumberInSurah` INTEGER NOT NULL, `bookmark` INTEGER NOT NULL, `favorite` INTEGER NOT NULL, `note` TEXT NOT NULL, `audioFileLocation` TEXT NOT NULL, `sajda` INTEGER NOT NULL, `sajdaType` TEXT NOT NULL, `ruku` INTEGER NOT NULL, `juzNumber` INTEGER NOT NULL, `translationLanguage` TEXT NOT NULL, PRIMARY KEY(`ayaNumber`))")
			//copy data
			database.execSQL("INSERT INTO Aya_new SELECT ayaNumber, ayaArabic, translation, suraNumber, ayaNumberInSurah, bookmark, favorite, note, audioFileLocation, sajda, sajdaType, ruku, juzNumber, translationLanguage FROM Aya")
			//remove old table
			database.execSQL("DROP TABLE Aya")
			//rename new table
			database.execSQL("ALTER TABLE Aya_new RENAME TO Aya")
		}
	}

	class Migration4To5 : Migration(4 , 5)
	{

		override fun migrate(database : SupportSQLiteDatabase)
		{
			database.execSQL("CREATE TABLE IF NOT EXISTS `Aya_new` (`ayaNumberInQuran` INTEGER NOT NULL,`ayaNumber` INTEGER NOT NULL, `ayaArabic` TEXT NOT NULL, `translation` TEXT NOT NULL, `suraNumber` INTEGER NOT NULL, `ayaNumberInSurah` INTEGER NOT NULL, `bookmark` INTEGER NOT NULL, `favorite` INTEGER NOT NULL, `note` TEXT NOT NULL, `audioFileLocation` TEXT NOT NULL, `sajda` INTEGER NOT NULL, `sajdaType` TEXT NOT NULL, `ruku` INTEGER NOT NULL, `juzNumber` INTEGER NOT NULL, `translationLanguage` TEXT NOT NULL, PRIMARY KEY(`ayaNumberInQuran`))")
			//drop old table
			database.execSQL("DROP TABLE Aya")
			//rename new table
			database.execSQL("ALTER TABLE Aya_new RENAME TO Aya")
		}
	}
}