package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.core.text.StringProvider
import com.arshadshah.nimaz.data.device.AndroidCompassSensors
import com.arshadshah.nimaz.data.device.AndroidDeviceLocationRepository
import com.arshadshah.nimaz.data.device.AndroidHaptics
import com.arshadshah.nimaz.data.device.AndroidPermissionChecker
import com.arshadshah.nimaz.data.device.AndroidPowerSettings
import com.arshadshah.nimaz.data.platform.AndroidAppLocale
import com.arshadshah.nimaz.data.repository.AsmaUlHusnaRepositoryImpl
import com.arshadshah.nimaz.data.repository.AsmaUnNabiRepositoryImpl
import com.arshadshah.nimaz.data.repository.DuaRepositoryImpl
import com.arshadshah.nimaz.data.repository.FastingRepositoryImpl
import com.arshadshah.nimaz.data.repository.HadithRepositoryImpl
import com.arshadshah.nimaz.data.repository.HelpRepositoryImpl
import com.arshadshah.nimaz.data.repository.IslamicEventRepositoryImpl
import com.arshadshah.nimaz.data.repository.KhatamRepositoryImpl
import com.arshadshah.nimaz.data.repository.PrayerRepositoryImpl
import com.arshadshah.nimaz.data.repository.ProphetRepositoryImpl
import com.arshadshah.nimaz.data.repository.QaidaRepositoryImpl
import com.arshadshah.nimaz.data.repository.QuranRepositoryImpl
import com.arshadshah.nimaz.data.repository.TafseerRepositoryImpl
import com.arshadshah.nimaz.data.repository.TasbihRepositoryImpl
import com.arshadshah.nimaz.data.repository.UserDataRepositoryImpl
import com.arshadshah.nimaz.data.repository.ZakatRepositoryImpl
import com.arshadshah.nimaz.data.text.AndroidStringProvider
import com.arshadshah.nimaz.domain.repository.AppLocale
import com.arshadshah.nimaz.domain.repository.AsmaUlHusnaRepository
import com.arshadshah.nimaz.domain.repository.AsmaUnNabiRepository
import com.arshadshah.nimaz.domain.repository.CompassSensors
import com.arshadshah.nimaz.domain.repository.DeviceLocationRepository
import com.arshadshah.nimaz.domain.repository.DuaRepository
import com.arshadshah.nimaz.domain.repository.FastingRepository
import com.arshadshah.nimaz.domain.repository.HadithRepository
import com.arshadshah.nimaz.domain.repository.Haptics
import com.arshadshah.nimaz.domain.repository.HelpRepository
import com.arshadshah.nimaz.domain.repository.IslamicEventRepository
import com.arshadshah.nimaz.domain.repository.KhatamRepository
import com.arshadshah.nimaz.domain.repository.PermissionChecker
import com.arshadshah.nimaz.domain.repository.PowerSettings
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.domain.repository.ProphetRepository
import com.arshadshah.nimaz.domain.repository.QaidaRepository
import com.arshadshah.nimaz.domain.repository.QuranRepository
import com.arshadshah.nimaz.domain.repository.TafseerRepository
import com.arshadshah.nimaz.domain.repository.TasbihRepository
import com.arshadshah.nimaz.domain.repository.UserDataRepository
import com.arshadshah.nimaz.domain.repository.ZakatRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Every repository implementation in `:core:data`, bound to the `:core:domain` interface it
 * implements.
 *
 * Twenty-three of the forty-two `@Binds` that used to sit in `:app`'s 905-line
 * `RepositoryModule`. PR 22 of #551 split that file along module lines: a binding belongs to the
 * module that owns the *implementation*, because that is the module that has to compile if the
 * implementation changes. Nothing about the graph changed — same `SingletonComponent`, same
 * `@Singleton` — only where the declaration lives.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {


    @Binds
    @Singleton
    abstract fun bindQuranRepository(
        quranRepositoryImpl: QuranRepositoryImpl
    ): QuranRepository

    @Binds
    @Singleton
    abstract fun bindHadithRepository(
        hadithRepositoryImpl: HadithRepositoryImpl
    ): HadithRepository

    @Binds
    @Singleton
    abstract fun bindDuaRepository(
        duaRepositoryImpl: DuaRepositoryImpl
    ): DuaRepository

    @Binds
    @Singleton
    abstract fun bindDeviceLocationRepository(
        impl: AndroidDeviceLocationRepository
    ): DeviceLocationRepository

    @Binds
    @Singleton
    abstract fun bindPermissionChecker(
        impl: AndroidPermissionChecker
    ): PermissionChecker

    @Binds
    @Singleton
    abstract fun bindPowerSettings(
        impl: AndroidPowerSettings
    ): PowerSettings

    @Binds
    @Singleton
    abstract fun bindPrayerRepository(
        prayerRepositoryImpl: PrayerRepositoryImpl
    ): PrayerRepository

    @Binds
    @Singleton
    abstract fun bindFastingRepository(
        fastingRepositoryImpl: FastingRepositoryImpl
    ): FastingRepository

    @Binds
    @Singleton
    abstract fun bindTasbihRepository(
        tasbihRepositoryImpl: TasbihRepositoryImpl
    ): TasbihRepository

    @Binds
    @Singleton
    abstract fun bindZakatRepository(
        zakatRepositoryImpl: ZakatRepositoryImpl
    ): ZakatRepository

    @Binds
    @Singleton
    abstract fun bindTafseerRepository(
        tafseerRepositoryImpl: TafseerRepositoryImpl
    ): TafseerRepository

    @Binds
    @Singleton
    abstract fun bindKhatamRepository(
        khatamRepositoryImpl: KhatamRepositoryImpl
    ): KhatamRepository

    @Binds
    @Singleton
    abstract fun bindAsmaUlHusnaRepository(
        asmaUlHusnaRepositoryImpl: AsmaUlHusnaRepositoryImpl
    ): AsmaUlHusnaRepository

    @Binds
    @Singleton
    abstract fun bindAsmaUnNabiRepository(
        asmaUnNabiRepositoryImpl: AsmaUnNabiRepositoryImpl
    ): AsmaUnNabiRepository

    @Binds
    @Singleton
    abstract fun bindProphetRepository(
        prophetRepositoryImpl: ProphetRepositoryImpl
    ): ProphetRepository

    @Binds
    @Singleton
    abstract fun bindHelpRepository(
        helpRepositoryImpl: HelpRepositoryImpl
    ): HelpRepository

    @Binds
    @Singleton
    abstract fun bindQaidaRepository(
        qaidaRepositoryImpl: QaidaRepositoryImpl
    ): QaidaRepository

    @Binds
    @Singleton
    abstract fun bindIslamicEventRepository(
        islamicEventRepositoryImpl: IslamicEventRepositoryImpl
    ): IslamicEventRepository

    @Binds
    @Singleton
    abstract fun bindStringProvider(impl: AndroidStringProvider): StringProvider

    @Binds
    @Singleton
    abstract fun bindCompassSensors(impl: AndroidCompassSensors): CompassSensors

    @Binds
    @Singleton
    abstract fun bindHaptics(impl: AndroidHaptics): Haptics

    @Binds
    @Singleton
    abstract fun bindAppLocale(impl: AndroidAppLocale): AppLocale

    @Binds
    @Singleton
    abstract fun bindUserDataRepository(
        userDataRepositoryImpl: UserDataRepositoryImpl
    ): UserDataRepository
}
