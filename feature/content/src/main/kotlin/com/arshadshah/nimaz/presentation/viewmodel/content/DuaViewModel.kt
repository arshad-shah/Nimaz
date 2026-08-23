package com.arshadshah.nimaz.presentation.viewmodel.content

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.PerfMonitor
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.core.monitoring.traceFirstEmission
import com.arshadshah.nimaz.domain.time.TodayProvider
import com.arshadshah.nimaz.core.common.toUtcMidnightMillis
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.domain.repository.settings.DuaDisplaySettings
import com.arshadshah.nimaz.domain.usecase.DuaUseCases
import com.arshadshah.nimaz.domain.usecase.TasbihUseCases
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorKind
import com.arshadshah.nimaz.presentation.theme.QuranArabicFont
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class DuaViewModel @Inject constructor(
    private val duaUseCases: DuaUseCases,
    // "Add to tasbih" writes a custom preset. The reader screen used to reach for
    // `TasbihViewModel` directly; see `DuaEvent.AddToTasbih`.
    private val tasbihUseCases: TasbihUseCases,
    private val duaSettings: DuaDisplaySettings,
    private val todayProvider: TodayProvider,
    private val telemetry: Telemetry
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
            is DuaEvent.LoadCategory -> {
                telemetry.featureUsed(DOMAIN, "open_category")
                loadCategory(event.categoryId)
            }

            is DuaEvent.LoadDua -> {
                telemetry.featureUsed(DOMAIN, "open_reader")
                loadDua(event.duaId)
            }

            is DuaEvent.LoadDuasByOccasion -> {
                telemetry.featureUsed(DOMAIN, "open_occasion")
                loadDuasByOccasion(event.occasion)
            }

            is DuaEvent.ToggleFavorite -> {
                telemetry.featureUsed(DOMAIN, "toggle_favorite")
                toggleFavorite(event.duaId, event.categoryId)
            }

            is DuaEvent.AddToTasbih -> {
                telemetry.featureUsed(DOMAIN, "add_to_tasbih")
                addToTasbih(event.dua)
            }

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
        launchSafely(
            telemetry,
            DOMAIN,
            "load_categories",
            onFailure = { throwable ->
                _collectionState.update {
                    it.copy(
                        isLoading = false,
                        error = UiError(
                            message = R.string.dua_collection_load_failed,
                            details = throwable.message,
                        ),
                    )
                }
            }
        ) {
            combine(
                duaUseCases.getAllCategories(),
                duaSettings.duaCategoriesSortAlphabetical
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

    /**
     * `settingChanged`, not `featureUsed`: this writes a persisted preference, so it belongs on
     * the settings dashboard beside every other one rather than in the dua feature's usage
     * counter, where "how many people prefer A–Z" was not answerable — the old event recorded
     * that the sort was toggled and not which way it landed.
     *
     * Recorded after the write for the same reason the prayer events are: a failed write is not
     * a setting change.
     */
    private fun toggleCategoriesSort() {
        val alphabetical = !_collectionState.value.sortAlphabetical
        launchSafely(telemetry, DOMAIN, "toggle_category_sort") {
            duaSettings.setDuaCategoriesSortAlphabetical(alphabetical)
            telemetry.settingChanged(
                "dua_categories_sort",
                if (alphabetical) "alphabetical" else "curated",
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
        categoryJob = launchSafely(
            telemetry,
            DOMAIN,
            "load_category",
            onFailure = { throwable ->
                _categoryState.update {
                    it.copy(
                        isLoading = false,
                        error = UiError(
                            message = R.string.dua_category_load_failed,
                            details = throwable.message,
                        ),
                    )
                }
            }
        ) {
            val category = duaUseCases.getCategoryById(categoryId)
            _categoryState.update { it.copy(category = category) }

            duaUseCases.getDuasByCategory(categoryId)
                .traceFirstEmission(telemetry, PerfMonitor.Traces.DUA_CHAPTER_LOAD)
                .collect { duas ->
                    _categoryState.update {
                        it.copy(duas = duas, isLoading = false)
                    }
                }
        }
    }

    private fun loadDua(duaId: String) {
        _readerState.update { it.copy(isLoading = true, error = null) }
        readerJob?.cancel()
        readerJob = launchSafely(
            telemetry,
            DOMAIN,
            "load_dua",
            onFailure = { failReader(R.string.error_generic) }
        ) {
            val dua = duaUseCases.getDuaById(duaId)
            if (dua == null) {
                // Clearing the list matters as much as setting the message: leaving the
                // previous dua's pages loaded meant a reader asked for a dua that does not
                // exist kept paging through the one before it, while its state said "not
                // found". The screen keys its not-found message off an empty list.
                failReader(R.string.dua_reader_not_found)
                return@launchSafely
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
        }
    }

    private fun failReader(@StringRes error: Int, kind: NimazErrorKind = NimazErrorKind.NOT_FOUND) {
        _readerState.update {
            it.copy(
                duas = emptyList(),
                initialIndex = 0,
                isLoading = false,
                error = UiError(message = error, kind = kind),
            )
        }
    }

    /**
     * Reactively mirrors the persisted dua reading preferences into the reader
     * state, exactly as the Quran reader observes its own prefs. Combined in two
     * groups of three because [combine] only has typed overloads up to five flows.
     */
    private fun observeDuaSettings() {
        launchSafely(telemetry, DOMAIN, "observe_settings") {
            val displayFlow = combine(
                duaSettings.duaArabicFont,
                duaSettings.duaArabicFontSize,
                duaSettings.duaTranslationFontSize
            ) { fontId, arabicSize, transSize -> Triple(fontId, arabicSize, transSize) }

            val toggleFlow = combine(
                duaSettings.duaShowArabic,
                duaSettings.duaShowTransliteration,
                duaSettings.duaShowTranslation
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
        categoryJob = launchSafely(
            telemetry,
            DOMAIN,
            "load_occasion",
            onFailure = { throwable ->
                _categoryState.update {
                    it.copy(
                        isLoading = false,
                        error = UiError(
                            message = R.string.dua_category_load_failed,
                            details = throwable.message,
                        ),
                    )
                }
            }
        ) {
            duaUseCases.getDuasByOccasion(occasion).collect { duas ->
                _categoryState.update {
                    it.copy(duas = duas, isLoading = false)
                }
            }
        }
    }

    /**
     * Saves a dua as a custom tasbih preset.
     *
     * The `Dua -> TasbihPreset` mapping lived in `DuaReaderScreen` as a private extension. It is
     * a mapping between two domain models with a truncation rule and three fallbacks in it —
     * decisions a screen should not be making, and ones nothing could test where it was.
     */
    private fun addToTasbih(dua: Dua) {
        launchSafely(telemetry, DOMAIN, "add_to_tasbih") {
            tasbihUseCases.insertPreset(dua.toTasbihPreset(System.currentTimeMillis()))
        }
    }

    private fun toggleFavorite(duaId: String, categoryId: String) {
        launchSafely(telemetry, DOMAIN, "toggle_favorite") {
            duaUseCases.toggleFavorite(duaId, categoryId)
        }
    }

    private fun loadFavorites() {
        launchSafely(
            telemetry,
            DOMAIN,
            "load_favorites",
            onFailure = { _favoritesState.update { it.copy(isLoading = false) } }
        ) {
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
        progressJob = launchSafely(
            telemetry,
            DOMAIN,
            "load_progress",
            onFailure = { _dailyProgressState.update { it.copy(isLoading = false) } }
        ) {
            duaUseCases.getProgressForDate(date).collect { progressList ->
                _dailyProgressState.update {
                    it.copy(progressList = progressList, isLoading = false)
                }
            }
        }
    }

    private fun getTodayEpoch(): Long {
        return todayProvider.today().toUtcMidnightMillis()
    }

    fun isDuaFavorite(duaId: String) = duaUseCases.isDuaFavorite(duaId)

    private companion object {
        private const val DOMAIN = AppAnalytics.Feature.DUA
    }
}

/**
 * A dua as a custom tasbih preset.
 *
 * `targetCount` falls back to 33 — the conventional tasbih count — when the dua carries no repeat
 * count, and the name falls back to the Arabic title when the English one is blank, so a preset is
 * never created nameless. The 40-character truncation keeps it readable in the tasbih list.
 *
 * `now` is a parameter rather than a `System.currentTimeMillis()` call inside, so the mapping is
 * testable. The caller still reads the wall clock directly: `TodayProvider` exposes `today()` and
 * `todayChanges` but no millis accessor, and widening that seam for a `createdAt` stamp is a
 * change to make deliberately rather than in passing. Same clock the screen used before.
 */
internal fun Dua.toTasbihPreset(now: Long): TasbihPreset {
    val presetName = titleEnglish.trim().let {
        if (it.length > 40) it.take(40).trimEnd() + "\u2026" else it
    }
    return TasbihPreset(
        id = 0,
        name = presetName.ifBlank { titleArabic.trim() },
        arabicText = textArabic.ifBlank { null },
        transliteration = textTransliteration?.ifBlank { null },
        translation = textEnglish.ifBlank { null },
        targetCount = repeatCount?.takeIf { it > 0 } ?: 33,
        category = TasbihCategory.CUSTOM,
        reference = reference?.ifBlank { null },
        isDefault = false,
        displayOrder = 0,
        createdAt = now,
        updatedAt = now,
    )
}
