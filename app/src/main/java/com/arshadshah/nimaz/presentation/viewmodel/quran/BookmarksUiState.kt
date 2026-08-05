package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.HadithBookmark
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.BookmarkType
import com.arshadshah.nimaz.domain.model.UnifiedBookmark
import com.arshadshah.nimaz.presentation.viewmodel.UiError

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

    /**
     * A failed **read** — the list could not be built, so there is nothing valid to show
     * and the screen renders an error in place of it.
     */
    val error: UiError? = null,

    /**
     * A failed **write** — a delete, an undo, a note edit, a wipe.
     *
     * Deliberately a separate field: the bookmarks on screen are still correct and still
     * useful, so replacing them with a full-screen error because a delete failed would
     * destroy good content to report a bad button press. This one surfaces on the
     * snackbar the screen already has, and never touches [isLoading].
     */
    val writeError: UiError? = null,
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
