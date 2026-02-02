package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.data.repository.AsmaUlHusnaRepositoryImpl
import com.arshadshah.nimaz.data.repository.AsmaUnNabiRepositoryImpl
import com.arshadshah.nimaz.data.repository.DuaRepositoryImpl
import com.arshadshah.nimaz.data.repository.FastingRepositoryImpl
import com.arshadshah.nimaz.data.repository.HadithRepositoryImpl
import com.arshadshah.nimaz.data.repository.KhatamRepositoryImpl
import com.arshadshah.nimaz.data.repository.PrayerRepositoryImpl
import com.arshadshah.nimaz.data.repository.ProphetRepositoryImpl
import com.arshadshah.nimaz.data.repository.QuranRepositoryImpl
import com.arshadshah.nimaz.data.repository.TafseerRepositoryImpl
import com.arshadshah.nimaz.data.repository.TasbihRepositoryImpl
import com.arshadshah.nimaz.data.repository.ZakatRepositoryImpl
import com.arshadshah.nimaz.domain.repository.AsmaUlHusnaRepository
import com.arshadshah.nimaz.domain.repository.AsmaUnNabiRepository
import com.arshadshah.nimaz.domain.repository.DuaRepository
import com.arshadshah.nimaz.domain.repository.FastingRepository
import com.arshadshah.nimaz.domain.repository.HadithRepository
import com.arshadshah.nimaz.domain.repository.KhatamRepository
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.domain.repository.ProphetRepository
import com.arshadshah.nimaz.domain.repository.QuranRepository
import com.arshadshah.nimaz.domain.repository.TafseerRepository
import com.arshadshah.nimaz.domain.repository.TasbihRepository
import com.arshadshah.nimaz.domain.repository.ZakatRepository
import com.arshadshah.nimaz.domain.usecase.DeleteQuranBookmarkUseCase
import com.arshadshah.nimaz.domain.usecase.GetAvailableTranslatorsUseCase
import com.arshadshah.nimaz.domain.usecase.GetAyahsByJuzUseCase
import com.arshadshah.nimaz.domain.usecase.GetAyahsByPageUseCase
import com.arshadshah.nimaz.domain.usecase.GetQuranBookmarksUseCase
import com.arshadshah.nimaz.domain.usecase.GetQuranFavoriteAyahIdsUseCase
import com.arshadshah.nimaz.domain.usecase.GetQuranFavoritesUseCase
import com.arshadshah.nimaz.domain.usecase.GetReadingProgressUseCase
import com.arshadshah.nimaz.domain.usecase.GetSajdaAyahsUseCase
import com.arshadshah.nimaz.domain.usecase.GetSurahInfoUseCase
import com.arshadshah.nimaz.domain.usecase.GetSurahListUseCase
import com.arshadshah.nimaz.domain.usecase.GetSurahWithAyahsUseCase
import com.arshadshah.nimaz.domain.usecase.IncrementAyahsReadUseCase
import com.arshadshah.nimaz.domain.usecase.IsAyahBookmarkedUseCase
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.SearchQuranUseCase
import com.arshadshah.nimaz.domain.usecase.ToggleQuranBookmarkUseCase
import com.arshadshah.nimaz.domain.usecase.ToggleQuranFavoriteUseCase
import com.arshadshah.nimaz.domain.usecase.UpdateQuranBookmarkUseCase
import com.arshadshah.nimaz.domain.usecase.UpdateReadingPositionUseCase
import com.arshadshah.nimaz.domain.usecase.AsmaUlHusnaUseCases
import com.arshadshah.nimaz.domain.usecase.AsmaUnNabiUseCases
import com.arshadshah.nimaz.domain.usecase.GetAllAsmaUlHusnaUseCase
import com.arshadshah.nimaz.domain.usecase.GetAsmaUlHusnaByIdUseCase
import com.arshadshah.nimaz.domain.usecase.SearchAsmaUlHusnaUseCase
import com.arshadshah.nimaz.domain.usecase.ToggleAsmaUlHusnaFavoriteUseCase
import com.arshadshah.nimaz.domain.usecase.GetFavoriteAsmaUlHusnaUseCase
import com.arshadshah.nimaz.domain.usecase.GetAllAsmaUnNabiUseCase
import com.arshadshah.nimaz.domain.usecase.GetAsmaUnNabiByIdUseCase
import com.arshadshah.nimaz.domain.usecase.SearchAsmaUnNabiUseCase
import com.arshadshah.nimaz.domain.usecase.ToggleAsmaUnNabiFavoriteUseCase
import com.arshadshah.nimaz.domain.usecase.GetFavoriteAsmaUnNabiUseCase
import com.arshadshah.nimaz.domain.usecase.ProphetUseCases
import com.arshadshah.nimaz.domain.usecase.GetAllProphetsUseCase
import com.arshadshah.nimaz.domain.usecase.GetProphetByIdUseCase
import com.arshadshah.nimaz.domain.usecase.SearchProphetsUseCase
import com.arshadshah.nimaz.domain.usecase.ToggleProphetFavoriteUseCase
import com.arshadshah.nimaz.domain.usecase.GetFavoriteProphetsUseCase
import com.arshadshah.nimaz.domain.usecase.AbandonKhatamUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveAbandonedKhatamsUseCase
import com.arshadshah.nimaz.domain.usecase.ReactivateKhatamUseCase
import com.arshadshah.nimaz.domain.usecase.CompleteKhatamUseCase
import com.arshadshah.nimaz.domain.usecase.CreateKhatamUseCase
import com.arshadshah.nimaz.domain.usecase.DeleteKhatamUseCase
import com.arshadshah.nimaz.domain.usecase.GetActiveKhatamUseCase
import com.arshadshah.nimaz.domain.usecase.GetJuzProgressUseCase
import com.arshadshah.nimaz.domain.usecase.GetKhatamStatsUseCase
import com.arshadshah.nimaz.domain.usecase.GetReadAyahIdsUseCase
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import com.arshadshah.nimaz.domain.usecase.LogDailyProgressUseCase
import com.arshadshah.nimaz.domain.usecase.MarkAyahsReadUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveActiveKhatamUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveAllKhatamsUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveCompletedKhatamsUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveDailyLogsUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveInProgressKhatamsUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveKhatamByIdUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveReadAyahIdsUseCase
import com.arshadshah.nimaz.domain.usecase.SetActiveKhatamUseCase
import com.arshadshah.nimaz.domain.usecase.GetNextUnreadPositionUseCase
import com.arshadshah.nimaz.domain.usecase.MarkSurahAsReadUseCase
import com.arshadshah.nimaz.domain.usecase.UnmarkAyahReadUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

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
}

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideQuranUseCases(
        repository: QuranRepository
    ): QuranUseCases {
        return QuranUseCases(
            getSurahList = GetSurahListUseCase(repository),
            getSurahWithAyahs = GetSurahWithAyahsUseCase(repository),
            getAyahsByJuz = GetAyahsByJuzUseCase(repository),
            getAyahsByPage = GetAyahsByPageUseCase(repository),
            getSajdaAyahs = GetSajdaAyahsUseCase(repository),
            searchQuran = SearchQuranUseCase(repository),
            getAvailableTranslators = GetAvailableTranslatorsUseCase(repository),
            toggleBookmark = ToggleQuranBookmarkUseCase(repository),
            getBookmarks = GetQuranBookmarksUseCase(repository),
            isAyahBookmarked = IsAyahBookmarkedUseCase(repository),
            updateBookmark = UpdateQuranBookmarkUseCase(repository),
            deleteBookmark = DeleteQuranBookmarkUseCase(repository),
            toggleFavorite = ToggleQuranFavoriteUseCase(repository),
            getFavorites = GetQuranFavoritesUseCase(repository),
            getFavoriteAyahIds = GetQuranFavoriteAyahIdsUseCase(repository),
            getReadingProgress = GetReadingProgressUseCase(repository),
            updateReadingPosition = UpdateReadingPositionUseCase(repository),
            incrementAyahsRead = IncrementAyahsReadUseCase(repository),
            getSurahInfo = GetSurahInfoUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideKhatamUseCases(
        repository: KhatamRepository
    ): KhatamUseCases {
        return KhatamUseCases(
            createKhatam = CreateKhatamUseCase(repository),
            getActiveKhatam = GetActiveKhatamUseCase(repository),
            observeActiveKhatam = ObserveActiveKhatamUseCase(repository),
            setActiveKhatam = SetActiveKhatamUseCase(repository),
            markAyahsRead = MarkAyahsReadUseCase(repository),
            getReadAyahIds = GetReadAyahIdsUseCase(repository),
            observeReadAyahIds = ObserveReadAyahIdsUseCase(repository),
            getJuzProgress = GetJuzProgressUseCase(repository),
            observeDailyLogs = ObserveDailyLogsUseCase(repository),
            completeKhatam = CompleteKhatamUseCase(repository),
            abandonKhatam = AbandonKhatamUseCase(repository),
            reactivateKhatam = ReactivateKhatamUseCase(repository),
            deleteKhatam = DeleteKhatamUseCase(repository),
            observeAllKhatams = ObserveAllKhatamsUseCase(repository),
            observeInProgressKhatams = ObserveInProgressKhatamsUseCase(repository),
            observeCompletedKhatams = ObserveCompletedKhatamsUseCase(repository),
            observeAbandonedKhatams = ObserveAbandonedKhatamsUseCase(repository),
            observeKhatamById = ObserveKhatamByIdUseCase(repository),
            logDailyProgress = LogDailyProgressUseCase(repository),
            getKhatamStats = GetKhatamStatsUseCase(repository),
            getNextUnreadPosition = GetNextUnreadPositionUseCase(repository),
            unmarkAyahRead = UnmarkAyahReadUseCase(repository),
            markSurahAsRead = MarkSurahAsReadUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideAsmaUlHusnaUseCases(
        repository: AsmaUlHusnaRepository
    ): AsmaUlHusnaUseCases {
        return AsmaUlHusnaUseCases(
            getAllNames = GetAllAsmaUlHusnaUseCase(repository),
            getNameById = GetAsmaUlHusnaByIdUseCase(repository),
            searchNames = SearchAsmaUlHusnaUseCase(repository),
            toggleFavorite = ToggleAsmaUlHusnaFavoriteUseCase(repository),
            getFavorites = GetFavoriteAsmaUlHusnaUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideAsmaUnNabiUseCases(
        repository: AsmaUnNabiRepository
    ): AsmaUnNabiUseCases {
        return AsmaUnNabiUseCases(
            getAllNames = GetAllAsmaUnNabiUseCase(repository),
            getNameById = GetAsmaUnNabiByIdUseCase(repository),
            searchNames = SearchAsmaUnNabiUseCase(repository),
            toggleFavorite = ToggleAsmaUnNabiFavoriteUseCase(repository),
            getFavorites = GetFavoriteAsmaUnNabiUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideProphetUseCases(
        repository: ProphetRepository
    ): ProphetUseCases {
        return ProphetUseCases(
            getAllProphets = GetAllProphetsUseCase(repository),
            getProphetById = GetProphetByIdUseCase(repository),
            searchProphets = SearchProphetsUseCase(repository),
            toggleFavorite = ToggleProphetFavoriteUseCase(repository),
            getFavorites = GetFavoriteProphetsUseCase(repository)
        )
    }
}
