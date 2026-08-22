package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.core.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.repository.settings.AiSettings
import com.arshadshah.nimaz.domain.repository.settings.AppSettings
import com.arshadshah.nimaz.domain.repository.settings.DuaDisplaySettings
import com.arshadshah.nimaz.domain.repository.settings.HadithDisplaySettings
import com.arshadshah.nimaz.domain.repository.settings.HijriSettings
import com.arshadshah.nimaz.domain.repository.settings.LocationSettings
import com.arshadshah.nimaz.domain.repository.settings.MoreSettings
import com.arshadshah.nimaz.domain.repository.settings.QuranPreferences
import com.arshadshah.nimaz.domain.repository.settings.SearchSettings
import com.arshadshah.nimaz.domain.repository.settings.TasbihSettings
import com.arshadshah.nimaz.domain.repository.settings.ZakatSettings
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The settings seams and preference-backed implementations that live in `:core:datastore`.
 *
 * Twelve of the forty-two `@Binds` from `:app`'s old `RepositoryModule`, split out in PR 22 of
 * #551. These are the per-feature `*Settings` interfaces a screen or ViewModel reads instead of
 * constructing `PreferencesDataStore` itself — the rule `CLAUDE.md` states for this module, now
 * with its wiring in the same place as the thing it wires.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsBindingsModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        preferencesDataStore: PreferencesDataStore
    ): SettingsRepository

    // Feature-scoped settings seams. Every one resolves to the same DataStore-backed
    // singleton as SettingsRepository above — the split exists so a ViewModel declares
    // which feature's preferences it depends on instead of taking all 179 members.
    // See domain/repository/settings/SettingsSeams.kt.

    @Binds
    @Singleton
    abstract fun bindQuranPreferences(impl: PreferencesDataStore): QuranPreferences

    @Binds
    @Singleton
    abstract fun bindHadithDisplaySettings(impl: PreferencesDataStore): HadithDisplaySettings

    @Binds
    @Singleton
    abstract fun bindDuaDisplaySettings(impl: PreferencesDataStore): DuaDisplaySettings

    @Binds
    @Singleton
    abstract fun bindTasbihSettings(impl: PreferencesDataStore): TasbihSettings

    @Binds
    @Singleton
    abstract fun bindZakatSettings(impl: PreferencesDataStore): ZakatSettings

    @Binds
    abstract fun bindHijriSettings(impl: PreferencesDataStore): HijriSettings

    @Binds
    abstract fun bindSearchSettings(impl: PreferencesDataStore): SearchSettings

    @Binds
    @Singleton
    abstract fun bindAiSettings(impl: PreferencesDataStore): AiSettings

    @Binds
    @Singleton
    abstract fun bindLocationSettings(impl: PreferencesDataStore): LocationSettings

    @Binds
    @Singleton
    abstract fun bindMoreSettings(impl: PreferencesDataStore): MoreSettings

    @Binds
    @Singleton
    abstract fun bindAppSettings(impl: PreferencesDataStore): AppSettings
}
