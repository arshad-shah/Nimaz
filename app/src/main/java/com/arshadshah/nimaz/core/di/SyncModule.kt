package com.arshadshah.nimaz.core.di

import android.content.Context
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.database.dao.KhatamDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.dao.TafseerDao
import com.arshadshah.nimaz.data.local.database.dao.TasbihDao
import com.arshadshah.nimaz.data.local.database.dao.ZakatDao
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.data.sync.NearbyConnectionsManager
import com.arshadshah.nimaz.data.sync.SyncDataExporter
import com.arshadshah.nimaz.data.sync.SyncDataImporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideNearbyConnectionsManager(
        @ApplicationContext context: Context
    ): NearbyConnectionsManager = NearbyConnectionsManager(context)

    @Provides
    @Singleton
    fun provideSyncDataExporter(
        quranDao: QuranDao,
        prayerDao: PrayerDao,
        fastingDao: FastingDao,
        tasbihDao: TasbihDao,
        khatamDao: KhatamDao,
        tafseerDao: TafseerDao,
        zakatDao: ZakatDao,
        preferencesDataStore: PreferencesDataStore
    ): SyncDataExporter = SyncDataExporter(
        quranDao, prayerDao, fastingDao, tasbihDao, khatamDao, tafseerDao, zakatDao, preferencesDataStore
    )

    @Provides
    @Singleton
    fun provideSyncDataImporter(
        database: NimazDatabase,
        quranDao: QuranDao,
        prayerDao: PrayerDao,
        fastingDao: FastingDao,
        tasbihDao: TasbihDao,
        khatamDao: KhatamDao,
        tafseerDao: TafseerDao,
        zakatDao: ZakatDao,
        preferencesDataStore: PreferencesDataStore
    ): SyncDataImporter = SyncDataImporter(
        database, quranDao, prayerDao, fastingDao, tasbihDao, khatamDao, tafseerDao, zakatDao, preferencesDataStore
    )
}
