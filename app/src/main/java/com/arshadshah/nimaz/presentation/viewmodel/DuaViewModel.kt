package com.arshadshah.nimaz.presentation.viewmodel

import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.util.toUtcMidnightMillis
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.arshadshah.nimaz.domain.model.DuaProgress
import com.arshadshah.nimaz.domain.model.DuaSearchResult
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.DuaUseCases
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.arshadshah.nimaz.presentation.theme.QuranArabicFont
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DuaCollectionUiState(
    val categories: List<DuaCategory> = emptyList(),
    val filteredCategories: List<DuaCategory> = emptyList(),
    val searchQuery: String = "",
    val sortAlphabetical: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class DuaCategoryUiState(
    val category: DuaCategory? = null,
    val duas: List<Dua> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class DuaReaderUiState(
    val duas: List<Dua> = emptyList(),
    val initialIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showArabic: Boolean = true,
    val showTransliteration: Boolean = true,
    val showTranslation: Boolean = true,
    val fontSize: Float = 16f,
    val arabicFontSize: Float = 28f,
    val arabicFontFamily: FontFamily = AmiriFontFamily,
    val selectedArabicFontId: String = "amiri"
)

data class DuaSearchUiState(
    val query: String = "",
    val results: List<DuaSearchResult> = emptyList(),
    val isSearching: Boolean = false
)

data class DuaFavoritesUiState(
    val favorites: List<DuaBookmark> = emptyList(),
    val isLoading: Boolean = true
)

data class DuaDailyProgressUiState(
    val progressList: List<DuaProgress> = emptyList(),
    val date: Long = System.currentTimeMillis(),
    val isLoading: Boolean = true
)

sealed interface DuaEvent {
    data class LoadCategory(val categoryId: String) : DuaEvent
    data class LoadDua(val duaId: String) : DuaEvent
    data class LoadDuasByOccasion(val occasion: DuaOccasion) : DuaEvent
    data class Search(val query: String) : DuaEvent
    data class SearchCategories(val query: String) : DuaEvent
    data class ToggleFavorite(val duaId: String, val categoryId: String) : DuaEvent
    data class SetFontSize(val size: Float) : DuaEvent
    data class SetArabicFontSize(val size: Float) : DuaEvent
    data class LoadProgressForDate(val date: Long) : DuaEvent
    data object ToggleArabic : DuaEvent
    data object ToggleTransliteration : DuaEvent
    data object ToggleTranslation : DuaEvent
    data object ToggleCategoriesSort : DuaEvent
    data object ClearSearch : DuaEvent
    data object LoadAllCategories : DuaEvent
    data object LoadFavorites : DuaEvent
    data object LoadTodayProgress : DuaEvent
}

@HiltViewModel
class DuaViewModel @Inject constructor(
    private val duaUseCases: DuaUseCases,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _collectionState = MutableStateFlow(DuaCollectionUiState())
    val collectionState: StateFlow<DuaCollectionUiState> = _collectionState.asStateFlow()

    private val _categoryState = MutableStateFlow(DuaCategoryUiState())
    val categoryState: StateFlow<DuaCategoryUiState> = _categoryState.asStateFlow()

    private val _readerState = MutableStateFlow(DuaReaderUiState())
    val readerState: StateFlow<DuaReaderUiState> = _readerState.asStateFlow()

    private val _searchState = MutableStateFlow(DuaSearchUiState())
    val searchState: StateFlow<DuaSearchUiState> = _searchState.asStateFlow()

    private val _favoritesState = MutableStateFlow(DuaFavoritesUiState())
    val favoritesState: StateFlow<DuaFavoritesUiState> = _favoritesState.asStateFlow()

    private val _dailyProgressState = MutableStateFlow(DuaDailyProgressUiState())
    val dailyProgressState: StateFlow<DuaDailyProgressUiState> = _dailyProgressState.asStateFlow()

    // These loaders all collect Room flows, which never complete, and each is re-invoked as
    // the user navigates or types. Without a handle to cancel, every category opened, dua
    // read and keystroke typed left a live collector writing the same state for the rest of
    // the ViewModel's life — and Room re-emits to *all* of them on any table change, so an
    // earlier one could land last and replace what is on screen.
    //
    // One handle per *surface*, not per function: `loadCategory` and `loadDuasByOccasion`
    // both fill `_categoryState`, so they share `categoryJob` — an occasion list and a
    // category list are the same surface and must not be live at once.
    // (AP-7.1b in docs/CLEAN_ARCHITECTURE_CHECKLIST.md.)
    private var categoryJob: Job? = null
    private var readerJob: Job? = null
    private var searchJob: Job? = null
    private var progressJob: Job? = null

    init {
        loadAllCategories()
        loadFavorites()
        loadTodayProgress()
        observeDuaSettings()
    }

    fun onEvent(event: DuaEvent) {
        when (event) {
            is DuaEvent.LoadCategory -> AppAnalytics.logFeatureUsed("dua", "open_category")
            is DuaEvent.LoadDua -> AppAnalytics.logFeatureUsed("dua", "open_reader")
            is DuaEvent.LoadDuasByOccasion -> AppAnalytics.logFeatureUsed("dua", "open_occasion")
            is DuaEvent.Search -> AppAnalytics.logFeatureUsed("dua", "search")
            is DuaEvent.ToggleFavorite -> AppAnalytics.logFeatureUsed("dua", "toggle_favorite")
            else -> {}
        }
        when (event) {
            is DuaEvent.LoadCategory -> loadCategory(event.categoryId)
            is DuaEvent.LoadDua -> loadDua(event.duaId)
            is DuaEvent.LoadDuasByOccasion -> loadDuasByOccasion(event.occasion)
            is DuaEvent.Search -> search(event.query)
            is DuaEvent.SearchCategories -> searchCategories(event.query)
            is DuaEvent.ToggleFavorite -> toggleFavorite(event.duaId, event.categoryId)
            is DuaEvent.SetFontSize -> _readerState.update { it.copy(fontSize = event.size) }
            is DuaEvent.SetArabicFontSize -> _readerState.update { it.copy(arabicFontSize = event.size) }
            is DuaEvent.LoadProgressForDate -> loadProgressForDate(event.date)
            DuaEvent.ToggleArabic -> _readerState.update { it.copy(showArabic = !it.showArabic) }
            DuaEvent.ToggleTransliteration -> _readerState.update { it.copy(showTransliteration = !it.showTransliteration) }
            DuaEvent.ToggleTranslation -> _readerState.update { it.copy(showTranslation = !it.showTranslation) }
            DuaEvent.ToggleCategoriesSort -> toggleCategoriesSort()
            DuaEvent.ClearSearch -> {
                searchJob?.cancel()
                _searchState.update { DuaSearchUiState() }
                _collectionState.update {
                    it.copy(
                        searchQuery = "",
                        filteredCategories = filterAndSortCategories(
                            it.categories,
                            query = "",
                            alphabetical = it.sortAlphabetical
                        )
                    )
                }
            }

            DuaEvent.LoadAllCategories -> loadAllCategories()
            DuaEvent.LoadFavorites -> loadFavorites()
            DuaEvent.LoadTodayProgress -> loadTodayProgress()
        }
    }

    private fun loadAllCategories() {
        viewModelScope.launch {
            combine(
                duaUseCases.getAllCategories(),
                settingsRepository.duaCategoriesSortAlphabetical
            ) { categories, alphabetical -> categories to alphabetical }
                .collect { (categories, alphabetical) ->
                    _collectionState.update {
                        it.copy(
                            categories = categories,
                            filteredCategories = filterAndSortCategories(
                                categories,
                                it.searchQuery,
                                alphabetical
                            ),
                            sortAlphabetical = alphabetical,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun toggleCategoriesSort() {
        AppAnalytics.logFeatureUsed("dua", "toggle_category_sort")
        viewModelScope.launch {
            settingsRepository.setDuaCategoriesSortAlphabetical(
                !_collectionState.value.sortAlphabetical
            )
        }
    }

    /**
     * Applies the active category search filter, then orders the result either
     * alphabetically (by English name) or by the curated [DuaCategory.displayOrder].
     */
    private fun filterAndSortCategories(
        categories: List<DuaCategory>,
        query: String,
        alphabetical: Boolean
    ): List<DuaCategory> {
        val filtered = if (query.isBlank()) {
            categories
        } else {
            categories.filter { category ->
                category.nameEnglish.contains(query, ignoreCase = true) ||
                        category.nameArabic.contains(query) ||
                        category.description?.contains(query, ignoreCase = true) == true
            }
        }
        return if (alphabetical) {
            filtered.sortedBy { it.nameEnglish.lowercase() }
        } else {
            filtered.sortedBy { it.displayOrder }
        }
    }

    private fun loadCategory(categoryId: String) {
        _categoryState.update { it.copy(isLoading = true, error = null) }
        categoryJob?.cancel()
        categoryJob = viewModelScope.launch {
            try {
                val category = duaUseCases.getCategoryById(categoryId)
                _categoryState.update { it.copy(category = category) }

                duaUseCases.getDuasByCategory(categoryId).collect { duas ->
                    _categoryState.update {
                        it.copy(duas = duas, isLoading = false)
                    }
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                AppAnalytics.logError("dua", "load_category", e.message)
                _categoryState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun loadDua(duaId: String) {
        _readerState.update { it.copy(isLoading = true, error = null) }
        readerJob?.cancel()
        readerJob = viewModelScope.launch {
            try {
                val dua = duaUseCases.getDuaById(duaId)
                if (dua == null) {
                    _readerState.update { it.copy(isLoading = false, error = "Dua not found") }
                    return@launch
                }
                // Load the sibling duas in this collection so the reader can page
                // through them with a HorizontalPager.
                duaUseCases.getDuasByCategory(dua.categoryId).collect { categoryDuas ->
                    val list = categoryDuas.ifEmpty { listOf(dua) }
                    val index = list.indexOfFirst { it.id == duaId }.coerceAtLeast(0)
                    _readerState.update {
                        it.copy(duas = list, initialIndex = index, isLoading = false)
                    }
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                AppAnalytics.logError("dua", "load_dua", e.message)
                _readerState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    /**
     * Reactively mirrors the persisted dua reading preferences into the reader
     * state, exactly as the Quran reader observes its own prefs. Combined in two
     * groups of three because [combine] only has typed overloads up to five flows.
     */
    private fun observeDuaSettings() {
        viewModelScope.launch {
            val displayFlow = combine(
                settingsRepository.duaArabicFont,
                settingsRepository.duaArabicFontSize,
                settingsRepository.duaTranslationFontSize
            ) { fontId, arabicSize, transSize -> Triple(fontId, arabicSize, transSize) }

            val toggleFlow = combine(
                settingsRepository.duaShowArabic,
                settingsRepository.duaShowTransliteration,
                settingsRepository.duaShowTranslation
            ) { showArabic, showTranslit, showTrans -> Triple(showArabic, showTranslit, showTrans) }

            combine(displayFlow, toggleFlow) { display, toggles -> display to toggles }
                .collect { (display, toggles) ->
                    val (fontId, arabicSize, transSize) = display
                    val (showArabic, showTranslit, showTrans) = toggles
                    _readerState.update {
                        it.copy(
                            selectedArabicFontId = fontId,
                            arabicFontSize = arabicSize,
                            fontSize = transSize,
                            showArabic = showArabic,
                            showTransliteration = showTranslit,
                            showTranslation = showTrans,
                            arabicFontFamily = QuranArabicFont.fromId(fontId).fontFamily
                        )
                    }
                }
        }
    }

    private fun loadDuasByOccasion(occasion: DuaOccasion) {
        _categoryState.update { it.copy(isLoading = true, error = null) }
        categoryJob?.cancel()
        categoryJob = viewModelScope.launch {
            duaUseCases.getDuasByOccasion(occasion).collect { duas ->
                _categoryState.update {
                    it.copy(duas = duas, isLoading = false)
                }
            }
        }
    }

    private fun search(query: String) {
        if (query.isBlank()) {
            // The last non-empty query's collector is still live otherwise, and its next
            // emission repopulates the results the user just cleared.
            searchJob?.cancel()
            _searchState.update { DuaSearchUiState() }
            return
        }

        _searchState.update { it.copy(query = query, isSearching = true) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            duaUseCases.searchDuas(query).collect { results ->
                _searchState.update { it.copy(results = results, isSearching = false) }
            }
        }
    }

    private fun searchCategories(query: String) {
        _collectionState.update { state ->
            state.copy(
                searchQuery = query,
                filteredCategories = filterAndSortCategories(
                    state.categories,
                    query,
                    state.sortAlphabetical
                )
            )
        }
    }

    private fun toggleFavorite(duaId: String, categoryId: String) {
        viewModelScope.launch {
            duaUseCases.toggleFavorite(duaId, categoryId)
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            duaUseCases.getFavoriteDuas().collect { favorites ->
                _favoritesState.update { it.copy(favorites = favorites, isLoading = false) }
            }
        }
    }

    private fun loadTodayProgress() {
        val todayEpoch = getTodayEpoch()
        loadProgressForDate(todayEpoch)
    }

    private fun loadProgressForDate(date: Long) {
        _dailyProgressState.update { it.copy(isLoading = true, date = date) }
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            duaUseCases.getProgressForDate(date).collect { progressList ->
                _dailyProgressState.update {
                    it.copy(progressList = progressList, isLoading = false)
                }
            }
        }
    }

    private fun getTodayEpoch(): Long {
        return LocalDate.now().toUtcMidnightMillis()
    }

    fun isDuaFavorite(duaId: String) = duaUseCases.isDuaFavorite(duaId)
}
