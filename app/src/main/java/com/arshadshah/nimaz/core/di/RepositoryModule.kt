package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.data.local.dua.AndroidDuaAssetReader
import com.arshadshah.nimaz.data.local.dua.DataStoreDuaContentVersionStore
import com.arshadshah.nimaz.data.local.dua.DuaAssetReader
import com.arshadshah.nimaz.data.local.dua.DuaContentVersionStore
import com.arshadshah.nimaz.data.local.hadith.AndroidHadithAssetReader
import com.arshadshah.nimaz.data.local.hadith.DataStoreHadithBackfillVersionStore
import com.arshadshah.nimaz.data.local.hadith.HadithAssetReader
import com.arshadshah.nimaz.data.local.hadith.HadithBackfillVersionStore
import com.arshadshah.nimaz.data.local.help.AndroidHelpAssetReader
import com.arshadshah.nimaz.data.local.help.DataStoreHelpContentVersionStore
import com.arshadshah.nimaz.data.local.help.HelpAssetReader
import com.arshadshah.nimaz.data.local.help.HelpContentVersionStore
import com.arshadshah.nimaz.data.local.qaida.AndroidQaidaAssetReader
import com.arshadshah.nimaz.data.local.qaida.DataStoreQaidaContentVersionStore
import com.arshadshah.nimaz.data.local.qaida.QaidaAssetReader
import com.arshadshah.nimaz.data.local.qaida.QaidaContentVersionStore
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
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
import com.arshadshah.nimaz.data.repository.ZakatRepositoryImpl
import com.arshadshah.nimaz.domain.repository.AsmaUlHusnaRepository
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.repository.AsmaUnNabiRepository
import com.arshadshah.nimaz.domain.repository.DuaRepository
import com.arshadshah.nimaz.domain.repository.FastingRepository
import com.arshadshah.nimaz.domain.repository.HadithRepository
import com.arshadshah.nimaz.domain.repository.HelpRepository
import com.arshadshah.nimaz.domain.repository.IslamicEventRepository
import com.arshadshah.nimaz.domain.repository.KhatamRepository
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.domain.repository.ProphetRepository
import com.arshadshah.nimaz.domain.repository.QaidaRepository
import com.arshadshah.nimaz.domain.repository.QuranRepository
import com.arshadshah.nimaz.domain.repository.TafseerRepository
import com.arshadshah.nimaz.domain.repository.TasbihRepository
import com.arshadshah.nimaz.domain.repository.ZakatRepository
import com.arshadshah.nimaz.domain.usecase.AbandonKhatamUseCase
import com.arshadshah.nimaz.domain.usecase.AsmaUlHusnaUseCases
import com.arshadshah.nimaz.domain.usecase.AsmaUnNabiUseCases
import com.arshadshah.nimaz.domain.usecase.CompleteKhatamUseCase
import com.arshadshah.nimaz.domain.usecase.CreateKhatamUseCase
import com.arshadshah.nimaz.domain.usecase.DeleteKhatamUseCase
import com.arshadshah.nimaz.domain.usecase.DeleteQuranBookmarkUseCase
import com.arshadshah.nimaz.domain.usecase.InsertQuranBookmarkUseCase
import com.arshadshah.nimaz.domain.usecase.InsertHadithBookmarkUseCase
import com.arshadshah.nimaz.domain.usecase.InsertDuaBookmarkUseCase
import com.arshadshah.nimaz.domain.usecase.UpdateDuaBookmarkUseCase
import com.arshadshah.nimaz.domain.usecase.GetAllAsmaUlHusnaUseCase
import com.arshadshah.nimaz.domain.usecase.GetAllAsmaUnNabiUseCase
import com.arshadshah.nimaz.domain.usecase.GetAllProphetsUseCase
import com.arshadshah.nimaz.domain.usecase.GetAsmaUlHusnaByIdUseCase
import com.arshadshah.nimaz.domain.usecase.GetAsmaUnNabiByIdUseCase
import com.arshadshah.nimaz.domain.usecase.GetAvailableTranslatorsUseCase
import com.arshadshah.nimaz.domain.usecase.GetAyahsByJuzUseCase
import com.arshadshah.nimaz.domain.usecase.GetAyahsBySurahUseCase
import com.arshadshah.nimaz.domain.usecase.GetAyahByIdUseCase
import com.arshadshah.nimaz.domain.usecase.GetSurahByNumberUseCase
import com.arshadshah.nimaz.domain.usecase.GetAyahsByPageUseCase
import com.arshadshah.nimaz.domain.usecase.GetCourseProgressUseCase
import com.arshadshah.nimaz.domain.usecase.GetFavoriteAsmaUlHusnaUseCase
import com.arshadshah.nimaz.domain.usecase.GetFavoriteAsmaUnNabiUseCase
import com.arshadshah.nimaz.domain.usecase.GetFavoriteProphetsUseCase
import com.arshadshah.nimaz.domain.usecase.GetHelpGuideUseCase
import com.arshadshah.nimaz.domain.usecase.GetHelpTopicDetailUseCase
import com.arshadshah.nimaz.domain.usecase.GetHelpTopicsUseCase
import com.arshadshah.nimaz.domain.usecase.GetLessonProgressUseCase
import com.arshadshah.nimaz.domain.usecase.GetNextUnreadPositionUseCase
import com.arshadshah.nimaz.domain.usecase.GetPageAyahRangesUseCase
import com.arshadshah.nimaz.domain.usecase.GetProphetByIdUseCase
import com.arshadshah.nimaz.domain.usecase.GetQaidaCellUseCase
import com.arshadshah.nimaz.domain.usecase.GetQaidaLessonContentUseCase
import com.arshadshah.nimaz.domain.usecase.GetQaidaLessonsUseCase
import com.arshadshah.nimaz.domain.usecase.GetQaidaLettersUseCase
import com.arshadshah.nimaz.domain.usecase.GetQuranBookmarksUseCase
import com.arshadshah.nimaz.domain.usecase.GetQuranFavoriteAyahIdsUseCase
import com.arshadshah.nimaz.domain.usecase.GetQuranFavoritesUseCase
import com.arshadshah.nimaz.domain.usecase.GetReadingProgressUseCase
import com.arshadshah.nimaz.domain.usecase.GetSajdaAyahsUseCase
import com.arshadshah.nimaz.domain.usecase.GetVerseOfTheDayUseCase
import com.arshadshah.nimaz.domain.usecase.GetSurahInfoUseCase
import com.arshadshah.nimaz.domain.usecase.GetSurahListUseCase
import com.arshadshah.nimaz.domain.usecase.GetSurahWithAyahsUseCase
import com.arshadshah.nimaz.domain.usecase.HelpUseCases
import com.arshadshah.nimaz.domain.usecase.IncrementAyahsReadUseCase
import com.arshadshah.nimaz.domain.usecase.IsAyahBookmarkedUseCase
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import com.arshadshah.nimaz.domain.usecase.LogDailyProgressUseCase
import com.arshadshah.nimaz.domain.usecase.MarkAyahsReadUseCase
import com.arshadshah.nimaz.domain.usecase.MarkCellHeardUseCase
import com.arshadshah.nimaz.domain.usecase.MarkSurahAsReadUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveAbandonedKhatamsUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveActiveKhatamUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveAllKhatamsUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveCompletedCellsUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveCompletedKhatamsUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveDailyLogsUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveInProgressKhatamsUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveJuzProgressUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveKhatamByIdUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveKhatamDetailUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveKhatamStatsUseCase
import com.arshadshah.nimaz.domain.usecase.UpdateKhatamUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveReadAyahIdsUseCase
import com.arshadshah.nimaz.domain.usecase.ProphetUseCases
import com.arshadshah.nimaz.domain.usecase.QaidaUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.ReactivateKhatamUseCase
import com.arshadshah.nimaz.domain.usecase.ResetQaidaProgressUseCase
import com.arshadshah.nimaz.domain.usecase.SearchAsmaUlHusnaUseCase
import com.arshadshah.nimaz.domain.usecase.SearchAsmaUnNabiUseCase
import com.arshadshah.nimaz.domain.usecase.SearchHelpUseCase
import com.arshadshah.nimaz.domain.usecase.SearchProphetsUseCase
import com.arshadshah.nimaz.domain.usecase.SearchQuranUseCase
import com.arshadshah.nimaz.domain.usecase.SetActiveKhatamUseCase
import com.arshadshah.nimaz.domain.usecase.ToggleAsmaUlHusnaFavoriteUseCase
import com.arshadshah.nimaz.domain.usecase.ToggleAsmaUnNabiFavoriteUseCase
import com.arshadshah.nimaz.domain.usecase.ToggleProphetFavoriteUseCase
import com.arshadshah.nimaz.domain.usecase.ToggleQuranBookmarkUseCase
import com.arshadshah.nimaz.domain.usecase.ToggleQuranFavoriteUseCase
import com.arshadshah.nimaz.domain.usecase.UnlockNextLessonUseCase
import com.arshadshah.nimaz.domain.usecase.UnmarkAyahReadUseCase
import com.arshadshah.nimaz.domain.usecase.UpdateQuranBookmarkUseCase
import com.arshadshah.nimaz.domain.usecase.UpdateReadingPositionUseCase
import com.arshadshah.nimaz.domain.usecase.CompleteSessionUseCase
import com.arshadshah.nimaz.domain.usecase.DeleteCustomPresetUseCase
import com.arshadshah.nimaz.domain.usecase.DeleteFastRecordByDateUseCase
import com.arshadshah.nimaz.domain.usecase.DuaUseCases
import com.arshadshah.nimaz.domain.usecase.FastingUseCases
import com.arshadshah.nimaz.domain.usecase.GetActiveSessionUseCase
import com.arshadshah.nimaz.domain.usecase.GetAllBookmarksUseCase
import com.arshadshah.nimaz.domain.usecase.GetAllBooksUseCase
import com.arshadshah.nimaz.domain.usecase.GetAllCategoriesUseCase
import com.arshadshah.nimaz.domain.usecase.GetAllMakeupFastsUseCase
import com.arshadshah.nimaz.domain.usecase.GetBookByIdUseCase
import com.arshadshah.nimaz.domain.usecase.GetCategoryByIdUseCase
import com.arshadshah.nimaz.domain.usecase.GetChapterByIdUseCase
import com.arshadshah.nimaz.domain.usecase.GetChaptersByBookUseCase
import com.arshadshah.nimaz.domain.usecase.GetCompletedSessionsInRangeUseCase
import com.arshadshah.nimaz.domain.usecase.GetCustomPresetsUseCase
import com.arshadshah.nimaz.domain.usecase.GetDefaultPresetsUseCase
import com.arshadshah.nimaz.domain.usecase.GetDuaByIdUseCase
import com.arshadshah.nimaz.domain.usecase.GetDuasByCategoryUseCase
import com.arshadshah.nimaz.domain.usecase.GetDuasByOccasionUseCase
import com.arshadshah.nimaz.domain.usecase.GetFastRecordForDateUseCase
import com.arshadshah.nimaz.domain.usecase.GetFastRecordsInRangeUseCase
import com.arshadshah.nimaz.domain.usecase.GetFastingStatsUseCase
import com.arshadshah.nimaz.domain.usecase.GetFavoriteDuasUseCase
import com.arshadshah.nimaz.domain.usecase.GetHadithByIdUseCase
import com.arshadshah.nimaz.domain.usecase.GetHadithByNumberUseCase
import com.arshadshah.nimaz.domain.usecase.GetHadithByReferenceUseCase
import com.arshadshah.nimaz.domain.usecase.GetHadithOfTheDayUseCase
import com.arshadshah.nimaz.domain.usecase.GetHadithsByChapterUseCase
import com.arshadshah.nimaz.domain.usecase.GetHadithsByGradeUseCase
import com.arshadshah.nimaz.domain.usecase.GetMakeupFastCountForDateUseCase
import com.arshadshah.nimaz.domain.usecase.GetPendingMakeupFastsUseCase
import com.arshadshah.nimaz.domain.usecase.GetPresetByIdUseCase
import com.arshadshah.nimaz.domain.usecase.GetProgressForDateUseCase
import com.arshadshah.nimaz.domain.usecase.GetRamadanFastedCountUseCase
import com.arshadshah.nimaz.domain.usecase.GetSessionByIdUseCase
import com.arshadshah.nimaz.domain.usecase.GetSessionsForDateUseCase
import com.arshadshah.nimaz.domain.usecase.GetSessionsInRangeUseCase
import com.arshadshah.nimaz.domain.usecase.GetTasbihStatsUseCase
import com.arshadshah.nimaz.domain.usecase.GetTotalCountInRangeUseCase
import com.arshadshah.nimaz.domain.usecase.GetTotalFidyaPaidUseCase
import com.arshadshah.nimaz.domain.usecase.GetVoluntaryFastCountUseCase
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.arshadshah.nimaz.domain.usecase.InsertFastRecordUseCase
import com.arshadshah.nimaz.domain.usecase.InsertMakeupFastUseCase
import com.arshadshah.nimaz.domain.usecase.InsertPresetUseCase
import com.arshadshah.nimaz.domain.usecase.InsertSessionUseCase
import com.arshadshah.nimaz.domain.usecase.IsDuaFavoriteUseCase
import com.arshadshah.nimaz.domain.usecase.IsHadithBookmarkedUseCase
import com.arshadshah.nimaz.domain.usecase.MarkFidyaPaidUseCase
import com.arshadshah.nimaz.domain.usecase.MarkMakeupFastCompletedUseCase
import com.arshadshah.nimaz.domain.usecase.SearchDuasUseCase
import com.arshadshah.nimaz.domain.usecase.SearchHadithsInBookUseCase
import com.arshadshah.nimaz.domain.usecase.SearchHadithsUseCase
import com.arshadshah.nimaz.domain.usecase.SeedMissingDefaultsUseCase
import com.arshadshah.nimaz.domain.usecase.TasbihUseCases
import com.arshadshah.nimaz.domain.usecase.ToggleBookmarkUseCase
import com.arshadshah.nimaz.domain.usecase.ToggleDuaFavoriteUseCase
import com.arshadshah.nimaz.domain.usecase.ToggleLocationFavoriteUseCase
import com.arshadshah.nimaz.domain.usecase.UpdateFastRecordUseCase
import com.arshadshah.nimaz.domain.usecase.UpdateFastStatusUseCase
import com.arshadshah.nimaz.domain.usecase.UpdateMakeupFastUseCase
import com.arshadshah.nimaz.domain.usecase.UpdatePresetUseCase
import com.arshadshah.nimaz.domain.usecase.UpdateSessionCountUseCase
import com.arshadshah.nimaz.domain.usecase.DeleteCalculationUseCase
import com.arshadshah.nimaz.domain.usecase.DeleteLocationUseCase
import com.arshadshah.nimaz.domain.usecase.GetAllHistoryUseCase
import com.arshadshah.nimaz.domain.usecase.GetAllLocationsUseCase
import com.arshadshah.nimaz.domain.usecase.GetCurrentLocationUseCase
import com.arshadshah.nimaz.domain.usecase.GetCurrentStreakUseCase
import com.arshadshah.nimaz.domain.usecase.GetFavoriteLocationsUseCase
import com.arshadshah.nimaz.domain.usecase.GetLongestStreakUseCase
import com.arshadshah.nimaz.domain.usecase.GetMissedPrayersRequiringQadaUseCase
import com.arshadshah.nimaz.domain.usecase.GetPrayerRecordsForDateUseCase
import com.arshadshah.nimaz.domain.usecase.GetPrayerRecordsInRangeUseCase
import com.arshadshah.nimaz.domain.usecase.GetPrayerStatsUseCase
import com.arshadshah.nimaz.domain.usecase.GetPrayerTimesForDateUseCase
import com.arshadshah.nimaz.domain.usecase.GetTodayPrayerRecordsUseCase
import com.arshadshah.nimaz.domain.usecase.GetTotalPaidUseCase
import com.arshadshah.nimaz.domain.usecase.InsertCalculationUseCase
import com.arshadshah.nimaz.domain.usecase.InsertLocationUseCase
import com.arshadshah.nimaz.domain.usecase.MarkAsPaidUseCase
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.arshadshah.nimaz.domain.usecase.SetCurrentLocationUseCase
import com.arshadshah.nimaz.domain.usecase.UpdatePrayerStatusUseCase
import com.arshadshah.nimaz.domain.usecase.ZakatUseCases
import com.arshadshah.nimaz.domain.usecase.TafseerUseCases
import com.arshadshah.nimaz.domain.usecase.GetTafseerForAyahUseCase
import com.arshadshah.nimaz.domain.usecase.GetTafseerNotesUseCase
import com.arshadshah.nimaz.domain.usecase.GetHighlightsForAyahUseCase
import com.arshadshah.nimaz.domain.usecase.AddHighlightUseCase
import com.arshadshah.nimaz.domain.usecase.UpdateHighlightUseCase
import com.arshadshah.nimaz.domain.usecase.DeleteHighlightUseCase
import com.arshadshah.nimaz.domain.usecase.GetNotesForAyahUseCase
import com.arshadshah.nimaz.domain.usecase.AddNoteUseCase
import com.arshadshah.nimaz.domain.usecase.UpdateNoteUseCase
import com.arshadshah.nimaz.domain.usecase.DeleteNoteUseCase
import com.arshadshah.nimaz.domain.usecase.ExportAnnotationsUseCase
import com.arshadshah.nimaz.domain.usecase.UpdateHadithBookmarkUseCase
import com.arshadshah.nimaz.domain.usecase.DeleteHadithBookmarkUseCase
import com.arshadshah.nimaz.domain.usecase.GetDuaBookmarksUseCase
import com.arshadshah.nimaz.domain.usecase.DeleteDuaBookmarkUseCase
import com.arshadshah.nimaz.domain.usecase.IslamicEventUseCases
import com.arshadshah.nimaz.domain.usecase.GetAllIslamicEventsUseCase
import com.arshadshah.nimaz.domain.usecase.GetDailyHadithUseCase
import com.arshadshah.nimaz.domain.usecase.GetDailyDuaUseCase
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

    @Binds
    @Singleton
    abstract fun bindHelpContentVersionStore(
        impl: DataStoreHelpContentVersionStore
    ): HelpContentVersionStore

    @Binds
    @Singleton
    abstract fun bindHelpAssetReader(
        impl: AndroidHelpAssetReader
    ): HelpAssetReader

    @Binds
    @Singleton
    abstract fun bindDuaContentVersionStore(
        impl: DataStoreDuaContentVersionStore
    ): DuaContentVersionStore

    @Binds
    @Singleton
    abstract fun bindDuaAssetReader(
        impl: AndroidDuaAssetReader
    ): DuaAssetReader

    @Binds
    @Singleton
    abstract fun bindQaidaContentVersionStore(
        impl: DataStoreQaidaContentVersionStore
    ): QaidaContentVersionStore

    @Binds
    @Singleton
    abstract fun bindQaidaAssetReader(
        impl: AndroidQaidaAssetReader
    ): QaidaAssetReader

    @Binds
    @Singleton
    abstract fun bindHadithBackfillVersionStore(
        impl: DataStoreHadithBackfillVersionStore
    ): HadithBackfillVersionStore

    @Binds
    @Singleton
    abstract fun bindHadithAssetReader(
        impl: AndroidHadithAssetReader
    ): HadithAssetReader

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
    abstract fun bindSettingsRepository(
        preferencesDataStore: PreferencesDataStore
    ): SettingsRepository
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
            getSurahByNumber = GetSurahByNumberUseCase(repository),
            getAyahsBySurah = GetAyahsBySurahUseCase(repository),
            getAyahById = GetAyahByIdUseCase(repository),
            getSurahWithAyahs = GetSurahWithAyahsUseCase(repository),
            getAyahsByJuz = GetAyahsByJuzUseCase(repository),
            getAyahsByPage = GetAyahsByPageUseCase(repository),
            getSajdaAyahs = GetSajdaAyahsUseCase(repository),
            searchQuran = SearchQuranUseCase(repository),
            getAvailableTranslators = GetAvailableTranslatorsUseCase(repository),
            toggleBookmark = ToggleQuranBookmarkUseCase(repository),
            getBookmarks = GetQuranBookmarksUseCase(repository),
            isAyahBookmarked = IsAyahBookmarkedUseCase(repository),
            insertBookmark = InsertQuranBookmarkUseCase(repository),
            updateBookmark = UpdateQuranBookmarkUseCase(repository),
            deleteBookmark = DeleteQuranBookmarkUseCase(repository),
            toggleFavorite = ToggleQuranFavoriteUseCase(repository),
            getFavorites = GetQuranFavoritesUseCase(repository),
            getFavoriteAyahIds = GetQuranFavoriteAyahIdsUseCase(repository),
            getReadingProgress = GetReadingProgressUseCase(repository),
            updateReadingPosition = UpdateReadingPositionUseCase(repository),
            incrementAyahsRead = IncrementAyahsReadUseCase(repository),
            getSurahInfo = GetSurahInfoUseCase(repository),
            getPageAyahRanges = GetPageAyahRangesUseCase(repository),
            getVerseOfTheDay = GetVerseOfTheDayUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideKhatamUseCases(
        repository: KhatamRepository
    ): KhatamUseCases {
        return KhatamUseCases(
            createKhatam = CreateKhatamUseCase(repository),
            updateKhatam = UpdateKhatamUseCase(repository),
            observeActiveKhatam = ObserveActiveKhatamUseCase(repository),
            setActiveKhatam = SetActiveKhatamUseCase(repository),
            markAyahsRead = MarkAyahsReadUseCase(repository),
            observeReadAyahIds = ObserveReadAyahIdsUseCase(repository),
            observeJuzProgress = ObserveJuzProgressUseCase(repository),
            observeDailyLogs = ObserveDailyLogsUseCase(repository),
            observeKhatamDetail = ObserveKhatamDetailUseCase(repository),
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
            observeKhatamStats = ObserveKhatamStatsUseCase(repository),
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

    @Provides
    @Singleton
    fun provideHelpUseCases(
        repository: HelpRepository
    ): HelpUseCases {
        return HelpUseCases(
            getTopics = GetHelpTopicsUseCase(repository),
            getTopicDetail = GetHelpTopicDetailUseCase(repository),
            getGuide = GetHelpGuideUseCase(repository),
            search = SearchHelpUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideQaidaUseCases(
        repository: QaidaRepository
    ): QaidaUseCases {
        val unlockNextLesson = UnlockNextLessonUseCase(repository)
        return QaidaUseCases(
            getLessons = GetQaidaLessonsUseCase(repository),
            getLessonContent = GetQaidaLessonContentUseCase(repository),
            getLetters = GetQaidaLettersUseCase(repository),
            getCell = GetQaidaCellUseCase(repository),
            markCellHeard = MarkCellHeardUseCase(repository, unlockNextLesson),
            unlockNextLesson = unlockNextLesson,
            getLessonProgress = GetLessonProgressUseCase(repository),
            getCourseProgress = GetCourseProgressUseCase(repository),
            resetProgress = ResetQaidaProgressUseCase(repository),
            observeCompletedCells = ObserveCompletedCellsUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideTasbihUseCases(
        repository: TasbihRepository
    ): TasbihUseCases {
        return TasbihUseCases(
            getDefaultPresets = GetDefaultPresetsUseCase(repository),
            getCustomPresets = GetCustomPresetsUseCase(repository),
            getPresetById = GetPresetByIdUseCase(repository),
            insertPreset = InsertPresetUseCase(repository),
            updatePreset = UpdatePresetUseCase(repository),
            deleteCustomPreset = DeleteCustomPresetUseCase(repository),
            seedMissingDefaults = SeedMissingDefaultsUseCase(repository),
            getSessionsForDate = GetSessionsForDateUseCase(repository),
            getSessionsInRange = GetSessionsInRangeUseCase(repository),
            getActiveSession = GetActiveSessionUseCase(repository),
            getSessionById = GetSessionByIdUseCase(repository),
            insertSession = InsertSessionUseCase(repository),
            updateSessionCount = UpdateSessionCountUseCase(repository),
            completeSession = CompleteSessionUseCase(repository),
            getTasbihStats = GetTasbihStatsUseCase(repository),
            getTotalCountInRange = GetTotalCountInRangeUseCase(repository),
            getCompletedSessionsInRange = GetCompletedSessionsInRangeUseCase(repository)
        )
    }
    @Provides
    @Singleton
    fun provideDuaUseCases(
        repository: DuaRepository
    ): DuaUseCases {
        return DuaUseCases(
            getAllCategories = GetAllCategoriesUseCase(repository),
            getCategoryById = GetCategoryByIdUseCase(repository),
            getDuaById = GetDuaByIdUseCase(repository),
            getDuasByCategory = GetDuasByCategoryUseCase(repository),
            getDuasByOccasion = GetDuasByOccasionUseCase(repository),
            getFavoriteDuas = GetFavoriteDuasUseCase(repository),
            getProgressForDate = GetProgressForDateUseCase(repository),
            isDuaFavorite = IsDuaFavoriteUseCase(repository),
            searchDuas = SearchDuasUseCase(repository),
            toggleFavorite = ToggleDuaFavoriteUseCase(repository),
            getAllBookmarks = GetDuaBookmarksUseCase(repository),
            insertBookmark = InsertDuaBookmarkUseCase(repository),
            updateBookmark = UpdateDuaBookmarkUseCase(repository),
            deleteBookmark = DeleteDuaBookmarkUseCase(repository),
            getDailyDua = GetDailyDuaUseCase(repository)
        )
    }
    @Provides
    @Singleton
    fun provideHadithUseCases(
        repository: HadithRepository
    ): HadithUseCases {
        return HadithUseCases(
            getAllBooks = GetAllBooksUseCase(repository),
            getBookById = GetBookByIdUseCase(repository),
            getChaptersByBook = GetChaptersByBookUseCase(repository),
            getChapterById = GetChapterByIdUseCase(repository),
            getHadithsByChapter = GetHadithsByChapterUseCase(repository),
            getHadithById = GetHadithByIdUseCase(repository),
            getHadithByNumber = GetHadithByNumberUseCase(repository),
            getHadithByReference = GetHadithByReferenceUseCase(repository),
            getHadithsByGrade = GetHadithsByGradeUseCase(repository),
            getHadithOfTheDay = GetHadithOfTheDayUseCase(repository),
            searchHadiths = SearchHadithsUseCase(repository),
            searchHadithsInBook = SearchHadithsInBookUseCase(repository),
            getAllBookmarks = GetAllBookmarksUseCase(repository),
            isHadithBookmarked = IsHadithBookmarkedUseCase(repository),
            toggleBookmark = ToggleBookmarkUseCase(repository),
            insertBookmark = InsertHadithBookmarkUseCase(repository),
            updateBookmark = UpdateHadithBookmarkUseCase(repository),
            deleteBookmark = DeleteHadithBookmarkUseCase(repository),
            getDailyHadith = GetDailyHadithUseCase(repository)
        )
    }
    @Provides
    @Singleton
    fun provideFastingUseCases(
        repository: FastingRepository
    ): FastingUseCases {
        return FastingUseCases(
            getFastRecordForDate = GetFastRecordForDateUseCase(repository),
            getFastRecordsInRange = GetFastRecordsInRangeUseCase(repository),
            insertFastRecord = InsertFastRecordUseCase(repository),
            updateFastRecord = UpdateFastRecordUseCase(repository),
            updateFastStatus = UpdateFastStatusUseCase(repository),
            deleteFastRecordByDate = DeleteFastRecordByDateUseCase(repository),
            getRamadanFastedCount = GetRamadanFastedCountUseCase(repository),
            getVoluntaryFastCount = GetVoluntaryFastCountUseCase(repository),
            getFastingStats = GetFastingStatsUseCase(repository),
            getAllMakeupFasts = GetAllMakeupFastsUseCase(repository),
            getPendingMakeupFasts = GetPendingMakeupFastsUseCase(repository),
            getMakeupFastCountForDate = GetMakeupFastCountForDateUseCase(repository),
            insertMakeupFast = InsertMakeupFastUseCase(repository),
            updateMakeupFast = UpdateMakeupFastUseCase(repository),
            markMakeupFastCompleted = MarkMakeupFastCompletedUseCase(repository),
            markFidyaPaid = MarkFidyaPaidUseCase(repository),
            getTotalFidyaPaid = GetTotalFidyaPaidUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun providePrayerUseCases(
        repository: PrayerRepository
    ): PrayerUseCases {
        return PrayerUseCases(
            getPrayerRecordsForDate = GetPrayerRecordsForDateUseCase(repository),
            getPrayerRecordsInRange = GetPrayerRecordsInRangeUseCase(repository),
            getTodayPrayerRecords = GetTodayPrayerRecordsUseCase(repository),
            updatePrayerStatus = UpdatePrayerStatusUseCase(repository),
            getPrayerTimesForDate = GetPrayerTimesForDateUseCase(repository),
            getCurrentStreak = GetCurrentStreakUseCase(repository),
            getLongestStreak = GetLongestStreakUseCase(repository),
            getMissedPrayersRequiringQada = GetMissedPrayersRequiringQadaUseCase(repository),
            getPrayerStats = GetPrayerStatsUseCase(repository),
            getCurrentLocation = GetCurrentLocationUseCase(repository),
            getAllLocations = GetAllLocationsUseCase(repository),
            getFavoriteLocations = GetFavoriteLocationsUseCase(repository),
            insertLocation = InsertLocationUseCase(repository),
            deleteLocation = DeleteLocationUseCase(repository),
            setCurrentLocation = SetCurrentLocationUseCase(repository),
            toggleFavorite = ToggleLocationFavoriteUseCase(repository)
        )
    }
    @Provides
    @Singleton
    fun provideZakatUseCases(
        repository: ZakatRepository
    ): ZakatUseCases {
        return ZakatUseCases(
            getAllHistory = GetAllHistoryUseCase(repository),
            insertCalculation = InsertCalculationUseCase(repository),
            markAsPaid = MarkAsPaidUseCase(repository),
            getTotalPaid = GetTotalPaidUseCase(repository),
            deleteCalculation = DeleteCalculationUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideTafseerUseCases(
        repository: TafseerRepository,
        quranRepository: QuranRepository
    ): TafseerUseCases {
        return TafseerUseCases(
            getTafseerForAyah = GetTafseerForAyahUseCase(repository),
            getHighlightsForAyah = GetHighlightsForAyahUseCase(repository),
            addHighlight = AddHighlightUseCase(repository),
            updateHighlight = UpdateHighlightUseCase(repository),
            deleteHighlight = DeleteHighlightUseCase(repository),
            getNotesForAyah = GetNotesForAyahUseCase(repository),
            addNote = AddNoteUseCase(repository),
            updateNote = UpdateNoteUseCase(repository),
            deleteNote = DeleteNoteUseCase(repository),
            exportAnnotations = ExportAnnotationsUseCase(repository),
            getTafseerNotes = GetTafseerNotesUseCase(repository, quranRepository)
        )
    }

    @Provides
    @Singleton
    fun provideIslamicEventUseCases(
        repository: IslamicEventRepository
    ): IslamicEventUseCases {
        return IslamicEventUseCases(
            getAllEvents = GetAllIslamicEventsUseCase(repository)
        )
    }
}
