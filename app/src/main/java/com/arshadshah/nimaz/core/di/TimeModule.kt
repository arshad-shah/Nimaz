package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.core.time.SystemTodayProvider
import com.arshadshah.nimaz.core.time.TodayProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * The dispatcher for work that is CPU-bound rather than blocking on I/O.
 *
 * Injected rather than referenced as `Dispatchers.Default` so a test can substitute its own
 * scheduler and stay deterministic — without that, a `withContext(Dispatchers.Default)` runs
 * on real threads and `advanceUntilIdle()` has nothing to wait for.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/**
 * The dispatcher for work that blocks on I/O — a geocoder round trip, a location fix.
 *
 * Same reason as [DefaultDispatcher]: injected so a test stays deterministic.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

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
