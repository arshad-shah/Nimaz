package com.arshadshah.nimaz.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.arshadshah.nimaz.constants.AppConstants.DATABASE_VERSION
import com.arshadshah.nimaz.data.local.dao.AyaDao
import com.arshadshah.nimaz.data.local.dao.CategoryDao
import com.arshadshah.nimaz.data.local.dao.DuaDao
import com.arshadshah.nimaz.data.local.dao.FastTrackerDao
import com.arshadshah.nimaz.data.local.dao.HadithDao
import com.arshadshah.nimaz.data.local.dao.JuzDao
import com.arshadshah.nimaz.data.local.dao.KhatamProgressDao
import com.arshadshah.nimaz.data.local.dao.KhatamSessionDao
import com.arshadshah.nimaz.data.local.dao.PrayerTimesDao
import com.arshadshah.nimaz.data.local.dao.PrayerTrackerDao
import com.arshadshah.nimaz.data.local.dao.ReadingProgressDao
import com.arshadshah.nimaz.data.local.dao.SurahDao
import com.arshadshah.nimaz.data.local.dao.TafsirDao
import com.arshadshah.nimaz.data.local.dao.TafsirEditionDao
import com.arshadshah.nimaz.data.local.dao.TasbihTrackerDao
import com.arshadshah.nimaz.data.local.models.HadithChapter
import com.arshadshah.nimaz.data.local.models.HadithEntity
import com.arshadshah.nimaz.data.local.models.HadithMetadata
import com.arshadshah.nimaz.data.local.models.KhatamProgress
import com.arshadshah.nimaz.data.local.models.KhatamSession
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
import com.arshadshah.nimaz.data.local.models.ReadingProgress
import com.arshadshah.nimaz.data.local.models.Tafsir
import com.arshadshah.nimaz.data.local.models.TafsirEdition

@Database(
    entities = [
        LocalAya::class,
        LocalJuz::class,
        LocalSurah::class,
        LocalPrayerTimes::class,
        LocalDua::class,
        LocalChapter::class,
        LocalPrayersTracker::class,
        LocalFastTracker::class,
        LocalTasbih::class,
        LocalCategory::class,
        HadithEntity::class,
        HadithMetadata::class,
        HadithChapter::class,
        Tafsir::class,
        TafsirEdition::class,
        ReadingProgress::class,
        KhatamSession::class,
        KhatamProgress::class
    ],
    version = DATABASE_VERSION,
    exportSchema = true,
)
@TypeConverters(
    TimestampTypeConvertor::class,
    TypeConvertorForListOfDuas::class,
    LocalDateTimestampTypeConvertor::class,
)
abstract class AppDatabase : RoomDatabase() {
    // Quran related DAOs
    abstract val ayaDao: AyaDao
    abstract val juzDao: JuzDao
    abstract val surahDao: SurahDao

    // Prayer related DAOs
    abstract val prayerTimesDao: PrayerTimesDao
    abstract val prayerTrackerDao: PrayerTrackerDao
    abstract val fastTrackerDao: FastTrackerDao

    // Dua related DAOs
    abstract val duaDao: DuaDao
    abstract val categoryDao: CategoryDao

    // Tasbih related DAO
    abstract val tasbihTrackerDao: TasbihTrackerDao

    // Hadith related DAO
    abstract val hadithDao: HadithDao

    // Tafsir related DAOs
    abstract val tafsirDao: TafsirDao
    abstract val tafsirEditionDao: TafsirEditionDao

    abstract val readingProgressDao: ReadingProgressDao

    abstract val khatamProgressDao: KhatamProgressDao
    abstract val khatamSessionDao: KhatamSessionDao

}