package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.core.common.DefaultDispatcher
import com.arshadshah.nimaz.core.common.IoDispatcher
import com.arshadshah.nimaz.domain.time.SystemTodayProvider
import com.arshadshah.nimaz.domain.time.TodayProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    @Singleton
    fun provideClock(): java.time.Clock = java.time.Clock.systemDefaultZone()

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeBindingsModule {

    @Binds
    @Singleton
    abstract fun bindTodayProvider(impl: SystemTodayProvider): TodayProvider
}
