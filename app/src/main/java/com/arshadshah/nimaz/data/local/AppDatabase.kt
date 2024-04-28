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
import com.arshadshah.nimaz.data.local.dao.PrayerTimesDao
import com.arshadshah.nimaz.data.local.dao.PrayerTrackerDao
import com.arshadshah.nimaz.data.local.dao.SurahDao
import com.arshadshah.nimaz.data.local.dao.TasbihTrackerDao
import com.arshadshah.nimaz.data.local.models.HadithChapter
import com.arshadshah.nimaz.data.local.models.HadithEntity
import com.arshadshah.nimaz.data.local.models.HadithMetadata
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
    TimestampTypeConvertor::class,
    TypeConvertorForListOfDuas::class,
    LocalDateTimestampTypeConvertor::class
)
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
    ],
    version = DATABASE_VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract val ayaDao: AyaDao
    abstract val juz: JuzDao
    abstract val surah: SurahDao
    abstract val prayerTimes: PrayerTimesDao
    abstract val dua: DuaDao
    abstract val prayersTracker: PrayerTrackerDao
    abstract val fastTracker: FastTrackerDao
    abstract val tasbihTracker: TasbihTrackerDao
    abstract val category: CategoryDao
    abstract val hadith: HadithDao
}