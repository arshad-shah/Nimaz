package com.arshadshah.nimaz.modules

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WidgetModule {

    @Provides
    @Singleton
    fun providesGlanceWidgetManager(@ApplicationContext context: Context): GlanceAppWidgetManager =
        GlanceAppWidgetManager(context)
}
