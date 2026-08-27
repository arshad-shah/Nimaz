package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.domain.time.SystemTodayProvider
import com.arshadshah.nimaz.domain.time.TodayProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * `TodayProvider` -> `SystemTodayProvider`, bound beside the implementation.
 *
 * Moved out of `:app`'s `core/di` in PR 22 of #551. The seam that removed twenty `LocalDate.now()`
 * calls from ViewModels is domain code on both sides, so nothing about this binding ever needed to
 * be visible to the application module.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TimeBindingsModule {

    @Binds
    @Singleton
    abstract fun bindTodayProvider(impl: SystemTodayProvider): TodayProvider
}
