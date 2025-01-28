package com.arshadshah.nimaz.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.WorkerFactory
import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.repositories.PrayerTrackerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@EntryPoint
interface WidgetEntryPoint {
    fun prayerTimesRepository(): PrayerTimesRepository
    fun prayerTrackerRepository(): PrayerTrackerRepository
}

fun getPrayerTrackerRepository(appContext: Context): PrayerTrackerRepository {
    val entryPoint = EntryPointAccessors.fromApplication(
        appContext,
        WidgetEntryPoint::class.java
    )
    return entryPoint.prayerTrackerRepository()
}

fun getGlanceAppWidgetManager(context: Context): GlanceAppWidgetManager {
    return GlanceAppWidgetManager(context)
}

@Module
@InstallIn(SingletonComponent::class)
object WorkerBindingModule {
    @Provides
    fun providesWorkerFactory(
        workerFactory: HiltWorkerFactory
    ): WorkerFactory = workerFactory
}