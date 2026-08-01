package com.arshadshah.nimaz.presentation.viewmodel

import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithBook
import com.arshadshah.nimaz.domain.model.HadithBookmark
import com.arshadshah.nimaz.domain.model.HadithChapter
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.domain.model.HadithSearchResult
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
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
import javax.inject.Inject

data class HadithCollectionUiState(
    val books: List<HadithBook> = emptyList(),
    val hadithOfTheDay: Hadith? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class HadithChaptersUiState(
    val book: HadithBook? = null,
    val chapters: List<HadithChapter> = emptyList(),
    val filteredChapters: List<HadithChapter> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

data class HadithReaderUiState(
    val chapter: HadithChapter? = null,
    val hadiths: List<Hadith> = emptyList(),
    val currentHadithIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showArabic: Boolean = true,
    val showTranslation: Boolean = true,
    val showGrade: Boolean = true,
    val showChain: Boolean = true,
    val fontSize: Float = 16f,
    val arabicFontSize: Float = 24f,
    val arabicFontFamily: FontFamily = AmiriFontFamily,
    val selectedArabicFontId: String = "amiri"
)

data class HadithSearchUiState(
    val query: String = "",
    val results: List<HadithSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val selectedBookId: String? = null
)

data class HadithBookmarksUiState(
    val bookmarks: List<HadithBookmark> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface HadithEvent {
    data class LoadBook(val bookId: String) : HadithEvent
    data class LoadChapter(val chapterId: String) : HadithEvent
    data class LoadHadithById(val hadithId: String) : HadithEvent
    data class LoadHadithByNumber(val bookId: String, val hadithNumber: Int) : HadithEvent
    data class Search(val query: String) : HadithEvent
    data class SearchInBook(val bookId: String, val query: String) : HadithEvent
    data class SearchChapters(val query: String) : HadithEvent
    data class FilterByGrade(val grade: HadithGrade) : HadithEvent
    data class ToggleBookmark(val hadithId: String, val bookId: String, val hadithNumber: Int) :
        HadithEvent

    data class NavigateToHadith(val index: Int) : HadithEvent
    data class SetFontSize(val size: Float) : HadithEvent
    data class SetArabicFontSize(val size: Float) : HadithEvent
    data object ToggleArabic : HadithEvent
    data object ClearSearch : HadithEvent
    data object LoadAllBooks : HadithEvent
    data object LoadBookmarks : HadithEvent
}

@HiltViewModel
class HadithViewModel @Inject constructor(
    private val hadithUseCases: HadithUseCases,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _collectionState = MutableStateFlow(HadithCollectionUiState())
    val collectionState: StateFlow<HadithCollectionUiState> = _collectionState.asStateFlow()

    private val _chaptersState = MutableStateFlow(HadithChaptersUiState())
    val chaptersState: StateFlow<HadithChaptersUiState> = _chaptersState.asStateFlow()

    private val _readerState = MutableStateFlow(HadithReaderUiState())
    val readerState: StateFlow<HadithReaderUiState> = _readerState.asStateFlow()

    private val _searchState = MutableStateFlow(HadithSearchUiState())
    val searchState: StateFlow<HadithSearchUiState> = _searchState.asStateFlow()

    private val _bookmarksState = MutableStateFlow(HadithBookmarksUiState())
    val bookmarksState: StateFlow<HadithBookmarksUiState> = _bookmarksState.asStateFlow()

    // Each of these collects a Room Flow, and a Room Flow never completes — so a bare
    // `viewModelScope.launch { flow.collect { … } }` with no handle leaks a collector per
    // invocation, all of them writing the same state for the lifetime of the ViewModel.
    // Search re-runs per keystroke (so an earlier, slower query could land last and win),
    // and the chapter/reader loaders re-run per navigation. One handle per *identity of the
    // request*: the reader shows one chapter at a time, so one handle each is correct here.
    // (AP-7.1b in docs/CLEAN_ARCHITECTURE_CHECKLIST.md — same defect as the Quran reader's.)
    private var searchJob: Job? = null
    private var chaptersJob: Job? = null
    private var readerJob: Job? = null

    init {
        loadAllBooks()
        loadBookmarks()
        loadHadithOfTheDay()
        observeHadithSettings()
    }

    fun onEvent(event: HadithEvent) {
        when (event) {
            is HadithEvent.LoadBook -> AppAnalytics.logFeatureUsed("hadith", "open_book")
            is HadithEvent.LoadChapter -> AppAnalytics.logFeatureUsed("hadith", "open_reader")
            is HadithEvent.LoadHadithById -> AppAnalytics.logFeatureUsed("hadith", "open_hadith")
            is HadithEvent.LoadHadithByNumber -> AppAnalytics.logFeatureUsed(
                "hadith",
                "open_hadith"
            )

            is HadithEvent.Search -> AppAnalytics.logFeatureUsed("hadith", "search")
            is HadithEvent.SearchInBook -> AppAnalytics.logFeatureUsed("hadith", "search_in_book")
            is HadithEvent.FilterByGrade -> AppAnalytics.logFeatureUsed("hadith", "filter_by_grade")
            is HadithEvent.ToggleBookmark -> AppAnalytics.logFeatureUsed(
                "hadith",
                "toggle_bookmark"
            )

            else -> {}
        }
        when (event) {
            is HadithEvent.LoadBook -> loadBook(event.bookId)
            is HadithEvent.LoadChapter -> loadChapter(event.chapterId)
            is HadithEvent.LoadHadithById -> loadHadithById(event.hadithId)
            is HadithEvent.LoadHadithByNumber -> loadHadithByNumber(
                event.bookId,
                event.hadithNumber
            )

            is HadithEvent.Search -> search(event.query)
            is HadithEvent.SearchInBook -> searchInBook(event.bookId, event.query)
            is HadithEvent.SearchChapters -> searchChapters(event.query)
            is HadithEvent.FilterByGrade -> filterByGrade(event.grade)
            is HadithEvent.ToggleBookmark -> toggleBookmark(
                event.hadithId,
                event.bookId,
                event.hadithNumber
            )

            is HadithEvent.NavigateToHadith -> _readerState.update { it.copy(currentHadithIndex = event.index) }
            is HadithEvent.SetFontSize -> _readerState.update { it.copy(fontSize = event.size) }
            is HadithEvent.SetArabicFontSize -> _readerState.update { it.copy(arabicFontSize = event.size) }
            HadithEvent.ToggleArabic -> _readerState.update { it.copy(showArabic = !it.showArabic) }
            HadithEvent.ClearSearch -> {
                searchJob?.cancel()
                _searchState.update { HadithSearchUiState() }
                _chaptersState.update { it.copy(searchQuery = "", filteredChapters = it.chapters) }
            }

            HadithEvent.LoadAllBooks -> loadAllBooks()
            HadithEvent.LoadBookmarks -> loadBookmarks()
        }
    }

    private fun loadAllBooks() {
        viewModelScope.launch {
            hadithUseCases.getAllBooks().collect { books ->
                _collectionState.update {
                    it.copy(books = books, isLoading = false)
                }
            }
        }
    }

    private fun loadHadithOfTheDay() {
        viewModelScope.launch {
            val hadith = hadithUseCases.getHadithOfTheDay()
            _collectionState.update { it.copy(hadithOfTheDay = hadith) }
        }
    }

    private fun loadBook(bookId: String) {
        _chaptersState.update { it.copy(isLoading = true, error = null) }
        chaptersJob?.cancel()
        chaptersJob = viewModelScope.launch {
            try {
                val book = hadithUseCases.getBookById(bookId)
                _chaptersState.update { it.copy(book = book) }

                hadithUseCases.getChaptersByBook(bookId).collect { chapters ->
                    _chaptersState.update { state ->
                        state.copy(
                            chapters = chapters,
                            filteredChapters = chapters,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                AppAnalytics.logError("hadith", "load_book", e.message)
                _chaptersState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun loadChapter(chapterId: String) {
        _readerState.update { it.copy(isLoading = true, error = null) }
        readerJob?.cancel()
        readerJob = viewModelScope.launch {
            try {
                val chapter = hadithUseCases.getChapterById(chapterId)
                _readerState.update { it.copy(chapter = chapter) }

                hadithUseCases.getHadithsByChapter(chapterId).collect { hadiths ->
                    _readerState.update {
                        it.copy(hadiths = hadiths, isLoading = false, currentHadithIndex = 0)
                    }
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                AppAnalytics.logError("hadith", "load_chapter", e.message)
                _readerState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun loadHadithById(hadithId: String) {
        _readerState.update { it.copy(isLoading = true, error = null) }
        readerJob?.cancel()
        readerJob = viewModelScope.launch {
            try {
                val hadith = hadithUseCases.getHadithById(hadithId)
                if (hadith != null) {
                    // Load the chapter containing this hadith to get context
                    val chapterId = "${hadith.bookId}_${hadith.chapterId}"
                    val chapter = hadithUseCases.getChapterById(chapterId)

                    // Get all hadiths in this chapter
                    hadithUseCases.getHadithsByChapter(chapterId).collect { hadiths ->
                        // Find the index of the target hadith
                        val index = hadiths.indexOfFirst { it.id == hadithId }
                        _readerState.update {
                            it.copy(
                                chapter = chapter,
                                hadiths = hadiths,
                                currentHadithIndex = if (index >= 0) index else 0,
                                isLoading = false
                            )
                        }
                    }
                } else {
                    _readerState.update { it.copy(error = "Hadith not found", isLoading = false) }
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                AppAnalytics.logError("hadith", "load_hadith_by_id", e.message)
                _readerState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun loadHadithByNumber(bookId: String, hadithNumber: Int) {
        viewModelScope.launch {
            try {
                val hadith = hadithUseCases.getHadithByNumber(bookId, hadithNumber)
                hadith?.let {
                    // Load the chapter containing this hadith
                    loadChapter(it.chapterId)
                    // Find the index in the list
                    val index = _readerState.value.hadiths.indexOfFirst { h -> h.id == it.id }
                    if (index >= 0) {
                        _readerState.update { state -> state.copy(currentHadithIndex = index) }
                    }
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                AppAnalytics.logError("hadith", "load_hadith_by_number", e.message)
                _readerState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun search(query: String) {
        if (query.isBlank()) {
            // Without this cancel, the collector for the last non-empty query is still live
            // and its next emission repopulates the results the user just cleared.
            searchJob?.cancel()
            _searchState.update { HadithSearchUiState() }
            return
        }

        _searchState.update { it.copy(query = query, isSearching = true) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            hadithUseCases.searchHadiths(query).collect { results ->
                _searchState.update { it.copy(results = results, isSearching = false) }
            }
        }
    }

    private fun searchInBook(bookId: String, query: String) {
        if (query.isBlank()) {
            searchJob?.cancel()
            _searchState.update { HadithSearchUiState() }
            return
        }

        _searchState.update { it.copy(query = query, selectedBookId = bookId, isSearching = true) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            hadithUseCases.searchHadithsInBook(bookId, query).collect { results ->
                _searchState.update { it.copy(results = results, isSearching = false) }
            }
        }
    }

    private fun searchChapters(query: String) {
        _chaptersState.update { state ->
            val filtered = if (query.isBlank()) {
                state.chapters
            } else {
                state.chapters.filter { chapter ->
                    chapter.nameEnglish.contains(query, ignoreCase = true) ||
                            chapter.nameArabic.contains(query)
                }
            }
            state.copy(searchQuery = query, filteredChapters = filtered)
        }
    }

    private fun filterByGrade(grade: HadithGrade) {
        readerJob?.cancel()
        readerJob = viewModelScope.launch {
            hadithUseCases.getHadithsByGrade(grade).collect { hadiths ->
                _readerState.update { it.copy(hadiths = hadiths) }
            }
        }
    }

    private fun toggleBookmark(hadithId: String, bookId: String, hadithNumber: Int) {
        viewModelScope.launch {
            hadithUseCases.toggleBookmark(hadithId, bookId, hadithNumber)
        }
    }

    private fun loadBookmarks() {
        viewModelScope.launch {
            hadithUseCases.getAllBookmarks().collect { bookmarks ->
                _bookmarksState.update { it.copy(bookmarks = bookmarks, isLoading = false) }
            }
        }
    }

    fun isHadithBookmarked(hadithId: String) = hadithUseCases.isHadithBookmarked(hadithId)

    /**
     * Reactively mirrors the persisted hadith reading preferences into the reader
     * state, exactly as the Quran/Dua readers observe their own prefs.
     */
    private fun observeHadithSettings() {
        viewModelScope.launch {
            val displayFlow = combine(
                settingsRepository.hadithArabicFont,
                settingsRepository.hadithArabicFontSize,
                settingsRepository.hadithTranslationFontSize
            ) { fontId, arabicSize, transSize -> Triple(fontId, arabicSize, transSize) }

            val toggleFlow = combine(
                settingsRepository.hadithShowArabic,
                settingsRepository.hadithShowTranslation,
                settingsRepository.hadithShowGrade,
                settingsRepository.hadithShowChain
            ) { showArabic, showTranslation, showGrade, showChain ->
                HadithToggles(showArabic, showTranslation, showGrade, showChain)
            }

            combine(displayFlow, toggleFlow) { display, toggles -> display to toggles }
                .collect { (display, toggles) ->
                    val (fontId, arabicSize, transSize) = display
                    _readerState.update {
                        it.copy(
                            selectedArabicFontId = fontId,
                            arabicFontSize = arabicSize,
                            fontSize = transSize,
                            showArabic = toggles.showArabic,
                            showTranslation = toggles.showTranslation,
                            showGrade = toggles.showGrade,
                            showChain = toggles.showChain,
                            arabicFontFamily = QuranArabicFont.fromId(fontId).fontFamily
                        )
                    }
                }
        }
    }

    private data class HadithToggles(
        val showArabic: Boolean,
        val showTranslation: Boolean,
        val showGrade: Boolean,
        val showChain: Boolean
    )
}
