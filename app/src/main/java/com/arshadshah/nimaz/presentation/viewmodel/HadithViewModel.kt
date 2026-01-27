package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithBook
import com.arshadshah.nimaz.domain.model.HadithBookmark
import com.arshadshah.nimaz.domain.model.HadithChapter
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.domain.model.HadithSearchResult
import com.arshadshah.nimaz.domain.repository.HadithRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HadithCollectionUiState(
    val books: List<HadithBook> = emptyList(),
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
    val fontSize: Float = 16f,
    val arabicFontSize: Float = 24f
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
    data class LoadHadithByNumber(val bookId: String, val hadithNumber: Int) : HadithEvent
    data class Search(val query: String) : HadithEvent
    data class SearchInBook(val bookId: String, val query: String) : HadithEvent
    data class SearchChapters(val query: String) : HadithEvent
    data class FilterByGrade(val grade: HadithGrade) : HadithEvent
    data class ToggleBookmark(val hadithId: String, val bookId: String, val hadithNumber: Int) : HadithEvent
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
    private val hadithRepository: HadithRepository
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

    init {
        loadAllBooks()
        loadBookmarks()
    }

    fun onEvent(event: HadithEvent) {
        when (event) {
            is HadithEvent.LoadBook -> loadBook(event.bookId)
            is HadithEvent.LoadChapter -> loadChapter(event.chapterId)
            is HadithEvent.LoadHadithByNumber -> loadHadithByNumber(event.bookId, event.hadithNumber)
            is HadithEvent.Search -> search(event.query)
            is HadithEvent.SearchInBook -> searchInBook(event.bookId, event.query)
            is HadithEvent.SearchChapters -> searchChapters(event.query)
            is HadithEvent.FilterByGrade -> filterByGrade(event.grade)
            is HadithEvent.ToggleBookmark -> toggleBookmark(event.hadithId, event.bookId, event.hadithNumber)
            is HadithEvent.NavigateToHadith -> _readerState.update { it.copy(currentHadithIndex = event.index) }
            is HadithEvent.SetFontSize -> _readerState.update { it.copy(fontSize = event.size) }
            is HadithEvent.SetArabicFontSize -> _readerState.update { it.copy(arabicFontSize = event.size) }
            HadithEvent.ToggleArabic -> _readerState.update { it.copy(showArabic = !it.showArabic) }
            HadithEvent.ClearSearch -> {
                _searchState.update { HadithSearchUiState() }
                _chaptersState.update { it.copy(searchQuery = "", filteredChapters = it.chapters) }
            }
            HadithEvent.LoadAllBooks -> loadAllBooks()
            HadithEvent.LoadBookmarks -> loadBookmarks()
        }
    }

    private fun loadAllBooks() {
        viewModelScope.launch {
            hadithRepository.getAllBooks().collect { books ->
                _collectionState.update {
                    it.copy(books = books, isLoading = false)
                }
            }
        }
    }

    private fun loadBook(bookId: String) {
        _chaptersState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val book = hadithRepository.getBookById(bookId)
                _chaptersState.update { it.copy(book = book) }

                hadithRepository.getChaptersByBook(bookId).collect { chapters ->
                    _chaptersState.update { state ->
                        state.copy(
                            chapters = chapters,
                            filteredChapters = chapters,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _chaptersState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun loadChapter(chapterId: String) {
        _readerState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val chapter = hadithRepository.getChapterById(chapterId)
                _readerState.update { it.copy(chapter = chapter) }

                hadithRepository.getHadithsByChapter(chapterId).collect { hadiths ->
                    _readerState.update {
                        it.copy(hadiths = hadiths, isLoading = false, currentHadithIndex = 0)
                    }
                }
            } catch (e: Exception) {
                _readerState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun loadHadithByNumber(bookId: String, hadithNumber: Int) {
        viewModelScope.launch {
            try {
                val hadith = hadithRepository.getHadithByNumber(bookId, hadithNumber)
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
                _readerState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun search(query: String) {
        if (query.isBlank()) {
            _searchState.update { HadithSearchUiState() }
            return
        }

        _searchState.update { it.copy(query = query, isSearching = true) }
        viewModelScope.launch {
            hadithRepository.searchHadiths(query).collect { results ->
                _searchState.update { it.copy(results = results, isSearching = false) }
            }
        }
    }

    private fun searchInBook(bookId: String, query: String) {
        if (query.isBlank()) {
            _searchState.update { HadithSearchUiState() }
            return
        }

        _searchState.update { it.copy(query = query, selectedBookId = bookId, isSearching = true) }
        viewModelScope.launch {
            hadithRepository.searchHadithsInBook(bookId, query).collect { results ->
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
        viewModelScope.launch {
            hadithRepository.getHadithsByGrade(grade).collect { hadiths ->
                _readerState.update { it.copy(hadiths = hadiths) }
            }
        }
    }

    private fun toggleBookmark(hadithId: String, bookId: String, hadithNumber: Int) {
        viewModelScope.launch {
            hadithRepository.toggleBookmark(hadithId, bookId, hadithNumber)
        }
    }

    private fun loadBookmarks() {
        viewModelScope.launch {
            hadithRepository.getAllBookmarks().collect { bookmarks ->
                _bookmarksState.update { it.copy(bookmarks = bookmarks, isLoading = false) }
            }
        }
    }

    fun isHadithBookmarked(hadithId: String) = hadithRepository.isHadithBookmarked(hadithId)
}
