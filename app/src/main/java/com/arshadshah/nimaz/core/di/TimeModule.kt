package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.core.common.DefaultDispatcher
import com.arshadshah.nimaz.core.common.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * The three JVM values that stayed in `:app` when PR 22 of #551 dissolved `core/di`.
 *
 * `@IoDispatcher` and `@DefaultDispatcher` are declared in `:core:common`, and `SingletonComponent`
 * is what says *where* these install — so the natural home is `:core:common`. It does not apply
 * `nimaz.android.hilt`, and adding a KSP processor to a module for three `@Provides` of
 * `Dispatchers.IO`, `Dispatchers.Default` and a `Clock` costs more build time than the tidiness is
 * worth. `TimeBindingsModule` did move: `SystemTodayProvider` is a `:core:domain` type, so its
 * binding now sits beside it.
 */
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
