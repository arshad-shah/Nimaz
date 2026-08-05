package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.core.util.NextWorshipResolver
import com.arshadshah.nimaz.domain.repository.KhatamRepository
import com.arshadshah.nimaz.domain.repository.QaidaRepository
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.repository.ZakatRepository
import com.arshadshah.nimaz.domain.repository.settings.ZakatSettings
import com.arshadshah.nimaz.domain.usecase.GetAllHistoryUseCase
import com.arshadshah.nimaz.domain.usecase.GetHijriTodayUseCase
import com.arshadshah.nimaz.domain.usecase.GetNextWorshipUseCase
import com.arshadshah.nimaz.domain.usecase.GetPendingMakeupFastsUseCase
import com.arshadshah.nimaz.domain.usecase.GetTodayPrayerRecordsUseCase
import com.arshadshah.nimaz.domain.usecase.MoreUseCases
import com.arshadshah.nimaz.domain.usecase.ObserveHijriDayOffsetUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveKhatamRowProgressUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveQaidaRowProgressUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveZakatCurrencyUseCase
import com.arshadshah.nimaz.domain.repository.FastingRepository
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The bundle More reads.
 *
 * Its own module rather than another entry in `RepositoryModule`: More is the one screen that
 * reaches across seven features, so its dependency list is the widest in the app and worth being
 * legible in one place. If a row is ever dropped from More, the import that stops being needed
 * says so here.
 */
@Module
@InstallIn(SingletonComponent::class)
object MoreModule {

    @Provides
    @Singleton
    fun provideMoreUseCases(
        prayerRepository: PrayerRepository,
        fastingRepository: FastingRepository,
        nextWorshipResolver: NextWorshipResolver,
        khatamRepository: KhatamRepository,
        qaidaRepository: QaidaRepository,
        zakatRepository: ZakatRepository,
        zakatSettings: ZakatSettings,
        settingsRepository: SettingsRepository,
    ): MoreUseCases = MoreUseCases(
        todayPrayerRecords = GetTodayPrayerRecordsUseCase(prayerRepository),
        pendingMakeupFasts = GetPendingMakeupFastsUseCase(fastingRepository),
        nextWorship = GetNextWorshipUseCase(nextWorshipResolver),
        khatamRowProgress = ObserveKhatamRowProgressUseCase(khatamRepository),
        qaidaRowProgress = ObserveQaidaRowProgressUseCase(qaidaRepository),
        zakatHistory = GetAllHistoryUseCase(zakatRepository),
        hijriToday = GetHijriTodayUseCase(),
        hijriDayOffset = ObserveHijriDayOffsetUseCase(settingsRepository),
        zakatCurrency = ObserveZakatCurrencyUseCase(zakatSettings),
    )
}
