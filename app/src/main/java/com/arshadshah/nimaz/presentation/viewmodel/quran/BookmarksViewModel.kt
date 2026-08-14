package com.arshadshah.nimaz.presentation.viewmodel.quran

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.text.StringProvider
import com.arshadshah.nimaz.core.monitoring.catchAndReport
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.HadithBookmark
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.QuranFavorite
import com.arshadshah.nimaz.domain.model.SavedKind
import com.arshadshah.nimaz.domain.usecase.DuaUseCases
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.arshadshah.nimaz.domain.model.BookmarkType
import com.arshadshah.nimaz.domain.model.UnifiedBookmark

enum class BookmarkSortOrder {
    DATE_NEWEST, DATE_OLDEST, TYPE, ALPHABETICAL
}

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val quranUseCases: QuranUseCases,
    private val hadithUseCases: HadithUseCases,
    private val duaUseCases: DuaUseCases,
    private val strings: StringProvider,
    private val telemetry: Telemetry,
) : ViewModel() {

    private companion object {
        const val DOMAIN = "bookmarks"
    }

    private val _bookmarksState = MutableStateFlow(BookmarksUiState())
    val bookmarksState: StateFlow<BookmarksUiState> = _bookmarksState.asStateFlow()

    private val _statsState = MutableStateFlow(BookmarkStatsUiState())
    val statsState: StateFlow<BookmarkStatsUiState> = _statsState.asStateFlow()

    init {
        loadAllBookmarks()
    }

    fun onEvent(event: BookmarksEvent) {
        // One exhaustive table: an analytics `when` with an `else -> {}` alongside the
        // dispatch meant a new event shipped with no telemetry and no compiler warning.
        when (event) {
            is BookmarksEvent.SetKind -> {
                telemetry.featureUsed(DOMAIN, "set_kind")
                setKind(event.kind)
            }

            is BookmarksEvent.SetFilter -> {
                telemetry.featureUsed(DOMAIN, "set_filter")
                setFilter(event.type)
            }

            is BookmarksEvent.DeleteBookmark -> {
                telemetry.featureUsed(DOMAIN, "delete")
                deleteBookmark(event.id)
            }

            BookmarksEvent.UndoDelete -> {
                telemetry.featureUsed(DOMAIN, "undo_delete")
                undoDelete()
            }

            // The undo banner expiring unused is the signal that a delete was meant, which
            // is only legible next to `undo_delete`.
            BookmarksEvent.DismissUndo -> {
                telemetry.featureUsed(DOMAIN, "dismiss_undo")
                dismissUndo()
            }

            is BookmarksEvent.EditNote -> {
                telemetry.featureUsed(DOMAIN, "update_note")
                editNote(event.id, event.note)
            }

            is BookmarksEvent.SetSearchQuery -> setSearchQuery(event.query)

            is BookmarksEvent.SetSortOrder -> {
                telemetry.settingChanged("bookmark_sort", event.order.name)
                setSortOrder(event.order)
            }

            BookmarksEvent.ClearAllBookmarks -> {
                telemetry.featureUsed(DOMAIN, "clear_all")
                clearAllBookmarks()
            }

            BookmarksEvent.Retry -> {
                _bookmarksState.update { it.copy(isLoading = true, error = null) }
                loadQuranBookmarks()
                loadHadithBookmarks()
                loadDuaBookmarks()
            }

            BookmarksEvent.DismissWriteError ->
                _bookmarksState.update { it.copy(writeError = null) }
        }
    }

    private fun loadAllBookmarks() {
        loadQuranBookmarks()
        loadHadithBookmarks()
        loadDuaBookmarks()
    }

    /**
     * The Qur'an half of Saved: bookmarks **and** favourites, merged.
     *
     * They are one row in the store — `bookmarks` carries `bookmarked` and `favourite` as two
     * flags — but two queries, because everything else in the app asks for one or the other.
     * Merging here by ayah id is what stops a verse that is both from appearing twice, and is
     * why the kind axis can be a set rather than a guess.
     */
    private fun loadQuranBookmarks() {
        launchSafely(telemetry, DOMAIN, "load_quran_bookmarks") {
            combine(
                quranUseCases.getBookmarks(),
                quranUseCases.getFavorites(),
            ) { bookmarks, favourites -> bookmarks to favourites }
                .catchAndReport(telemetry, DOMAIN, "load_quran") { throwable ->
                    // Enrichment runs a suspend query per row inside this collector, so a
                    // content row missing after a database replacement used to kill the
                    // stream and pin isLoading on for ever, silently.
                    _bookmarksState.update {
                        it.copy(
                            isLoading = false,
                            error = UiError(
                                message = R.string.bookmarks_load_failed,
                                details = throwable.message,
                            ),
                        )
                    }
                }
                .collect { (bookmarks, favourites) ->
                    // One query for the whole list. This used to be a suspend call per row
                    // inside the collector, so N bookmarks meant N round-trips on every
                    // re-emission — and clearing them all re-emitted once per delete, making
                    // the wipe O(N^2).
                    val favouriteIds = favourites.map { it.ayahId }.toSet()
                    val bookmarkedIds = bookmarks.map { it.ayahId }.toSet()
                    val onlyFavourites = favourites.filterNot { it.ayahId in bookmarkedIds }
                    val ids = bookmarks.map { it.ayahId } + onlyFavourites.map { it.ayahId }
                    val texts = quranUseCases.getAyahById.forIds(ids)
                    val mapped = bookmarks.map { bm ->
                        bm.toUnified(isFavourite = bm.ayahId in favouriteIds)
                            .copy(arabicText = texts[bm.ayahId]?.textArabic)
                    } + onlyFavourites.map { fav ->
                        fav.toUnified().copy(arabicText = texts[fav.ayahId]?.textArabic)
                    }
                    _bookmarksState.update { state ->
                        val unified = state.allBookmarks.filter { it.type != BookmarkType.QURAN } +
                                mapped
                        state.copy(
                            quranBookmarks = bookmarks,
                            quranFavourites = favourites,
                            allBookmarks = unified,
                            filteredBookmarks = applyFilters(
                                unified,
                                state.selectedFilter,
                                state.selectedKind,
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
        launchSafely(telemetry, DOMAIN, "load_hadith_bookmarks") {
            hadithUseCases.getAllBookmarks()
                .catchAndReport(telemetry, DOMAIN, "load_hadith") { throwable ->
                    // Enrichment runs a suspend query per row inside this collector, so a
                    // content row missing after a database replacement used to kill the
                    // stream and pin isLoading on for ever, silently.
                    _bookmarksState.update {
                        it.copy(
                            isLoading = false,
                            error = UiError(
                                message = R.string.bookmarks_load_failed,
                                details = throwable.message,
                            ),
                        )
                    }
                }
                .collect { bookmarks ->
                val texts = hadithUseCases.getHadithById.forIds(bookmarks.map { it.hadithId })
                val mapped = bookmarks.map { bm ->
                    bm.toUnified().copy(arabicText = texts[bm.hadithId]?.textArabic)
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
                            state.selectedKind,
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
        launchSafely(telemetry, DOMAIN, "load_dua_bookmarks") {
            duaUseCases.getAllBookmarks()
                .catchAndReport(telemetry, DOMAIN, "load_dua") { throwable ->
                    // Enrichment runs a suspend query per row inside this collector, so a
                    // content row missing after a database replacement used to kill the
                    // stream and pin isLoading on for ever, silently.
                    _bookmarksState.update {
                        it.copy(
                            isLoading = false,
                            error = UiError(
                                message = R.string.bookmarks_load_failed,
                                details = throwable.message,
                            ),
                        )
                    }
                }
                .collect { bookmarks ->
                val texts = duaUseCases.getDuaById.forIds(bookmarks.map { it.duaId })
                val mapped = bookmarks.map { bm ->
                    bm.toUnified().copy(arabicText = texts[bm.duaId]?.textArabic)
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
                            state.selectedKind,
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
                    state.selectedKind,
                    state.searchQuery,
                    state.sortOrder
                )
            )
        }
    }

    private fun setKind(kind: SavedKind?) {
        _bookmarksState.update { state ->
            state.copy(
                selectedKind = kind,
                filteredBookmarks = applyFilters(
                    state.allBookmarks,
                    state.selectedFilter,
                    kind,
                    state.searchQuery,
                    state.sortOrder
                )
            )
        }
    }

    private fun setSearchQuery(query: String) {
        _bookmarksState.update { state ->
            state.copy(
                searchQuery = query,
                filteredBookmarks = applyFilters(
                    state.allBookmarks,
                    state.selectedFilter,
                    state.selectedKind,
                    query,
                    state.sortOrder
                )
            )
        }
        // Post-filter, length only — the query text never reaches analytics.
        if (query.isNotBlank()) telemetry.search(DOMAIN, query.trim().length)
    }

    private fun setSortOrder(order: BookmarkSortOrder) {
        _bookmarksState.update { state ->
            state.copy(
                sortOrder = order,
                filteredBookmarks = applyFilters(
                    state.allBookmarks,
                    state.selectedFilter,
                    state.selectedKind,
                    state.searchQuery,
                    order
                )
            )
        }
    }

    private fun applyFilters(
        bookmarks: List<UnifiedBookmark>,
        filter: BookmarkType?,
        kind: SavedKind?,
        searchQuery: String,
        sortOrder: BookmarkSortOrder
    ): List<UnifiedBookmark> {
        var result = bookmarks

        // Apply type filter
        if (filter != null) {
            result = result.filter { it.type == filter }
        }

        // Apply kind filter. Membership, not equality: a verse can be bookmarked and
        // annotated, and it belongs under both.
        if (kind != null) {
            result = result.filter { kind in it.kinds }
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

    /**
     * Re-insert actions captured when bookmarks are deleted, newest last, so Undo can
     * losslessly restore them (note/colour preserved).
     *
     * This was a single `var`: deleting a second bookmark before the snackbar timed
     * out overwrote the first, making it unrecoverable — while the UI still showed an
     * ordinary single-item undo. It is a stack now, so every delete stays undoable
     * until the user dismisses.
     */
    private val pendingRestores = ArrayDeque<Pair<UnifiedBookmark, suspend () -> Unit>>()

    /**
     * Records a failed **write** without disturbing the list.
     *
     * Never touches `isLoading` or `error`: the bookmarks on screen are still correct, and
     * replacing them with a full-screen failure because a delete did not go through would
     * destroy good content to report a bad button press.
     */
    private fun writeFailed(@StringRes message: Int, throwable: Throwable) {
        _bookmarksState.update {
            it.copy(writeError = UiError(message = message, details = throwable.message))
        }
    }

    /**
     * Deletes a bookmark, and only then offers the undo.
     *
     * The undo snackbar and the pending-restore entry used to be written the moment the
     * event arrived, outside the coroutine that did the deleting. A delete that threw
     * therefore left the row on screen *and* an "Undo" for it — the UI asserting
     * something untrue, and an undo that would have re-inserted a bookmark which was
     * never removed. Both now happen inside the block, after the delete has returned, so
     * a failure leaves the screen exactly as it was and says so.
     */
    private fun deleteBookmark(id: String) {
        val state = _bookmarksState.value
        val unified = state.allBookmarks.find { it.id == id } ?: return
        val operation: Pair<suspend () -> Unit, suspend () -> Unit> = when (unified.type) {
            // Unsave the verse, not just half of it. A verse that is bookmarked *and*
            // favourited is one card, and deleting only the bookmark left the card on screen —
            // it came straight back through the favourites half of the merge, with the counts
            // moving and nothing else. A favourite-only card could not be deleted at all: the
            // lookup into `quranBookmarks` found nothing and the whole delete returned early.
            BookmarkType.QURAN -> {
                val bookmark = state.quranBookmarks
                    .find { BookmarkType.QURAN.idFor(it.ayahId) == id }
                val favourite = state.quranFavourites
                    .find { BookmarkType.QURAN.idFor(it.ayahId) == id }
                if (bookmark == null && favourite == null) return
                Pair<suspend () -> Unit, suspend () -> Unit>(
                    {
                        bookmark?.let { quranUseCases.deleteBookmark(it.ayahId) }
                        // A toggle is safe in both directions here because the current state is
                        // known: it is favourited now, so this clears it; after the delete it is
                        // not, so the same call in `restore` puts it back.
                        favourite?.let {
                            quranUseCases.toggleFavorite(it.ayahId, it.surahNumber, it.ayahNumber)
                        }
                    },
                    {
                        bookmark?.let { quranUseCases.insertBookmark(it) }
                        favourite?.let {
                            quranUseCases.toggleFavorite(it.ayahId, it.surahNumber, it.ayahNumber)
                        }
                    },
                )
            }

            BookmarkType.HADITH -> {
                val original = state.hadithBookmarks
                    .find { BookmarkType.HADITH.idFor(it.hadithId) == id } ?: return
                Pair<suspend () -> Unit, suspend () -> Unit>(
                    { hadithUseCases.deleteBookmark(original.hadithId) },
                    { hadithUseCases.insertBookmark(original) },
                )
            }

            BookmarkType.DUA -> {
                val original = state.duaBookmarks
                    .find { BookmarkType.DUA.idFor(it.duaId) == id } ?: return
                Pair<suspend () -> Unit, suspend () -> Unit>(
                    { duaUseCases.deleteBookmark(original.duaId) },
                    { duaUseCases.insertBookmark(original) },
                )
            }
        }
        val (delete, restore) = operation
        launchSafely(
            telemetry, DOMAIN, "delete",
            onFailure = { writeFailed(R.string.bookmarks_delete_failed, it) },
        ) {
            delete()
            pendingRestores.addLast(unified to restore)
            _bookmarksState.update { it.copy(recentlyDeleted = unified) }
        }
    }

    private fun undoDelete() {
        val (_, restore) = pendingRestores.removeLastOrNull() ?: return
        launchSafely(
            telemetry, DOMAIN, "undo_delete",
            onFailure = { writeFailed(R.string.bookmarks_restore_failed, it) },
        ) { restore() }
        // Surface the next pending restore so a run of deletes can be undone one by one.
        _bookmarksState.update { it.copy(recentlyDeleted = pendingRestores.lastOrNull()?.first) }
    }

    private fun dismissUndo() {
        pendingRestores.clear()
        _bookmarksState.update { it.copy(recentlyDeleted = null) }
    }

    private fun editNote(id: String, note: String?) {
        val state = _bookmarksState.value
        val trimmed = note?.trim()?.ifBlank { null }
        launchSafely(
            telemetry, DOMAIN, "update_note",
            onFailure = { writeFailed(R.string.bookmarks_note_failed, it) },
        ) {
            when (BookmarkType.ofId(id)) {
                BookmarkType.QURAN -> state.quranBookmarks
                    .find { BookmarkType.QURAN.idFor(it.ayahId) == id }
                    ?.let { quranUseCases.updateBookmark(it.copy(note = trimmed)) }

                BookmarkType.HADITH -> state.hadithBookmarks
                    .find { BookmarkType.HADITH.idFor(it.hadithId) == id }
                    ?.let { hadithUseCases.updateBookmark(it.copy(note = trimmed)) }

                BookmarkType.DUA -> state.duaBookmarks
                    .find { BookmarkType.DUA.idFor(it.duaId) == id }
                    ?.let { duaUseCases.updateBookmark(it.copy(note = trimmed)) }

                null -> Unit
            }
        }
    }

    private fun clearAllBookmarks() {
        // Destructive and, by decision, not undoable — the confirmation dialog is the
        // safety net. Pending restores are dropped so a stale snackbar cannot
        // resurrect one bookmark out of a wipe the user confirmed.
        pendingRestores.clear()
        _bookmarksState.update { it.copy(recentlyDeleted = null) }
        launchSafely(
            telemetry, DOMAIN, "clear_all",
            onFailure = { writeFailed(R.string.bookmarks_clear_failed, it) },
        ) {
            // Favourites too, not only the bookmark rows: a wipe the user confirmed on a
            // screen called Saved that left every favourited verse behind is the same
            // half-delete the per-row action had.
            val before = _bookmarksState.value
            before.quranBookmarks.forEach {
                quranUseCases.deleteBookmark(it.ayahId)
            }
            before.quranFavourites.forEach {
                quranUseCases.toggleFavorite(it.ayahId, it.surahNumber, it.ayahNumber)
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
                // Counted off the unified list, not off `quranBookmarks`: a verse favourited
                // but not bookmarked is in Saved and is not in that list, so counting it there
                // would print a Qur'an chip that disagrees with the rows beneath it.
                quranCount = state.allBookmarks.count { it.type == BookmarkType.QURAN },
                hadithCount = state.hadithBookmarks.size,
                duaCount = state.duaBookmarks.size,
                bookmarkCount = state.allBookmarks.count { SavedKind.BOOKMARK in it.kinds },
                favouriteCount = state.allBookmarks.count { SavedKind.FAVOURITE in it.kinds },
                noteCount = state.allBookmarks.count { SavedKind.NOTE in it.kinds },
            )
        }
    }

    // Extension functions to convert to unified format
    private fun QuranBookmark.toUnified(isFavourite: Boolean = false) = UnifiedBookmark(
        id = BookmarkType.QURAN.idFor(ayahId),
        type = BookmarkType.QURAN,
        kinds = buildSet {
            add(SavedKind.BOOKMARK)
            if (isFavourite) add(SavedKind.FAVOURITE)
            if (!note.isNullOrBlank()) add(SavedKind.NOTE)
        },
        title = strings.get(R.string.bookmark_surah_ayah_format, surahNumber, ayahNumber),
        subtitle = strings.get(R.string.quran),
        arabicText = null, // Enriched in loadQuranBookmarks via getAyahById
        createdAt = createdAt,
        note = note,
        color = color,
        surahNumber = surahNumber,
        ayahNumber = ayahNumber
    )

    private fun HadithBookmark.toUnified() = UnifiedBookmark(
        id = BookmarkType.HADITH.idFor(hadithId),
        type = BookmarkType.HADITH,
        kinds = kindsOf(note),
        title = strings.get(R.string.bookmark_hadith_format, hadithNumber),
        subtitle = bookId,
        arabicText = null, // Enriched in loadHadithBookmarks via getHadithById
        createdAt = createdAt,
        note = note,
        color = null,
        hadithBookId = bookId,
        hadithNumber = hadithNumber
    )

    private fun DuaBookmark.toUnified() = UnifiedBookmark(
        id = BookmarkType.DUA.idFor(duaId),
        type = BookmarkType.DUA,
        kinds = kindsOf(note),
        title = strings.get(R.string.dua_label),
        subtitle = categoryId,
        arabicText = null, // Enriched in loadDuaBookmarks via getDuaById
        createdAt = createdAt,
        note = note,
        color = null,
        duaId = duaId,
        categoryId = categoryId
    )

    /**
     * A verse favourited but not bookmarked — a real state, since the two are separate flags on
     * one row, and one the Saved screen has to show or the Favourites filter would be empty for
     * anyone who only ever tapped the heart.
     */
    private fun QuranFavorite.toUnified() = UnifiedBookmark(
        id = BookmarkType.QURAN.idFor(ayahId),
        type = BookmarkType.QURAN,
        kinds = setOf(SavedKind.FAVOURITE),
        title = strings.get(R.string.bookmark_surah_ayah_format, surahNumber, ayahNumber),
        subtitle = strings.get(R.string.quran),
        arabicText = null,
        createdAt = createdAt,
        note = null,
        color = null,
        surahNumber = surahNumber,
        ayahNumber = ayahNumber
    )

    /** Bookmarked, plus annotated when there is a note. The two corpora without favourites. */
    private fun kindsOf(note: String?): Set<SavedKind> = buildSet {
        add(SavedKind.BOOKMARK)
        if (!note.isNullOrBlank()) add(SavedKind.NOTE)
    }
}
