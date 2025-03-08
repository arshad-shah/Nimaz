package com.arshadshah.nimaz.modules

import android.content.Context
import com.arshadshah.nimaz.BuildConfig
import com.arshadshah.nimaz.utils.FirebaseLogger
import com.arshadshah.nimaz.utils.NotificationHelper
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.ShowcaseDataStore
import com.arshadshah.nimaz.utils.ThemeDataStore
import com.arshadshah.nimaz.utils.alarms.Alarms
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

// File: di/AppModule.kt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCoroutineDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }

    @Provides
    @Singleton
    fun provideCoroutineScope(
        dispatcher: CoroutineDispatcher
    ): CoroutineScope {
        return CoroutineScope(dispatcher + SupervisorJob())
    }


    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideFirebaseLogger(firebaseAnalytics: FirebaseAnalytics): FirebaseLogger {
        val logger = FirebaseLogger(firebaseAnalytics)

        // Configure the logger - enable debug mode in debug builds
        logger.configure(debugMode = BuildConfig.DEBUG)

        // Set some app-wide user properties
        logger.setUserProperty("app_version", BuildConfig.VERSION_NAME)
        logger.setUserProperty("build_type", BuildConfig.BUILD_TYPE)

        return logger
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(): NotificationHelper {
        return NotificationHelper()
    }

    @Provides
    @Singleton
    fun provideAlarms(): Alarms {
        return Alarms()
    }

    @Provides
    @Singleton
    fun provideCreateAlarms(
        notificationHelper: NotificationHelper,
        sharedPreferences: PrivateSharedPreferences,
        alarms: Alarms
    ): CreateAlarms {
        return CreateAlarms(
            notificationHelper,
            sharedPreferences = sharedPreferences,
            alarms = alarms
        )
    }


    @Provides
    @Singleton
    fun provideThemeDataStore(
        @ApplicationContext context: Context
    ): ThemeDataStore = ThemeDataStore(context)

    @Provides
    @Singleton
    fun provideShowcaseDataStore(
        @ApplicationContext context: Context
    ): ShowcaseDataStore = ShowcaseDataStore(context)
}