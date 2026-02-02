package com.arshadshah.nimaz.core.di

import android.content.Context
import androidx.room.Room
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.arshadshah.nimaz.data.local.database.dao.AsmaUlHusnaDao
import com.arshadshah.nimaz.data.local.database.dao.AsmaUnNabiDao
import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.data.local.database.dao.IslamicEventDao
import com.arshadshah.nimaz.data.local.database.dao.KhatamDao
import com.arshadshah.nimaz.data.local.database.dao.LocationDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.dao.ProphetDao
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.dao.TafseerDao
import com.arshadshah.nimaz.data.local.database.dao.TasbihDao
import com.arshadshah.nimaz.data.local.database.dao.ZakatDao
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
    fun provideNimazDatabase(
        @ApplicationContext context: Context
    ): NimazDatabase {
        return Room.databaseBuilder(
            context,
            NimazDatabase::class.java,
            NimazDatabase.DATABASE_NAME
        )
            .createFromAsset("database/nimaz_prepopulated.db")
            .addMigrations(NimazDatabase.MIGRATION_7_8, NimazDatabase.MIGRATION_8_9)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideQuranDao(database: NimazDatabase): QuranDao = database.quranDao()

    @Provides
    @Singleton
    fun provideHadithDao(database: NimazDatabase): HadithDao = database.hadithDao()

    @Provides
    @Singleton
    fun provideDuaDao(database: NimazDatabase): DuaDao = database.duaDao()

    @Provides
    @Singleton
    fun providePrayerDao(database: NimazDatabase): PrayerDao = database.prayerDao()

    @Provides
    @Singleton
    fun provideFastingDao(database: NimazDatabase): FastingDao = database.fastingDao()

    @Provides
    @Singleton
    fun provideTasbihDao(database: NimazDatabase): TasbihDao = database.tasbihDao()

    @Provides
    @Singleton
    fun provideLocationDao(database: NimazDatabase): LocationDao = database.locationDao()

    @Provides
    @Singleton
    fun provideIslamicEventDao(database: NimazDatabase): IslamicEventDao = database.islamicEventDao()

    @Provides
    @Singleton
    fun provideZakatDao(database: NimazDatabase): ZakatDao = database.zakatDao()

    @Provides
    @Singleton
    fun provideTafseerDao(database: NimazDatabase): TafseerDao = database.tafseerDao()

    @Provides
    @Singleton
    fun provideKhatamDao(database: NimazDatabase): KhatamDao = database.khatamDao()

    @Provides
    @Singleton
    fun provideAsmaUlHusnaDao(database: NimazDatabase): AsmaUlHusnaDao = database.asmaUlHusnaDao()

    @Provides
    @Singleton
    fun provideAsmaUnNabiDao(database: NimazDatabase): AsmaUnNabiDao = database.asmaUnNabiDao()

    @Provides
    @Singleton
    fun provideProphetDao(database: NimazDatabase): ProphetDao = database.prophetDao()
}
