package com.arshadshah.nimaz.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.arshadshah.nimaz.data.local.dao.AyaDao
import com.arshadshah.nimaz.data.local.dao.JuzDao
import com.arshadshah.nimaz.data.local.dao.PrayerTimesDao
import com.arshadshah.nimaz.data.local.dao.SurahDao
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.data.local.models.LocalJuz
import com.arshadshah.nimaz.data.local.models.LocalPrayerTimes
import com.arshadshah.nimaz.data.local.models.LocalSurah

@Database(entities = [LocalAya::class, LocalJuz::class, LocalSurah::class, LocalPrayerTimes::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract val aya: AyaDao
    abstract val juz: JuzDao
    abstract val surah: SurahDao
    abstract val prayerTimes: PrayerTimesDao
}