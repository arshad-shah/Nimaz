package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.HadithBookmark
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.repository.DuaRepository
import com.arshadshah.nimaz.domain.repository.HadithRepository
import com.arshadshah.nimaz.domain.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UnifiedBookmark(
    val id: String,
    val type: BookmarkType,
    val title: String,
    val subtitle: String,
    val arabicText: String?,
    val createdAt: Long,
    val note: String?,
    val color: String?,
    // Navigation data
    val surahNumber: Int? = null,
    val ayahNumber: Int? = null,
    val hadithBookId: String? = null,
    val hadithNumber: Int? = null,
    val duaId: String? = null,
    val categoryId: String? = null
)

enum class BookmarkType {
    QURAN, HADITH, DUA
}

enum class BookmarkSortOrder {
    DATE_NEWEST, DATE_OLDEST, TYPE, ALPHABETICAL
}

data class BookmarksUiState(
    val allBookmarks: List<UnifiedBookmark> = emptyList(),
    val filteredBookmarks: List<UnifiedBookmark> = emptyList(),
    val quranBookmarks: List<QuranBookmark> = emptyList(),
    val hadithBookmarks: List<HadithBookmark> = emptyList(),
    val duaBookmarks: List<DuaBookmark> = emptyList(),
    val selectedFilter: BookmarkType? = null,
    val sortOrder: BookmarkSortOrder = BookmarkSortOrder.DATE_NEWEST,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

data class BookmarkStatsUiState(
    val totalBookmarks: Int = 0,
    val quranCount: Int = 0,
    val hadithCount: Int = 0,
    val duaCount: Int = 0
)

sealed interface BookmarksEvent {
    data class SetFilter(val type: BookmarkType?) : BookmarksEvent
    data class SetSortOrder(val order: BookmarkSortOrder) : BookmarksEvent
    data class Search(val query: String) : BookmarksEvent
    data class DeleteQuranBookmark(val ayahId: Int) : BookmarksEvent
    data class DeleteHadithBookmark(val hadithId: String) : BookmarksEvent
    data class DeleteDuaBookmark(val duaId: String) : BookmarksEvent
    data class UpdateQuranBookmarkNote(val bookmark: QuranBookmark) : BookmarksEvent
    data class UpdateHadithBookmarkNote(val bookmark: HadithBookmark) : BookmarksEvent
    data object ClearSearch : BookmarksEvent
    data object RefreshAll : BookmarksEvent
    data object ClearAllBookmarks : BookmarksEvent
}

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val hadithRepository: HadithRepository,
    private val duaRepository: DuaRepository
) : ViewModel() {

    private val _bookmarksState = MutableStateFlow(BookmarksUiState())
    val bookmarksState: StateFlow<BookmarksUiState> = _bookmarksState.asStateFlow()

    private val _statsState = MutableStateFlow(BookmarkStatsUiState())
    val statsState: StateFlow<BookmarkStatsUiState> = _statsState.asStateFlow()

    init {
        loadAllBookmarks()
    }

    fun onEvent(event: BookmarksEvent) {
        when (event) {
            is BookmarksEvent.SetFilter -> setFilter(event.type)
            is BookmarksEvent.SetSortOrder -> setSortOrder(event.order)
            is BookmarksEvent.Search -> search(event.query)
            is BookmarksEvent.DeleteQuranBookmark -> deleteQuranBookmark(event.ayahId)
            is BookmarksEvent.DeleteHadithBookmark -> deleteHadithBookmark(event.hadithId)
            is BookmarksEvent.DeleteDuaBookmark -> deleteDuaBookmark(event.duaId)
            is BookmarksEvent.UpdateQuranBookmarkNote -> updateQuranBookmarkNote(event.bookmark)
            is BookmarksEvent.UpdateHadithBookmarkNote -> updateHadithBookmarkNote(event.bookmark)
            BookmarksEvent.ClearSearch -> clearSearch()
            BookmarksEvent.RefreshAll -> loadAllBookmarks()
            BookmarksEvent.ClearAllBookmarks -> clearAllBookmarks()
        }
    }

    private fun loadAllBookmarks() {
        loadQuranBookmarks()
        loadHadithBookmarks()
        loadDuaBookmarks()
    }

    private fun loadQuranBookmarks() {
        viewModelScope.launch {
            quranRepository.getAllBookmarks().collect { bookmarks ->
                _bookmarksState.update { state ->
                    val unified = state.allBookmarks.filter { it.type != BookmarkType.QURAN } +
                            bookmarks.map { it.toUnified() }
                    state.copy(
                        quranBookmarks = bookmarks,
                        allBookmarks = unified,
                        filteredBookmarks = applyFilters(unified, state.selectedFilter, state.searchQuery, state.sortOrder),
                        isLoading = false
                    )
                }
                updateStats()
            }
        }
    }

    private fun loadHadithBookmarks() {
        viewModelScope.launch {
            hadithRepository.getAllBookmarks().collect { bookmarks ->
                _bookmarksState.update { state ->
                    val unified = state.allBookmarks.filter { it.type != BookmarkType.HADITH } +
                            bookmarks.map { it.toUnified() }
                    state.copy(
                        hadithBookmarks = bookmarks,
                        allBookmarks = unified,
                        filteredBookmarks = applyFilters(unified, state.selectedFilter, state.searchQuery, state.sortOrder),
                        isLoading = false
                    )
                }
                updateStats()
            }
        }
    }

    private fun loadDuaBookmarks() {
        viewModelScope.launch {
            duaRepository.getAllBookmarks().collect { bookmarks ->
                _bookmarksState.update { state ->
                    val unified = state.allBookmarks.filter { it.type != BookmarkType.DUA } +
                            bookmarks.map { it.toUnified() }
                    state.copy(
                        duaBookmarks = bookmarks,
                        allBookmarks = unified,
                        filteredBookmarks = applyFilters(unified, state.selectedFilter, state.searchQuery, state.sortOrder),
                        isLoading = false
                    )
                }
                updateStats()
            }
        }
    }

    private fun setFilter(type: BookmarkType?) {
        _bookmarksState.update { state ->
            state.copy(
                selectedFilter = type,
                filteredBookmarks = applyFilters(state.allBookmarks, type, state.searchQuery, state.sortOrder)
            )
        }
    }

    private fun setSortOrder(order: BookmarkSortOrder) {
        _bookmarksState.update { state ->
            state.copy(
                sortOrder = order,
                filteredBookmarks = applyFilters(state.allBookmarks, state.selectedFilter, state.searchQuery, order)
            )
        }
    }

    private fun search(query: String) {
        _bookmarksState.update { state ->
            state.copy(
                searchQuery = query,
                filteredBookmarks = applyFilters(state.allBookmarks, state.selectedFilter, query, state.sortOrder)
            )
        }
    }

    private fun clearSearch() {
        _bookmarksState.update { state ->
            state.copy(
                searchQuery = "",
                filteredBookmarks = applyFilters(state.allBookmarks, state.selectedFilter, "", state.sortOrder)
            )
        }
    }

    private fun applyFilters(
        bookmarks: List<UnifiedBookmark>,
        filter: BookmarkType?,
        searchQuery: String,
        sortOrder: BookmarkSortOrder
    ): List<UnifiedBookmark> {
        var result = bookmarks

        // Apply type filter
        if (filter != null) {
            result = result.filter { it.type == filter }
        }

        // Apply search
        if (searchQuery.isNotBlank()) {
            result = result.filter { bookmark ->
                bookmark.title.contains(searchQuery, ignoreCase = true) ||
                bookmark.subtitle.contains(searchQuery, ignoreCase = true) ||
                bookmark.arabicText?.contains(searchQuery) == true ||
                bookmark.note?.contains(searchQuery, ignoreCase = true) == true
            }
        }

        // Apply sort
        result = when (sortOrder) {
            BookmarkSortOrder.DATE_NEWEST -> result.sortedByDescending { it.createdAt }
            BookmarkSortOrder.DATE_OLDEST -> result.sortedBy { it.createdAt }
            BookmarkSortOrder.TYPE -> result.sortedBy { it.type.ordinal }
            BookmarkSortOrder.ALPHABETICAL -> result.sortedBy { it.title.lowercase() }
        }

        return result
    }

    private fun deleteQuranBookmark(ayahId: Int) {
        viewModelScope.launch {
            quranRepository.deleteBookmark(ayahId)
        }
    }

    private fun deleteHadithBookmark(hadithId: String) {
        viewModelScope.launch {
            hadithRepository.deleteBookmark(hadithId)
        }
    }

    private fun deleteDuaBookmark(duaId: String) {
        viewModelScope.launch {
            duaRepository.deleteBookmark(duaId)
        }
    }

    private fun updateQuranBookmarkNote(bookmark: QuranBookmark) {
        viewModelScope.launch {
            quranRepository.updateBookmark(bookmark)
        }
    }

    private fun updateHadithBookmarkNote(bookmark: HadithBookmark) {
        viewModelScope.launch {
            hadithRepository.updateBookmark(bookmark)
        }
    }

    private fun clearAllBookmarks() {
        viewModelScope.launch {
            _bookmarksState.value.quranBookmarks.forEach {
                quranRepository.deleteBookmark(it.ayahId)
            }
            _bookmarksState.value.hadithBookmarks.forEach {
                hadithRepository.deleteBookmark(it.hadithId)
            }
            _bookmarksState.value.duaBookmarks.forEach {
                duaRepository.deleteBookmark(it.duaId)
            }
        }
    }

    private fun updateStats() {
        val state = _bookmarksState.value
        _statsState.update {
            BookmarkStatsUiState(
                totalBookmarks = state.allBookmarks.size,
                quranCount = state.quranBookmarks.size,
                hadithCount = state.hadithBookmarks.size,
                duaCount = state.duaBookmarks.size
            )
        }
    }

    // Extension functions to convert to unified format
    private fun QuranBookmark.toUnified() = UnifiedBookmark(
        id = "quran_$ayahId",
        type = BookmarkType.QURAN,
        title = "Surah $surahNumber, Ayah $ayahNumber",
        subtitle = "Quran",
        arabicText = null, // Would be populated from ayah data
        createdAt = createdAt,
        note = note,
        color = color,
        surahNumber = surahNumber,
        ayahNumber = ayahNumber
    )

    private fun HadithBookmark.toUnified() = UnifiedBookmark(
        id = "hadith_$hadithId",
        type = BookmarkType.HADITH,
        title = "Hadith #$hadithNumber",
        subtitle = bookId,
        arabicText = null, // Would be populated from hadith data
        createdAt = createdAt,
        note = note,
        color = null,
        hadithBookId = bookId,
        hadithNumber = hadithNumber
    )

    private fun DuaBookmark.toUnified() = UnifiedBookmark(
        id = "dua_$duaId",
        type = BookmarkType.DUA,
        title = "Dua",
        subtitle = categoryId,
        arabicText = null, // Would be populated from dua data
        createdAt = createdAt,
        note = null,
        color = null,
        duaId = duaId,
        categoryId = categoryId
    )
}
