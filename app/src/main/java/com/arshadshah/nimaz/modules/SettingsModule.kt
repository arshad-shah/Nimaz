package com.arshadshah.nimaz.modules

import android.content.Context
import com.arshadshah.nimaz.repositories.AutoLocationRepository
import com.arshadshah.nimaz.repositories.ManualLocationRepository
import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.repositories.UpdateManager
import com.arshadshah.nimaz.repositories.UpdateRepository
import com.arshadshah.nimaz.services.LocationService
import com.arshadshah.nimaz.services.LocationStateManager
import com.arshadshah.nimaz.services.PrayerTimesService
import com.arshadshah.nimaz.services.UpdateService
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): PrivateSharedPreferences {
        return PrivateSharedPreferences(context)
    }

    @Provides
    @Singleton
    fun provideManualLocationRepository(
        @ApplicationContext context: Context
    ): ManualLocationRepository {
        return ManualLocationRepository(context)
    }

    @Provides
    @Singleton
    fun provideAutoLocationRepository(
        @ApplicationContext context: Context
    ): AutoLocationRepository {
        return AutoLocationRepository(context)
    }

    @Provides
    @Singleton
    fun provideLocationService(
        @ApplicationContext context: Context,
        manualLocationRepository: ManualLocationRepository,
        autoLocationRepository: AutoLocationRepository,
        sharedPreferences: PrivateSharedPreferences,
        locationStateManager: LocationStateManager
    ): LocationService {
        return LocationService(
            context,
            manualLocationRepository,
            autoLocationRepository,
            sharedPreferences,
            locationStateManager
        )
    }

    @Provides
    @Singleton
    fun provideAppUpdateManager(
        @ApplicationContext context: Context
    ): AppUpdateManager = AppUpdateManagerFactory.create(context)

    @Provides
    @Singleton
    fun provideUpdateManager(
        appUpdateManager: AppUpdateManager
    ): UpdateManager = UpdateRepository(appUpdateManager)

    @Provides
    @Singleton
    fun provideUpdateService(
        updateManager: UpdateManager
    ): UpdateService {
        return UpdateService(updateManager)
    }

    @Provides
    @Singleton
    fun providePrayerTimesService(
        @ApplicationContext context: Context,
        prayerTimesRepository: PrayerTimesRepository
    ): PrayerTimesService {
        return PrayerTimesService(context, prayerTimesRepository)
    }
}