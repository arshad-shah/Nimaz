package com.arshadshah.nimaz.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arshadshah.nimaz.constants.AppConstants.DATABASE_VERSION
import com.arshadshah.nimaz.data.local.dao.AyaDao
import com.arshadshah.nimaz.data.local.dao.CategoryDao
import com.arshadshah.nimaz.data.local.dao.DuaDao
import com.arshadshah.nimaz.data.local.dao.FastTrackerDao
import com.arshadshah.nimaz.data.local.dao.JuzDao
import com.arshadshah.nimaz.data.local.dao.PrayerTimesDao
import com.arshadshah.nimaz.data.local.dao.PrayerTrackerDao
import com.arshadshah.nimaz.data.local.dao.SurahDao
import com.arshadshah.nimaz.data.local.dao.TasbihTrackerDao
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.data.local.models.LocalCategory
import com.arshadshah.nimaz.data.local.models.LocalChapter
import com.arshadshah.nimaz.data.local.models.LocalDua
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.data.local.models.LocalJuz
import com.arshadshah.nimaz.data.local.models.LocalPrayerTimes
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.data.local.models.LocalTasbih

@TypeConverters(
		 TimestampTypeConvertor::class ,
		 TypeConvertorForListOfDuas::class
			   )
@Database(
		 entities = [LocalAya::class , LocalJuz::class , LocalSurah::class , LocalPrayerTimes::class , LocalDua::class , LocalChapter::class , LocalPrayersTracker::class , LocalFastTracker::class , LocalTasbih::class, LocalCategory::class] ,
		 version = DATABASE_VERSION ,
		 exportSchema = true,
		 )
abstract class AppDatabase : RoomDatabase()
{

	abstract val ayaDao : AyaDao
	abstract val juz : JuzDao
	abstract val surah : SurahDao
	abstract val prayerTimes : PrayerTimesDao
	abstract val dua : DuaDao
	abstract val prayersTracker : PrayerTrackerDao
	abstract val fastTracker : FastTrackerDao
	abstract val tasbihTracker : TasbihTrackerDao
	abstract val category : CategoryDao

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

	//migration from version 5 to 6
	class Migration5To6 : Migration(5 , 6)
	{

		override fun migrate(database : SupportSQLiteDatabase)
		{
			//rename column translation to translationEnglish
			database.execSQL("ALTER TABLE Aya RENAME COLUMN translation TO translationEnglish")
			//add new column translationUrdu
			database.execSQL("ALTER TABLE Aya ADD COLUMN translationUrdu TEXT NOT NULL DEFAULT ''")

			//drop the column translationLanguage
			//1. Create the new table
			database.execSQL("CREATE TABLE IF NOT EXISTS `Aya_new` (`ayaNumberInQuran` INTEGER NOT NULL,`ayaNumber` INTEGER NOT NULL, `ayaArabic` TEXT NOT NULL, `translationEnglish` TEXT NOT NULL, `translationUrdu` TEXT NOT NULL, `suraNumber` INTEGER NOT NULL, `ayaNumberInSurah` INTEGER NOT NULL, `bookmark` INTEGER NOT NULL, `favorite` INTEGER NOT NULL, `note` TEXT NOT NULL, `audioFileLocation` TEXT NOT NULL, `sajda` INTEGER NOT NULL, `sajdaType` TEXT NOT NULL, `ruku` INTEGER NOT NULL, `juzNumber` INTEGER NOT NULL, PRIMARY KEY(`ayaNumberInQuran`))")
			//2. Copy the data
			database.execSQL("INSERT INTO Aya_new SELECT ayaNumberInQuran, ayaNumber, ayaArabic, translationEnglish, translationUrdu, suraNumber, ayaNumberInSurah, bookmark, favorite, note, audioFileLocation, sajda, sajdaType, ruku, juzNumber FROM Aya")
			//3. Remove the old table
			database.execSQL("DROP TABLE Aya")
			//4. Change the table name to the correct one
			database.execSQL("ALTER TABLE Aya_new RENAME TO Aya")

		}
	}

	//migration from version 6 to 7
	class Migration6To7 : Migration(6 , 7)
	{

		override fun migrate(database : SupportSQLiteDatabase)
		{
			//drop table PrayersTracker and create a new one
			database.execSQL("CREATE TABLE IF NOT EXISTS `PrayersTracker_new` (`date` TEXT NOT NULL, `fajr` INTEGER NOT NULL, `dhuhr` INTEGER NOT NULL, `asr` INTEGER NOT NULL, `maghrib` INTEGER NOT NULL, `isha` INTEGER NOT NULL, progress INTEGER NOT NULL, PRIMARY KEY(`date`))")

			database.execSQL("DROP TABLE IF EXISTS PrayersTracker")
			//rename new table
			database.execSQL("ALTER TABLE PrayersTracker_new RENAME TO PrayersTracker")
		}
	}

	//migration from version 7 to 8
	//a migration to add a new table for the new feature of the app
	class Migration7To8 : Migration(7 , 8)
	{

		override fun migrate(database : SupportSQLiteDatabase)
		{
			//create a new table
//			database.execSQL("CREATE TABLE IF NOT EXISTS `QuranTracker_new` (`date` TEXT NOT NULL, `ayaNumber` INTEGER NOT NULL, `suraNumber` INTEGER NOT NULL, `ayaNumberInSurah` INTEGER NOT NULL, `progress` INTEGER NOT NULL, PRIMARY KEY(`date`))")

			//fasting tracker
			database.execSQL("CREATE TABLE IF NOT EXISTS `FastTracker` (`date` TEXT NOT NULL, `isFasting` INTEGER NOT NULL, PRIMARY KEY(`date`))")
		}
	}

	//migration from version 8 to 9
	//a migration to alter the table prayer_times so that the timestamp column is renamed to date, and 	val nextPrayer : LocalPrayertime = LocalPrayertime("" , "") ,
	//	val currentPrayer : LocalPrayertime = LocalPrayertime("" , "") , are removed
	class Migration8To9 : Migration(8 , 9)
	{

		override fun migrate(database : SupportSQLiteDatabase)
		{
			//rename column timestamp to date
			database.execSQL("ALTER TABLE prayer_times RENAME COLUMN timestamp TO date")
			//drop the column nextPrayer
			//1. Create the new table
			database.execSQL("CREATE TABLE IF NOT EXISTS `prayer_times_new` (`date` TEXT NOT NULL, `fajr` TEXT NOT NULL,`sunrise` TEXT NOT NULL, `dhuhr` TEXT NOT NULL, `asr` TEXT NOT NULL, `maghrib` TEXT NOT NULL, `isha` TEXT NOT NULL, PRIMARY KEY(`date`))")
			//2. Copy the data`dhuhr` TEXT NOT NULL,
			database.execSQL("INSERT INTO prayer_times_new SELECT date, fajr,sunrise, dhuhr, asr, maghrib, isha FROM prayer_times")
			//3. Remove the old table
			database.execSQL("DROP TABLE IF EXISTS prayer_times")
			//4. Change the table name to the correct one
			database.execSQL("ALTER TABLE prayer_times_new RENAME TO prayer_times")
		}
	}

	//migration from version 9 to 10
	//a migration to add a new primary key to the table Tasbih
	class Migration9To10 : Migration(9 , 10)
	{

		override fun migrate(database : SupportSQLiteDatabase)
		{
			//drop the table Tasbih
			database.execSQL("DROP TABLE IF EXISTS Tasbih")
			//create a new table
			database.execSQL("CREATE TABLE IF NOT EXISTS `Tasbih_new` (`id` INTEGER NOT NULL, `date` TEXT NOT NULL, `arabicName` TEXT NOT NULL, `englishName` TEXT NOT NULL, `translationName` TEXT NOT NULL, `goal` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
			//rename new table
			database.execSQL("ALTER TABLE Tasbih_new RENAME TO Tasbih")
		}
	}

	//migration from version 10 to 11
	//remove the column isCompleted from the table Tasbih and completed is renamed to count
	class Migration10To11 : Migration(10 , 11)
	{

		override fun migrate(database : SupportSQLiteDatabase)
		{
			//drop the column isCompleted
			//1. Create the new table
			database.execSQL("CREATE TABLE IF NOT EXISTS `Tasbih_new` (`id` INTEGER NOT NULL, `date` TEXT NOT NULL, `arabicName` TEXT NOT NULL, `englishName` TEXT NOT NULL, `translationName` TEXT NOT NULL, `goal` INTEGER NOT NULL, `count` INTEGER NOT NULL, PRIMARY KEY(`id`))")
			//2. Copy the data
			database.execSQL("INSERT INTO Tasbih_new SELECT id, date, arabicName, englishName, translationName, goal, completed FROM Tasbih")
			//3. Remove the old table
			database.execSQL("DROP TABLE Tasbih")
			//4. Change the table name to the correct one
			database.execSQL("ALTER TABLE Tasbih_new RENAME TO Tasbih")
		}
	}

	//migration from version 11 to 12
	//add a new column to the table PrayersTracker and FastTracker
	//called isMenstruating
	class Migration11To12 : Migration(11 , 12)
	{

		override fun migrate(database : SupportSQLiteDatabase)
		{
			//add a new column to the table PrayersTracker
			database.execSQL("ALTER TABLE PrayersTracker ADD COLUMN isMenstruating INTEGER NOT NULL DEFAULT 0")
			//add a new column to the table FastTracker
			database.execSQL("ALTER TABLE FastTracker ADD COLUMN isMenstruating INTEGER NOT NULL DEFAULT 0")
		}
	}

	//migration from version 12 to 13
	//add a column in table Chapter called category
	//add a column in table Dua called category, isFavorite
	class Migration12To13 : Migration(12 , 13)
	{

		override fun migrate(database : SupportSQLiteDatabase)
		{
			database.execSQL("ALTER TABLE Chapter RENAME TO Chapter_old")
			//create a new table
			database.execSQL("CREATE TABLE IF NOT EXISTS `Chapter` (`id` INTEGER NOT NULL, `arabicName` TEXT NOT NULL, `englishName` TEXT NOT NULL, `translationName` TEXT NOT NULL, `category` TEXT NOT NULL, PRIMARY KEY(`id`))")
			//copy the data
			database.execSQL("INSERT INTO Chapter SELECT id, arabicName, englishName, translationName, '' FROM Chapter_old")
			//remove the old table
			database.execSQL("DROP TABLE Chapter_old")

			//add a new column to the table Dua
			database.execSQL("ALTER TABLE Dua ADD COLUMN category TEXT NOT NULL DEFAULT ''")
			//add a new column to the table Dua
			database.execSQL("ALTER TABLE Dua ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")

		}
	}
}