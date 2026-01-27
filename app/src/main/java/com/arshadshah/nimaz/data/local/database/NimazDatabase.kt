package com.arshadshah.nimaz.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.data.local.database.dao.IslamicEventDao
import com.arshadshah.nimaz.data.local.database.dao.LocationDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.dao.TasbihDao
import com.arshadshah.nimaz.data.local.database.entity.AyahEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaCategoryEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.FastRecordEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithBookEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithEntity
import com.arshadshah.nimaz.data.local.database.entity.IslamicEventEntity
import com.arshadshah.nimaz.data.local.database.entity.LocationEntity
import com.arshadshah.nimaz.data.local.database.entity.MakeupFastEntity
import com.arshadshah.nimaz.data.local.database.entity.PrayerRecordEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.ReadingProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihPresetEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihSessionEntity
import com.arshadshah.nimaz.data.local.database.entity.TranslationEntity

@Database(
    entities = [
        // Quran
        SurahEntity::class,
        AyahEntity::class,
        TranslationEntity::class,
        QuranBookmarkEntity::class,
        ReadingProgressEntity::class,
        // Hadith
        HadithBookEntity::class,
        HadithEntity::class,
        HadithBookmarkEntity::class,
        // Dua
        DuaCategoryEntity::class,
        DuaEntity::class,
        DuaBookmarkEntity::class,
        DuaProgressEntity::class,
        // Prayer & Fasting
        PrayerRecordEntity::class,
        FastRecordEntity::class,
        MakeupFastEntity::class,
        // Tasbih
        TasbihPresetEntity::class,
        TasbihSessionEntity::class,
        // Other
        LocationEntity::class,
        IslamicEventEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class NimazDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao
    abstract fun hadithDao(): HadithDao
    abstract fun duaDao(): DuaDao
    abstract fun prayerDao(): PrayerDao
    abstract fun fastingDao(): FastingDao
    abstract fun tasbihDao(): TasbihDao
    abstract fun locationDao(): LocationDao
    abstract fun islamicEventDao(): IslamicEventDao

    companion object {
        const val DATABASE_NAME = "nimaz_database"
    }
}
