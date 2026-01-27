package com.arshadshah.nimaz.core.di

import android.content.Context
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): PreferencesDataStore {
        return PreferencesDataStore(context)
    }
}
