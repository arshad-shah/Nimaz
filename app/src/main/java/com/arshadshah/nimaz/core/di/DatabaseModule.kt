package com.arshadshah.nimaz.core.di

import android.content.Context
import androidx.room.Room
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.arshadshah.nimaz.data.local.user.CustomPresetDao
import com.arshadshah.nimaz.data.local.user.ReadingProgressDao
import com.arshadshah.nimaz.data.local.user.TafseerUserDao
import com.arshadshah.nimaz.data.local.user.TasbihSessionDao
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.NimazUserDatabase
import com.arshadshah.nimaz.data.local.user.ProgressDao
import com.arshadshah.nimaz.data.local.database.dao.AsmaUlHusnaDao
import com.arshadshah.nimaz.data.local.database.dao.AsmaUnNabiDao
import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.data.local.database.dao.HelpDao
import com.arshadshah.nimaz.data.local.database.dao.IslamicEventDao
import com.arshadshah.nimaz.data.local.database.dao.KhatamDao
import com.arshadshah.nimaz.data.local.database.dao.LocationDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.dao.ProphetDao
import com.arshadshah.nimaz.data.local.database.dao.QaidaDao
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
            .createFromAsset("database/nimaz_prepopulated.db", NimazDatabase.PREPACKAGED_CALLBACK)
            // Single source of truth — see NimazDatabase.ALL_MIGRATIONS. Listing them here
            // by hand is what let the chain test drift out of sync with production.
            .addMigrations(*NimazDatabase.ALL_MIGRATIONS)
            .build()
    }

    /**
     * The user's own database, created by Room on the device.
     *
     * Separate from the content database on purpose: content arrives as a 128 MB asset and
     * is replaced wholesale by every release, and until now it also *held* the user's data.
     * The only thing that stopped a content release from taking someone's bookmarks with it
     * was that `createFromAsset` happens not to re-copy on upgrade. Two files makes that
     * structural.
     *
     * The first open copies anything an existing install already has out of the content
     * database — see [LegacyUserDataImport]. The old rows are left where they are, so a bug
     * in the copy is survivable.
     */
    @Provides
    @Singleton
    fun provideNimazUserDatabase(
        @ApplicationContext context: Context
    ): NimazUserDatabase {
        // No `addCallback`. Writing in `onOpen` looked right and failed on a device with
        // "no such table: room_table_modification_log" on *every* launch: Room's invalidation
        // tracker is set up after the callback returns, so an INSERT there fires triggers
        // against a log table that does not exist yet. The copy runs from AppInitializer
        // instead, once Room has finished opening. See [LegacyUserDataImport].
        return Room.databaseBuilder(
            context,
            NimazUserDatabase::class.java,
            NimazUserDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideBookmarkDao(database: NimazUserDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    @Singleton
    fun provideProgressDao(database: NimazUserDatabase): ProgressDao = database.progressDao()

    @Provides
    @Singleton
    fun provideReadingProgressDao(database: NimazUserDatabase): ReadingProgressDao =
        database.readingProgressDao()

    @Provides
    @Singleton
    fun provideCustomPresetDao(database: NimazUserDatabase): CustomPresetDao =
        database.customPresetDao()

    @Provides
    @Singleton
    fun provideTasbihSessionDao(database: NimazUserDatabase): TasbihSessionDao =
        database.tasbihSessionDao()

    @Provides
    @Singleton
    fun provideTafseerUserDao(database: NimazUserDatabase): TafseerUserDao =
        database.tafseerUserDao()

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
    fun providePrayerDao(database: NimazUserDatabase): PrayerDao = database.prayerDao()

    @Provides
    @Singleton
    fun provideFastingDao(database: NimazUserDatabase): FastingDao = database.fastingDao()

    @Provides
    @Singleton
    fun provideTasbihDao(database: NimazDatabase): TasbihDao = database.tasbihDao()

    @Provides
    @Singleton
    fun provideLocationDao(database: NimazUserDatabase): LocationDao = database.locationDao()

    @Provides
    @Singleton
    fun provideIslamicEventDao(database: NimazDatabase): IslamicEventDao =
        database.islamicEventDao()

    @Provides
    @Singleton
    fun provideZakatDao(database: NimazUserDatabase): ZakatDao = database.zakatDao()

    @Provides
    @Singleton
    fun provideTafseerDao(database: NimazDatabase): TafseerDao = database.tafseerDao()

    @Provides
    @Singleton
    fun provideKhatamDao(database: NimazUserDatabase): KhatamDao = database.khatamDao()

    @Provides
    @Singleton
    fun provideAsmaUlHusnaDao(database: NimazDatabase): AsmaUlHusnaDao = database.asmaUlHusnaDao()

    @Provides
    @Singleton
    fun provideAsmaUnNabiDao(database: NimazDatabase): AsmaUnNabiDao = database.asmaUnNabiDao()

    @Provides
    @Singleton
    fun provideProphetDao(database: NimazDatabase): ProphetDao = database.prophetDao()

    @Provides
    @Singleton
    fun provideHelpDao(database: NimazDatabase): HelpDao = database.helpDao()

    @Provides
    @Singleton
    fun provideQaidaDao(database: NimazDatabase): QaidaDao = database.qaidaDao()
}
