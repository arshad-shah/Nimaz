package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.HadithBookmark
import com.arshadshah.nimaz.domain.model.QuranBookmark

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
