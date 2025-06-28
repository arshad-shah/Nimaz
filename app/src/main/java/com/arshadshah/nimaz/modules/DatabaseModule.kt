package com.arshadshah.nimaz.modules

import android.content.Context
import androidx.room.Room
import com.arshadshah.nimaz.data.local.AppDatabase
import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.local.dao.AyaDao
import com.arshadshah.nimaz.data.local.dao.CategoryDao
import com.arshadshah.nimaz.data.local.dao.DuaDao
import com.arshadshah.nimaz.data.local.dao.FastTrackerDao
import com.arshadshah.nimaz.data.local.dao.HadithDao
import com.arshadshah.nimaz.data.local.dao.JuzDao
import com.arshadshah.nimaz.data.local.dao.PrayerTimesDao
import com.arshadshah.nimaz.data.local.dao.PrayerTrackerDao
import com.arshadshah.nimaz.data.local.dao.SurahDao
import com.arshadshah.nimaz.data.local.dao.TafsirDao
import com.arshadshah.nimaz.data.local.dao.TafsirEditionDao
import com.arshadshah.nimaz.data.local.dao.TasbihTrackerDao
import com.arshadshah.nimaz.data.local.systems.DuaSystem
import com.arshadshah.nimaz.data.local.systems.HadithSystem
import com.arshadshah.nimaz.data.local.systems.PrayerSystem
import com.arshadshah.nimaz.data.local.systems.QuranSystem
import com.arshadshah.nimaz.data.local.systems.TafsirSystem
import com.arshadshah.nimaz.data.local.systems.TasbihSystem
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "database")
            .createFromAsset("databases/quran_room_compatible.db")
            .fallbackToDestructiveMigration(true)
            .build()
    }

    // DAO Providers
    @Provides
    @Singleton
    fun provideHadithDao(database: AppDatabase) = database.hadithDao

    @Provides
    @Singleton
    fun provideCategoryDao(database: AppDatabase) = database.categoryDao

    @Provides
    @Singleton
    fun provideTasbihTrackerDao(database: AppDatabase) = database.tasbihTrackerDao

    @Provides
    @Singleton
    fun providePrayerTrackerDao(database: AppDatabase) = database.prayerTrackerDao

    @Provides
    @Singleton
    fun providePrayerTimesDao(database: AppDatabase) = database.prayerTimesDao

    @Provides
    @Singleton
    fun provideFastTrackerDao(database: AppDatabase) = database.fastTrackerDao

    @Provides
    @Singleton
    fun provideAyaDao(database: AppDatabase) = database.ayaDao

    @Provides
    @Singleton
    fun provideJuzDao(database: AppDatabase) = database.juzDao

    @Provides
    @Singleton
    fun provideSurahDao(database: AppDatabase) = database.surahDao

    @Provides
    @Singleton
    fun provideDuaDao(database: AppDatabase) = database.duaDao

    @Provides
    @Singleton
    fun provideTafsirDao(database: AppDatabase) = database.tafsirDao

    @Provides
    @Singleton
    fun provideTafsirEditionDao(database: AppDatabase) = database.tafsirEditionDao

    // System Providers
    @Provides
    @Singleton
    fun provideHadithSystem(
        hadithDao: HadithDao,
        categoryDao: CategoryDao
    ) = HadithSystem(hadithDao, categoryDao)

    @Provides
    @Singleton
    fun provideTasbihSystem(
        tasbihTrackerDao: TasbihTrackerDao
    ) = TasbihSystem(tasbihTrackerDao)

    @Provides
    @Singleton
    fun providePrayerSystem(
        prayerTrackerDao: PrayerTrackerDao,
        prayerTimesDao: PrayerTimesDao,
        fastTrackerDao: FastTrackerDao
    ) = PrayerSystem(prayerTrackerDao, prayerTimesDao, fastTrackerDao)

    @Provides
    @Singleton
    fun provideQuranSystem(
        ayaDao: AyaDao,
        juzDao: JuzDao,
        surahDao: SurahDao
    ) = QuranSystem(ayaDao, juzDao, surahDao)

    @Provides
    @Singleton
    fun provideDuaSystem(
        duaDao: DuaDao
    ) = DuaSystem(duaDao)

    @Provides
    @Singleton
    fun provideTafsirSystem(
        tafsirDao: TafsirDao,
        tafsirEditionDao: TafsirEditionDao
    ) = TafsirSystem(tafsirDao, tafsirEditionDao)

    // DataStore Provider
    @Provides
    @Singleton
    fun provideDataStore(
        hadithSystem: HadithSystem,
        tasbihSystem: TasbihSystem,
        prayerSystem: PrayerSystem,
        quranSystem: QuranSystem,
        duaSystem: DuaSystem,
        tafsirSystem: TafsirSystem
    ): DataStore = DataStore(
        hadithSystem,
        tasbihSystem,
        prayerSystem,
        quranSystem,
        duaSystem,
        tafsirSystem
    )
}