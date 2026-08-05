package com.arshadshah.nimaz.presentation.viewmodel.content

import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchBestEffort
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithBook
import com.arshadshah.nimaz.domain.model.HadithBookmark
import com.arshadshah.nimaz.domain.model.HadithChapter
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.domain.model.HadithSearchResult
import com.arshadshah.nimaz.domain.repository.settings.HadithDisplaySettings
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorKind
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.arshadshah.nimaz.presentation.theme.QuranArabicFont
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HadithViewModel @Inject constructor(
    private val hadithUseCases: HadithUseCases,
    private val hadithSettings: HadithDisplaySettings,
    private val telemetry: Telemetry
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
    // `launchSafely(telemetry, AppAnalytics.Feature.HADITH, "launch") { flow.collect { … } }` with no handle leaks a collector per
    // invocation, all of them writing the same state for the lifetime of the ViewModel.
    // Search re-runs per keystroke (so an earlier, slower query could land last and win),
    // and the chapter/reader loaders re-run per navigation. One handle per *identity of the
    // request*: the reader shows one chapter at a time, so one handle each is correct here.
    // (AP-7.1b in docs/CLEAN_ARCHITECTURE_CHECKLIST.md — same defect as the Quran reader's.)
    private var searchJob: Job? = null
    private var chaptersJob: Job? = null
    private var readerJob: Job? = null

    /**
     * The hadith the reader is anchored to, by id.
     *
     * `getHadithsByChapter` is a Room Flow: any write touching the hadiths table re-emits, and
     * so does a content-database swap by `ContentArtifactInstaller`. The loader used to set
     * `currentHadithIndex = 0` inside that collector — on *every* emission, not just the first
     * — so a background content refresh scrolled a reader at hadith 50 back to hadith 1.
     *
     * Held by id rather than by index so the anchor survives a refresh that inserts or reorders
     * rows, which an index would not.
     */
    private var anchorHadithId: String? = null

    /** What a [HadithEvent.Retry] should re-issue, per surface. */
    private var lastBookId: String? = null
    private var lastReaderLoad: (() -> Unit)? = null

    init {
        loadAllBooks()
        loadBookmarks()
        loadHadithOfTheDay()
        observeHadithSettings()
    }

    fun onEvent(event: HadithEvent) {
        when (event) {
            is HadithEvent.LoadBook -> {
                telemetry.featureUsed(AppAnalytics.Feature.HADITH, "open_book")
                loadBook(event.bookId)
            }
            is HadithEvent.LoadChapter -> {
                telemetry.featureUsed(AppAnalytics.Feature.HADITH, "open_reader")
                loadChapter(event.chapterId)
            }
            is HadithEvent.LoadHadithById -> {
                telemetry.featureUsed(AppAnalytics.Feature.HADITH, "open_hadith")
                loadHadithById(event.hadithId)
            }
            is HadithEvent.LoadHadithByNumber -> {
                telemetry.featureUsed(
                    AppAnalytics.Feature.HADITH,
                    "open_hadith"
                )
                loadHadithByNumber(
                    event.bookId,
                    event.hadithNumber
                )
            }

            is HadithEvent.FilterByGrade -> {
                telemetry.featureUsed(AppAnalytics.Feature.HADITH, "filter_by_grade")
                filterByGrade(event.grade)
            }
            is HadithEvent.ToggleBookmark -> {
                telemetry.featureUsed(
                    AppAnalytics.Feature.HADITH,
                    "toggle_bookmark"
                )
                toggleBookmark(
                    event.hadithId,
                    event.bookId,
                    event.hadithNumber
                )
            }

            is HadithEvent.NavigateToHadith -> {
                // Re-anchor, so the hadith the reader is *now* on is the one a content refresh
                // restores — not the one it was opened at.
                anchorHadithId = _readerState.value.hadiths.getOrNull(event.index)?.id
                _readerState.update { it.copy(currentHadithIndex = event.index) }
            }
            is HadithEvent.SetFontSize -> _readerState.update { it.copy(fontSize = event.size) }
            is HadithEvent.SetArabicFontSize -> _readerState.update { it.copy(arabicFontSize = event.size) }
            HadithEvent.ToggleArabic -> _readerState.update { it.copy(showArabic = !it.showArabic) }
            HadithEvent.ClearSearch -> {
                searchJob?.cancel()
                _searchState.update { HadithSearchUiState() }
                _chaptersState.update { it.copy(searchQuery = "") }
            }

            HadithEvent.LoadAllBooks -> loadAllBooks()
            HadithEvent.LoadBookmarks -> loadBookmarks()
            HadithEvent.Retry -> retryFailedLoads()
        }
    }

    /**
     * Re-runs only the surfaces that are actually failing, so a retry tapped in the reader
     * does not also re-fetch the book list behind it.
     */
    private fun retryFailedLoads() {
        if (_collectionState.value.error != null) loadAllBooks()
        if (_chaptersState.value.error != null) lastBookId?.let(::loadBook)
        if (_readerState.value.error != null) lastReaderLoad?.invoke()
    }

    private fun loadAllBooks() {
        _collectionState.update { it.copy(isLoading = true, error = null) }
        launchSafely(
            telemetry, AppAnalytics.Feature.HADITH, "load_all_books",
            onFailure = { throwable ->
                _collectionState.update {
                    it.copy(
                        isLoading = false,
                        error = UiError(
                            message = R.string.hadith_books_load_failed,
                            details = throwable.message,
                        ),
                    )
                }
            },
        ) {
            hadithUseCases.getAllBooks().collect { books ->
                _collectionState.update {
                    it.copy(books = books, isLoading = false)
                }
            }
        }
    }

    /**
     * Best-effort: the hadith of the day is one card on a screen whose real content is the
     * book list, so its failure is reported and dropped rather than shown. Replacing the
     * whole collection screen because one card could not load would be a worse outcome
     * than the missing card.
     */
    private fun loadHadithOfTheDay() {
        launchBestEffort(telemetry, AppAnalytics.Feature.HADITH, "load_hadith_of_the_day") {
            val hadith = hadithUseCases.getHadithOfTheDay()
            _collectionState.update { it.copy(hadithOfTheDay = hadith) }
        }
    }

    private fun loadBook(bookId: String) {
        lastBookId = bookId
        _chaptersState.update { it.copy(isLoading = true, error = null) }
        chaptersJob?.cancel()
        chaptersJob = launchSafely(
            telemetry,
            AppAnalytics.Feature.HADITH,
            "load_book",
            onFailure = { throwable ->
                _chaptersState.update {
                    it.copy(
                        isLoading = false,
                        error = UiError(
                            message = R.string.hadith_chapters_load_failed,
                            details = throwable.message,
                        ),
                    )
                }
            },
        ) {
            // The inner try/catch is gone: launchSafely's onFailure is the one failure path,
            // and duplicating it here meant two places could disagree about what a failed
            // load leaves on screen.
            val book = hadithUseCases.getBookById(bookId)
            _chaptersState.update { it.copy(book = book) }

            hadithUseCases.getChaptersByBook(bookId).collect { chapters ->
                _chaptersState.update { it.copy(chapters = chapters, isLoading = false) }
            }
        }
    }

    /**
     * Opens [chapterId] in the reader, optionally landing on [focusHadithId] rather than the
     * top — which is what "open this hadith" and "open hadith number N" both reduce to, since
     * the reader is a pager over a whole chapter either way.
     *
     * The one load path for all three entry points, so the composite-id and index-resolution
     * rules cannot drift between them: they did, and `loadHadithByNumber` had both wrong.
     */
    private fun loadChapter(chapterId: String) {
        lastReaderLoad = { loadChapter(chapterId) }
        startReaderLoad("load_chapter") { chapterId to null }
    }

    private fun loadHadithById(hadithId: String) {
        lastReaderLoad = { loadHadithById(hadithId) }
        startReaderLoad("load_hadith_by_id") {
            hadithUseCases.getHadithById(hadithId)?.let { it.chapterKey to it.id }
        }
    }

    /**
     * Opens hadith number [hadithNumber] of book [bookId].
     *
     * This is the **only** way to reach a bookmarked hadith: a `HadithBookmark` stores the book
     * and the number printed in the reader, not the database id — see `UnifiedBookmark`, which
     * carries `hadithBookId` and `hadithNumber` and no id at all.
     *
     * Two defects lived in the old two-line body. The chapter id was passed **raw**, while
     * `getChapterById` is keyed on the composite `bookId_chapterId`, so the chapter header
     * resolved to null. And the index was read out of `_readerState` on the line *after*
     * `loadChapter` had launched its own coroutine, so it always saw the **previous** chapter's
     * list: `indexOfFirst` was always -1 and the branch that sets the index could never run.
     */
    private fun loadHadithByNumber(bookId: String, hadithNumber: Int) {
        lastReaderLoad = { loadHadithByNumber(bookId, hadithNumber) }
        startReaderLoad("load_hadith_by_number") {
            hadithUseCases.getHadithByNumber(bookId, hadithNumber)?.let { it.chapterKey to it.id }
        }
    }

    /**
     * The one reader load path, for all three ways in.
     *
     * [resolve] returns the chapter to open and the hadith to land on within it (null to open at
     * the top), or null if the target does not exist. Everything after that — the chapter
     * header, the collector, the anchor — is identical for all three, and keeping it in one
     * place is why the composite-id and index rules can no longer drift between them.
     *
     * Deliberately one coroutine: resolving the hadith and then collecting its chapter used to
     * be two, and the second cancelled `readerJob` — the handle owned by the first — from inside
     * it.
     */
    private fun startReaderLoad(
        errorType: String,
        resolve: suspend () -> Pair<String, String?>?
    ) {
        _readerState.update { it.copy(isLoading = true, error = null) }
        readerJob?.cancel()
        readerJob = launchSafely(
            telemetry,
            AppAnalytics.Feature.HADITH,
            errorType,
            onFailure = { throwable ->
                _readerState.update {
                    it.copy(
                        isLoading = false,
                        error = UiError(
                            message = R.string.hadith_load_failed,
                            details = throwable.message,
                        ),
                    )
                }
            },
        ) {
            val target = resolve()
            if (target == null) {
                // A hadith that is not in the collection is an answer, not a failure:
                // nothing went wrong, so nothing is reported to telemetry — but the
                // reader is still told, and told in their own language. This used to set
                // the English literal "Hadith not found" as the state's error string.
                _readerState.update {
                    it.copy(
                        isLoading = false,
                        error = UiError(
                            message = R.string.hadith_not_found,
                            kind = NimazErrorKind.NOT_FOUND,
                        ),
                    )
                }
                return@launchSafely
            }
            val (chapterId, focusHadithId) = target
            anchorHadithId = focusHadithId

            val chapter = hadithUseCases.getChapterById(chapterId)
            _readerState.update { it.copy(chapter = chapter) }

            hadithUseCases.getHadithsByChapter(chapterId).collect { hadiths ->
                // Re-resolved against the list that just arrived. On the first emission the
                // anchor is the requested hadith (or absent, meaning the top); on a later
                // one it is wherever the reader already is — a content refresh re-emits, and
                // must not move it.
                val index = anchorHadithId
                    ?.let { id -> hadiths.indexOfFirst { it.id == id } }
                    ?.takeIf { it >= 0 }
                    ?: 0
                anchorHadithId = hadiths.getOrNull(index)?.id
                _readerState.update {
                    it.copy(hadiths = hadiths, isLoading = false, currentHadithIndex = index)
                }
            }
        }
    }

    /**
     * Replaces the reader's list with every hadith of [grade], across all chapters.
     *
     * Still reachable from no screen (#357). It is fixed rather than deleted because leaving it
     * as-is left a trap: it swapped `hadiths` while leaving `chapter` naming the chapter the
     * reader came from and `currentHadithIndex` pointing into the old list — so the first screen
     * to wire it would have rendered a foreign chapter header over the results and opened at
     * whatever index the previous chapter happened to be on. Both are cleared with the swap.
     */
    private fun filterByGrade(grade: HadithGrade) {
        readerJob?.cancel()
        anchorHadithId = null
        readerJob = launchSafely(
            telemetry, AppAnalytics.Feature.HADITH, "filter_by_grade",
            onFailure = { throwable ->
                _readerState.update {
                    it.copy(
                        isLoading = false,
                        error = UiError(
                            message = R.string.hadith_load_failed,
                            details = throwable.message,
                        ),
                    )
                }
            },
        ) {
            hadithUseCases.getHadithsByGrade(grade).collect { hadiths ->
                _readerState.update {
                    it.copy(
                        hadiths = hadiths,
                        chapter = null,
                        currentHadithIndex = 0,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * A write: a failed bookmark toggle is reported and dropped rather than shown, because
     * the hadith on screen is still perfectly readable and blanking it to report a failed
     * star would be the worse outcome.
     */
    private fun toggleBookmark(hadithId: String, bookId: String, hadithNumber: Int) {
        launchBestEffort(telemetry, AppAnalytics.Feature.HADITH, "toggle_bookmark") {
            hadithUseCases.toggleBookmark(hadithId, bookId, hadithNumber)
        }
    }

    /**
     * Feeds the "bookmarked" count on the collection screen. Best-effort: a wrong count on
     * one stat tile is not worth replacing a working book list over.
     */
    private fun loadBookmarks() {
        launchBestEffort(telemetry, AppAnalytics.Feature.HADITH, "load_bookmarks") {
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
        launchBestEffort(telemetry, AppAnalytics.Feature.HADITH, "observe_hadith_settings") {
            val displayFlow = combine(
                hadithSettings.hadithArabicFont,
                hadithSettings.hadithArabicFontSize,
                hadithSettings.hadithTranslationFontSize
            ) { fontId, arabicSize, transSize -> Triple(fontId, arabicSize, transSize) }

            val toggleFlow = combine(
                hadithSettings.hadithShowArabic,
                hadithSettings.hadithShowTranslation,
                hadithSettings.hadithShowGrade,
                hadithSettings.hadithShowChain
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
