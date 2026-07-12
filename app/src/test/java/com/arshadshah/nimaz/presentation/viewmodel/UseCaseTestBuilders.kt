package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.domain.repository.AnnouncementRepository
import com.arshadshah.nimaz.domain.repository.FastingRepository
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.domain.repository.HadithRepository
import com.arshadshah.nimaz.domain.repository.DuaRepository
import com.arshadshah.nimaz.domain.usecase.*

// Test helpers: wrap a (mock) repository in the real use-case wrappers so existing
// repository-level stubbing continues to drive ViewModel behaviour after the
// use-case layer was introduced.

fun buildAnnouncementUseCases(
    repository: AnnouncementRepository,
    currentVersionCode: Int = 1,
    nowMillis: () -> Long = { System.currentTimeMillis() },
    isKnownFeatureKey: (String) -> Boolean = { false },
) = AnnouncementUseCases(
    observeActiveAnnouncement = ObserveActiveAnnouncementUseCase(
        repository,
        currentVersionCode,
        nowMillis
    ),
    setAnnouncement = SetAnnouncementUseCase(repository),
    dismissAnnouncement = DismissAnnouncementUseCase(repository),
    resolveAnnouncementRoute = ResolveAnnouncementRouteUseCase(isKnownFeatureKey)
)

fun buildFastingUseCases(repository: FastingRepository) = FastingUseCases(
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

fun buildPrayerUseCases(repository: PrayerRepository) = PrayerUseCases(
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

fun buildHadithUseCases(repository: HadithRepository) = HadithUseCases(
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

fun buildDuaUseCases(repository: DuaRepository) = DuaUseCases(
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
