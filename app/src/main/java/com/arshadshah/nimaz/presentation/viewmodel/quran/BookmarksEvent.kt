package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.domain.model.BookmarkType
import com.arshadshah.nimaz.domain.model.SavedKind

sealed interface BookmarksEvent {
    data class SetFilter(val type: BookmarkType?) : BookmarksEvent

    /** Narrow to one way of having marked something, or null for all three. */
    data class SetKind(val kind: SavedKind?) : BookmarksEvent

    /** Delete any bookmark by its unified id (e.g. "quran_12"). Captures it for Undo. */
    data class DeleteBookmark(val id: String) : BookmarksEvent
    data object UndoDelete : BookmarksEvent
    data object DismissUndo : BookmarksEvent

    /** Set (or clear, with null) the note on any bookmark by its unified id. */
    data class EditNote(val id: String, val note: String?) : BookmarksEvent

    data class SetSearchQuery(val query: String) : BookmarksEvent
    data class SetSortOrder(val order: BookmarkSortOrder) : BookmarksEvent
    data object ClearAllBookmarks : BookmarksEvent

    /** Re-runs the three bookmark loads after a failed read. */
    data object Retry : BookmarksEvent

    /** Clears a failed-write message once the snackbar carrying it has been shown. */
    data object DismissWriteError : BookmarksEvent
}
