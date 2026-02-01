package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.data.repository.DuaRepositoryImpl
import com.arshadshah.nimaz.data.repository.FastingRepositoryImpl
import com.arshadshah.nimaz.data.repository.HadithRepositoryImpl
import com.arshadshah.nimaz.data.repository.PrayerRepositoryImpl
import com.arshadshah.nimaz.data.repository.QuranRepositoryImpl
import com.arshadshah.nimaz.data.repository.TafseerRepositoryImpl
import com.arshadshah.nimaz.data.repository.TasbihRepositoryImpl
import com.arshadshah.nimaz.data.repository.ZakatRepositoryImpl
import com.arshadshah.nimaz.domain.repository.DuaRepository
import com.arshadshah.nimaz.domain.repository.FastingRepository
import com.arshadshah.nimaz.domain.repository.HadithRepository
import com.arshadshah.nimaz.domain.repository.PrayerRepository
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
}
