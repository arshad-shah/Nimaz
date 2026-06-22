package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.HadithBookmark
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.usecase.DuaUseCases
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import android.content.Context
import com.arshadshah.nimaz.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val error: String? = null,
    // Holds the most recently deleted bookmark so the UI can offer an Undo
    // snackbar; cleared on undo, dismiss, or a subsequent delete.
    val recentlyDeleted: UnifiedBookmark? = null
)

data class BookmarkStatsUiState(
    val totalBookmarks: Int = 0,
    val quranCount: Int = 0,
    val hadithCount: Int = 0,
    val duaCount: Int = 0
)

sealed interface BookmarksEvent {
    data class SetFilter(val type: BookmarkType?) : BookmarksEvent
    /** Delete any bookmark by its unified id (e.g. "quran_12"). Captures it for Undo. */
    data class DeleteBookmark(val id: String) : BookmarksEvent
    data object UndoDelete : BookmarksEvent
    data object DismissUndo : BookmarksEvent
    /** Set (or clear, with null) the note on any bookmark by its unified id. */
    data class EditNote(val id: String, val note: String?) : BookmarksEvent
    data object RefreshAll : BookmarksEvent
    data object ClearAllBookmarks : BookmarksEvent
}

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val quranUseCases: QuranUseCases,
    private val hadithUseCases: HadithUseCases,
    private val duaUseCases: DuaUseCases,
    @ApplicationContext private val context: Context
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
            is BookmarksEvent.SetFilter -> AppAnalytics.logFeatureUsed("bookmarks", "set_filter")
            is BookmarksEvent.DeleteBookmark -> AppAnalytics.logFeatureUsed("bookmarks", "delete")
            BookmarksEvent.UndoDelete -> AppAnalytics.logFeatureUsed("bookmarks", "undo_delete")
            is BookmarksEvent.EditNote -> AppAnalytics.logFeatureUsed("bookmarks", "update_note")
            BookmarksEvent.ClearAllBookmarks -> AppAnalytics.logFeatureUsed("bookmarks", "clear_all")
            else -> {}
        }
        when (event) {
            is BookmarksEvent.SetFilter -> setFilter(event.type)
            is BookmarksEvent.DeleteBookmark -> deleteBookmark(event.id)
            BookmarksEvent.UndoDelete -> undoDelete()
            BookmarksEvent.DismissUndo -> dismissUndo()
            is BookmarksEvent.EditNote -> editNote(event.id, event.note)
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
            quranUseCases.getBookmarks().collect { bookmarks ->
                val mapped = bookmarks.map { bm ->
                    bm.toUnified().copy(arabicText = quranUseCases.getAyahById(bm.ayahId)?.textArabic)
                }
                _bookmarksState.update { state ->
                    val unified = state.allBookmarks.filter { it.type != BookmarkType.QURAN } +
                            mapped
                    state.copy(
                        quranBookmarks = bookmarks,
                        allBookmarks = unified,
                        filteredBookmarks = applyFilters(
                            unified,
                            state.selectedFilter,
                            state.searchQuery,
                            state.sortOrder
                        ),
                        isLoading = false
                    )
                }
                updateStats()
            }
        }
    }

    private fun loadHadithBookmarks() {
        viewModelScope.launch {
            hadithUseCases.getAllBookmarks().collect { bookmarks ->
                val mapped = bookmarks.map { bm ->
                    bm.toUnified().copy(arabicText = hadithUseCases.getHadithById(bm.hadithId)?.textArabic)
                }
                _bookmarksState.update { state ->
                    val unified = state.allBookmarks.filter { it.type != BookmarkType.HADITH } +
                            mapped
                    state.copy(
                        hadithBookmarks = bookmarks,
                        allBookmarks = unified,
                        filteredBookmarks = applyFilters(
                            unified,
                            state.selectedFilter,
                            state.searchQuery,
                            state.sortOrder
                        ),
                        isLoading = false
                    )
                }
                updateStats()
            }
        }
    }

    private fun loadDuaBookmarks() {
        viewModelScope.launch {
            duaUseCases.getAllBookmarks().collect { bookmarks ->
                val mapped = bookmarks.map { bm ->
                    bm.toUnified().copy(arabicText = duaUseCases.getDuaById(bm.duaId)?.textArabic)
                }
                _bookmarksState.update { state ->
                    val unified = state.allBookmarks.filter { it.type != BookmarkType.DUA } +
                            mapped
                    state.copy(
                        duaBookmarks = bookmarks,
                        allBookmarks = unified,
                        filteredBookmarks = applyFilters(
                            unified,
                            state.selectedFilter,
                            state.searchQuery,
                            state.sortOrder
                        ),
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
                filteredBookmarks = applyFilters(
                    state.allBookmarks,
                    type,
                    state.searchQuery,
                    state.sortOrder
                )
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

    // The re-insert action captured when a bookmark is deleted, so Undo can
    // losslessly restore it (note/colour/favourite preserved).
    private var pendingRestore: (suspend () -> Unit)? = null

    private fun deleteBookmark(id: String) {
        val state = _bookmarksState.value
        val unified = state.allBookmarks.find { it.id == id } ?: return
        when (unified.type) {
            BookmarkType.QURAN -> {
                val original = state.quranBookmarks.find { "quran_${it.ayahId}" == id } ?: return
                pendingRestore = { quranUseCases.insertBookmark(original) }
                viewModelScope.launch { quranUseCases.deleteBookmark(original.ayahId) }
            }

            BookmarkType.HADITH -> {
                val original = state.hadithBookmarks.find { "hadith_${it.hadithId}" == id } ?: return
                pendingRestore = { hadithUseCases.insertBookmark(original) }
                viewModelScope.launch { hadithUseCases.deleteBookmark(original.hadithId) }
            }

            BookmarkType.DUA -> {
                val original = state.duaBookmarks.find { "dua_${it.duaId}" == id } ?: return
                pendingRestore = { duaUseCases.insertBookmark(original) }
                viewModelScope.launch { duaUseCases.deleteBookmark(original.duaId) }
            }
        }
        _bookmarksState.update { it.copy(recentlyDeleted = unified) }
    }

    private fun undoDelete() {
        val restore = pendingRestore ?: return
        pendingRestore = null
        viewModelScope.launch { restore() }
        _bookmarksState.update { it.copy(recentlyDeleted = null) }
    }

    private fun dismissUndo() {
        pendingRestore = null
        _bookmarksState.update { it.copy(recentlyDeleted = null) }
    }

    private fun editNote(id: String, note: String?) {
        val state = _bookmarksState.value
        val trimmed = note?.trim()?.ifBlank { null }
        viewModelScope.launch {
            when {
                id.startsWith("quran_") -> state.quranBookmarks.find { "quran_${it.ayahId}" == id }
                    ?.let { quranUseCases.updateBookmark(it.copy(note = trimmed)) }

                id.startsWith("hadith_") -> state.hadithBookmarks.find { "hadith_${it.hadithId}" == id }
                    ?.let { hadithUseCases.updateBookmark(it.copy(note = trimmed)) }

                id.startsWith("dua_") -> state.duaBookmarks.find { "dua_${it.duaId}" == id }
                    ?.let { duaUseCases.updateBookmark(it.copy(note = trimmed)) }
            }
        }
    }

    private fun clearAllBookmarks() {
        viewModelScope.launch {
            _bookmarksState.value.quranBookmarks.forEach {
                quranUseCases.deleteBookmark(it.ayahId)
            }
            _bookmarksState.value.hadithBookmarks.forEach {
                hadithUseCases.deleteBookmark(it.hadithId)
            }
            _bookmarksState.value.duaBookmarks.forEach {
                duaUseCases.deleteBookmark(it.duaId)
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
        title = context.getString(R.string.bookmark_surah_ayah_format, surahNumber, ayahNumber),
        subtitle = context.getString(R.string.quran),
        arabicText = null, // Enriched in loadQuranBookmarks via getAyahById
        createdAt = createdAt,
        note = note,
        color = color,
        surahNumber = surahNumber,
        ayahNumber = ayahNumber
    )

    private fun HadithBookmark.toUnified() = UnifiedBookmark(
        id = "hadith_$hadithId",
        type = BookmarkType.HADITH,
        title = context.getString(R.string.bookmark_hadith_format, hadithNumber),
        subtitle = bookId,
        arabicText = null, // Enriched in loadHadithBookmarks via getHadithById
        createdAt = createdAt,
        note = note,
        color = null,
        hadithBookId = bookId,
        hadithNumber = hadithNumber
    )

    private fun DuaBookmark.toUnified() = UnifiedBookmark(
        id = "dua_$duaId",
        type = BookmarkType.DUA,
        title = context.getString(R.string.dua_label),
        subtitle = categoryId,
        arabicText = null, // Enriched in loadDuaBookmarks via getDuaById
        createdAt = createdAt,
        note = note,
        color = null,
        duaId = duaId,
        categoryId = categoryId
    )
}
