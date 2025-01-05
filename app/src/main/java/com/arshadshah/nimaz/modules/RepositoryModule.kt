package com.arshadshah.nimaz.modules

import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// File: di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePrayerTimesRepository(): PrayerTimesRepository {
        return PrayerTimesRepository
    }
}