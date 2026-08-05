@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.arshadshah.nimaz.presentation.viewmodel.quran

import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.data.audio.AudioState
import com.arshadshah.nimaz.data.audio.QuranAudioManager
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamDetailSnapshot
import com.arshadshah.nimaz.domain.model.KhatamInsights
import com.arshadshah.nimaz.domain.model.MushafPageLayout
import com.arshadshah.nimaz.domain.model.MushafPagination
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.QuranFavorite
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.QuranTranslation
import com.arshadshah.nimaz.domain.model.ReadingProgress
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.AyahTheme
import com.arshadshah.nimaz.domain.model.SurahInfo
import com.arshadshah.nimaz.domain.model.SurahOverview
import com.arshadshah.nimaz.domain.model.SurahWithAyahs
import com.arshadshah.nimaz.domain.model.TranslationLanguage
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.arshadshah.nimaz.presentation.theme.QuranArabicFont
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

enum class ReadingMode { SURAH, JUZ, PAGE }

/**
 * A Quran favourite enriched for the Favourites tab — the stored [QuranFavorite] plus the
 * ayah's Arabic text, so the tab can show the same rich card (badge, timestamp, Arabic
 * preview, overflow menu) as the Bookmarks screen.
 */
data class FavoriteAyahUi(
    val ayahId: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val arabicText: String?,
    val createdAt: Long,
)

@HiltViewModel
class QuranViewModel @Inject constructor(
    private val quranUseCases: QuranUseCases,
    val audioManager: QuranAudioManager,
    private val settingsRepository: SettingsRepository,
    private val khatamUseCases: KhatamUseCases,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _homeState = MutableStateFlow(QuranHomeUiState())
    val homeState: StateFlow<QuranHomeUiState> = _homeState.asStateFlow()

    private val _readerState = MutableStateFlow(QuranReaderUiState())
    val readerState: StateFlow<QuranReaderUiState> = _readerState.asStateFlow()

    private val _searchState = MutableStateFlow(QuranSearchUiState())
    val searchState: StateFlow<QuranSearchUiState> = _searchState.asStateFlow()

    private val _bookmarksState = MutableStateFlow(QuranBookmarksUiState())
    val bookmarksState: StateFlow<QuranBookmarksUiState> = _bookmarksState.asStateFlow()

    private val _surahInfo = MutableStateFlow<SurahInfo?>(null)
    val surahInfo: StateFlow<SurahInfo?> = _surahInfo.asStateFlow()

    /**
     * The surah's long-form background and its passage outline (schemaVersion 24).
     *
     * Kept beside [surahInfo] rather than folded into it: they are different content with
     * different provenance and different sizes — one row of one sentence against ~8 KB of prose
     * per surah — and the surah *list* reads the first for 114 rows and must never touch the
     * second.
     */
    private val _surahThematic = MutableStateFlow(SurahThematicUiState())
    val surahThematic: StateFlow<SurahThematicUiState> = _surahThematic.asStateFlow()

    val audioState: StateFlow<AudioState> = audioManager.audioState

    /**
     * The passage outline for the surah the reader is on, so the verse list can print a heading
     * where the mushaf's own outline starts a new subject.
     *
     * Surah mode only, and deliberately: a juz spans up to a dozen surahs and a page can span
     * two, so covering them would mean a query per surah on every page turn to label a boundary
     * that mode rarely crosses. Surah mode is where a reader is reading *through* something,
     * which is the case a passage heading is for.
     */
    private fun loadReaderPassages(surahNumber: Int) {
        passagesJob?.cancel()
        passagesJob = viewModelScope.launch {
            val passages = quranUseCases.getSurahThemes(surahNumber)
            _readerState.update { state ->
                // A slower load for a surah the reader has since left must not repaint headings
                // over a different surah's verses.
                if (readerTarget != ReaderTarget.Surah(surahNumber)) state
                else state.copy(passages = passages)
            }
        }
    }

    private var passagesJob: Job? = null

    /**
     * One job for the info screen's thematic load, cancelled when a different surah asks.
     *
     * The screen is reachable from the surah list, from a deep link and from the reader, so two
     * surahs can be requested in quick succession — without this, the slower of the two wins.
     */
    private var surahThematicJob: Job? = null

    // Debounced search support
    private val searchQueryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    /**
     * What the reader is currently showing. The ayah queries take the translator id as a
     * *parameter*, captured when the flow is subscribed, so changing translation does not
     * propagate to an already-running collector — without this the reader keeps rendering the
     * previous translation until the user navigates away and back.
     */
    private sealed interface ReaderTarget {
        data class Surah(val number: Int) : ReaderTarget
        data class Juz(val number: Int) : ReaderTarget
        data class Page(val number: Int) : ReaderTarget
    }

    private var readerTarget: ReaderTarget? = null

    /**
     * The in-flight collector for the *single-target* reading modes (surah, juz). Each load
     * cancels the previous one: these collect Room flows, which never complete, so without
     * cancelling, every surah the user opened would keep its collector alive and racing to
     * write [_readerState] — visible as the previous surah's content flicking back over the
     * new one.
     *
     * Page loads deliberately do **not** share this handle — see [pageJobs].
     */
    private var contentJob: Job? = null

    /**
     * In-flight page collectors, keyed by page number.
     *
     * Page mode is not single-target: the reader's pager keeps the settled page *and* its
     * neighbours composed, and each of them asks for its own content in the same frame. When
     * these shared [contentJob], every request cancelled the one before it, so only the last
     * page requested in a frame ever reached [QuranReaderUiState.pageCache] — the losers
     * rendered as an empty Mushaf frame and stayed blank until they left and re-entered
     * composition. That only ever hit the ayah-flow editions (Madani); the line-accurate
     * IndoPak layouts render from [loadMushafPageLayout], which never shared a job.
     *
     * One collector per page, cancelled wholesale by [cancelPageJobs] when the pages
     * themselves stop being valid (a script or translation change) or when the reader leaves
     * page mode.
     */
    private val pageJobs = mutableMapOf<Int, Job>()

    /**
     * Active khatam plus everything derived from it, as one stream shared by the reader
     * and home.
     *
     * MUST stay declared above `init`: property initialisers run in declaration order, so
     * a field declared below `init` is still null when `init` starts these collectors —
     * including a `by lazy` delegate, which is itself such a field.
     *
     * Previously each caller nested `observeReadAyahIds(...).collect` *inside*
     * `observeActiveKhatam().collect`. `collect` on a Room Flow never returns, so the outer
     * flow could never process a second emission and both surfaces stayed pinned to the
     * first khatam until process death. `flatMapLatest` cancels the inner subscription when
     * the active khatam changes.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeKhatamStream: Flow<KhatamDetailSnapshot?> =
        khatamUseCases.observeActiveKhatam()
            .flatMapLatest { khatam ->
                if (khatam == null) flowOf(null)
                else khatamUseCases.observeKhatamDetail(khatam.id)
            }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    init {
        loadSurahs()
        loadReadingProgress()
        loadBookmarks()
        loadFavorites()
        loadFavoriteAyahIds()
        observeMushafPagination()
        loadRukuCounts()
        loadVerseOfTheDay()
        observeQuranSettings()
        setupDebouncedSearch()
        observeActiveKhatam()
        observeActiveKhatamForHome()
    }

    @OptIn(FlowPreview::class)
    private fun setupDebouncedSearch() {
        searchQueryFlow
            .debounce(300L)
            .distinctUntilChanged()
            .onEach { query -> performSearch(query) }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: QuranEvent) {
        when (event) {
            is QuranEvent.LoadSurah -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.QURAN, "open_surah")
                loadSurah(event.surahNumber)
            }
            is QuranEvent.LoadJuz -> loadJuz(event.juzNumber)
            is QuranEvent.LoadPage -> loadPage(event.pageNumber)
            is QuranEvent.PrefetchPage -> loadPage(event.pageNumber, makeActive = false)
            is QuranEvent.LoadMushafPageLayout -> loadMushafPageLayout(event.pageNumber)
            is QuranEvent.Search -> {
                AppAnalytics.logSearch("quran", event.query.trim().length)
                search(event.query)
            }
            is QuranEvent.SetTopTab -> _homeState.update { it.copy(topTab = event.index) }
            is QuranEvent.SetTab -> _homeState.update { it.copy(selectedTab = event.index) }
            is QuranEvent.ToggleBookmark -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.QURAN, "toggle_bookmark")
                toggleBookmark(
                    event.ayahId,
                    event.surahNumber,
                    event.ayahNumber
                )
            }

            is QuranEvent.ToggleFavorite -> toggleFavorite(
                event.ayahId,
                event.surahNumber,
                event.ayahNumber
            )

            is QuranEvent.RemoveFavorite -> removeFavorite(event.favorite)
            QuranEvent.UndoRemoveFavorite -> undoRemoveFavorite()
            QuranEvent.DismissFavoriteUndo ->
                _homeState.update { it.copy(recentlyRemovedFavorite = null) }

            is QuranEvent.UpdateReadingPosition -> updateReadingPosition(
                event.surah,
                event.ayah,
                event.page,
                event.juz
            )

            QuranEvent.ToggleTranslation -> {
                val newValue = !_readerState.value.showTranslation
                _readerState.update { it.copy(showTranslation = newValue) }
                viewModelScope.launch { settingsRepository.setShowTranslation(newValue) }
            }

            QuranEvent.ClearSearch -> {
                _searchState.update { QuranSearchUiState() }
                _homeState.update { it.copy(searchQuery = "", filteredSurahs = it.surahs) }
            }

            is QuranEvent.PlaySurahAudio -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.QURAN, "play_surah_audio")
                playSurahAudio(event.surahNumber, event.surahName)
            }
            is QuranEvent.PlayAyahAudio -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.QURAN, "play_ayah_audio")
                playAyahAudio(
                    event.ayahGlobalId,
                    event.surahNumber,
                    event.ayahNumber
                )
            }

            QuranEvent.PauseAudio -> audioManager.togglePlayPause()
            QuranEvent.ResumeAudio -> audioManager.togglePlayPause()
            QuranEvent.StopAudio -> audioManager.stop()
            is QuranEvent.PlaySurahFromInfo -> playSurahFromInfo(event.surahNumber)
            is QuranEvent.LoadSurahInfo -> loadSurahInfo(event.surahNumber)
            is QuranEvent.MarkAyahsReadForKhatam -> markAyahsReadForKhatam(event.ayahIds)
            is QuranEvent.UnmarkAyahReadForKhatam -> unmarkAyahReadForKhatam(event.ayahId)
            is QuranEvent.ToggleKhatamAyah -> toggleKhatamAyah(event.ayahId)
            is QuranEvent.MarkSurahAsReadForKhatam -> markSurahAsReadForKhatam(event.surahNumber)
            is QuranEvent.TogglePageKhatam -> togglePageKhatam(event.ayahIds)
        }
    }

    private fun observeQuranSettings() {
        viewModelScope.launch {
            // DataStore's first emission is hydration, not a change the user made — and the
            // reader state it is compared against holds compiled-in defaults until it arrives.
            // Treating it as a change re-issued a load the reader had already performed with
            // the right values (see `translatorId`/`mushafScript`), and for a non-default
            // edition it repaginated a page number *from* an edition the reader was never on.
            var hydrated = false
            // Split into two groups of 4 to use typed combine overloads
            val displayFlow = combine(
                settingsRepository.quranTranslatorId,
                settingsRepository.showTranslation,
                settingsRepository.showTransliteration,
                settingsRepository.quranArabicFontSize,
                settingsRepository.quranArabicFont
            ) { translatorId: String, showTrans: Boolean, showTranslit: Boolean, arabicSize: Float, fontId: String ->
                QuranDisplaySettings(translatorId, showTrans, showTranslit, arabicSize, fontId)
            }

            val behaviorFlow = combine(
                settingsRepository.quranTranslationFontSize,
                settingsRepository.continuousReading,
                settingsRepository.keepScreenOn,
                settingsRepository.selectedReciterId
            ) { transSize: Float, continuous: Boolean, keepOn: Boolean, reciter: String? ->
                QuranBehaviorSettings(transSize, continuous, keepOn, reciter)
            }

            combine(
                displayFlow,
                behaviorFlow,
                settingsRepository.showTajweed,
                settingsRepository.tajweedUnderline,
                settingsRepository.quranMushafScript
            ) { display, behavior, showTajweed, tajweedUnderline, mushafScript ->
                QuranReaderSettings(display, behavior, showTajweed, tajweedUnderline,
                    MushafScript.fromName(mushafScript))
            }.collect { settings ->
                val (display, behavior, showTajweed, tajweedUnderline, mushafScript) = settings
                // Captured before the state update below overwrites them.
                val translationChanged = hydrated &&
                    _readerState.value.selectedTranslatorId != display.translatorId
                // A different edition repaginates the Quran, so page N no longer holds the
                // same ayahs.
                val previousScript = _readerState.value.mushafScript
                val scriptChanged = hydrated && previousScript != mushafScript
                hydrated = true
                audioManager.setReciter(behavior.reciterId)
                // Push continuous-reading reactively so toggling the setting while
                // in the reader takes effect immediately, not on next play-start.
                audioManager.setContinuousPlayback(behavior.continuousReading)
                _readerState.update {
                    // Drop the per-page caches on a script change rather than render the
                    // previous edition's content under the new page numbers (#325).
                    // A different translation invalidates the cached ayahs too — they carry
                    // the translation text they were fetched with. The layout cache is
                    // script-only and survives.
                    it.copy(
                        pageCache = if (scriptChanged || translationChanged) {
                            emptyMap()
                        } else {
                            it.pageCache
                        },
                        mushafPageLayoutCache =
                            if (scriptChanged) emptyMap() else it.mushafPageLayoutCache,
                        selectedTranslatorId = display.translatorId,
                        showTranslation = display.showTranslation,
                        showTransliteration = display.showTransliteration,
                        arabicFontSize = display.arabicFontSize,
                        arabicFontFamily = QuranArabicFont.fromId(display.arabicFontId).fontFamily,
                        fontSize = behavior.translationFontSize,
                        continuousReading = behavior.continuousReading,
                        keepScreenOn = behavior.keepScreenOn,
                        showTajweed = showTajweed,
                        tajweedUnderline = tajweedUnderline,
                        mushafScript = mushafScript
                    )
                }
                _homeState.update { it.copy(mushafScript = mushafScript) }

                if (translationChanged || scriptChanged) {
                    // The queries take the translator id and script as parameters captured at
                    // subscription, so an already-running collector keeps serving the old
                    // one — and it would re-populate the caches just cleared above. Drop
                    // every page collector and re-issue the current load.
                    cancelPageJobs()
                    // A script change repaginates the whole Quran, so a page target has to be
                    // re-resolved rather than re-issued. Done here rather than in
                    // `observeMushafPagination` so one collector owns the reload — the two
                    // observe the same preference independently and are not ordered against
                    // each other, so splitting it would race `cancelPageJobs` above.
                    reloadReaderContent(
                        repaginateFrom = previousScript.takeIf { scriptChanged }
                    )
                }
                if (translationChanged) {
                    // The home verse of the day is a one-shot read of the same preference.
                    loadVerseOfTheDay()
                }
            }
        }
    }

    /**
     * Re-fetches whatever the reader is showing, after a settings change that invalidates its
     * content. A no-op before the first load, so it is safe to call from the settings
     * observer's very first emission.
     *
     * Surah and juz numbers mean the same thing in every edition, so they are re-issued as-is.
     * A **page** number does not: page 500 of the 604-page Madani mushaf and page 500 of the
     * 847-page 13-line IndoPak are hundreds of ayahs apart, and Madani page 600 does not exist
     * in the 548-page edition at all — re-issuing the raw number threw the reader onto
     * unrelated text, or onto a page that loads nothing and renders blank. Pass
     * [repaginateFrom] (the edition in force *before* the change) to carry the position across
     * instead of the integer.
     */
    private suspend fun reloadReaderContent(repaginateFrom: MushafScript? = null) {
        when (val target = readerTarget) {
            is ReaderTarget.Surah -> loadSurah(target.number)
            is ReaderTarget.Juz -> loadJuz(target.number)
            is ReaderTarget.Page -> loadPage(repaginate(target.number, repaginateFrom))
            null -> Unit
        }
    }

    /**
     * [page], expressed in the edition now active.
     *
     * Falls back to clamping when the position cannot be resolved — either edition may have no
     * page ranges loaded — because landing near the right end of the book beats loading a page
     * the edition does not have.
     */
    private suspend fun repaginate(page: Int, from: MushafScript?): Int {
        val script = _readerState.value.mushafScript
        if (from == null || from == script) return page
        // Both mappings are resolved here rather than read off state. `observeMushafPagination`
        // publishes into the same `pagination` field from its own collector, and the two
        // observe the preference independently — so by the time this runs, the "previous"
        // mapping on state may already have been replaced by the new one, which would make
        // the remap a no-op. The repository memoises a line-accurate edition's ranges, so
        // asking for them again is cheap.
        val to = quranUseCases.getMushafPagination(script)
        val resolved = to.pageMatching(page, quranUseCases.getMushafPagination(from)) ?: page
        return resolved.coerceIn(1, to.totalPages.coerceAtLeast(1))
    }

    private data class QuranDisplaySettings(
        val translatorId: String,
        val showTranslation: Boolean,
        val showTransliteration: Boolean,
        val arabicFontSize: Float,
        val arabicFontId: String
    )

    private data class QuranBehaviorSettings(
        val translationFontSize: Float,
        val continuousReading: Boolean,
        val keepScreenOn: Boolean,
        val reciterId: String?
    )

    /** Aggregate of the reader settings observed in [observeQuranSettings]; combine() tops out
     *  at five sources, so the display/behavior sub-groups are folded together here. */
    private data class QuranReaderSettings(
        val display: QuranDisplaySettings,
        val behavior: QuranBehaviorSettings,
        val showTajweed: Boolean,
        val tajweedUnderline: Boolean,
        val mushafScript: MushafScript
    )

    private fun loadSurahInfo(surahNumber: Int) {
        viewModelScope.launch {
            _surahInfo.value = quranUseCases.getSurahInfo(surahNumber)
        }
        loadSurahThematic(surahNumber)
    }

    /**
     * The background and passage outline, in one job.
     *
     * Both come from the same artifact and are shown on the same screen, so loading them
     * separately would only buy two spinners that finish at the same time. A surah whose
     * overview is absent is not an error — see [SurahThematicUiState.isAvailable].
     */
    private fun loadSurahThematic(surahNumber: Int) {
        surahThematicJob?.cancel()
        surahThematicJob = viewModelScope.launch {
            _surahThematic.value = SurahThematicUiState(isLoading = true)
            val overview = quranUseCases.getSurahOverview(surahNumber)
            val themes = quranUseCases.getSurahThemes(surahNumber)
            val subjects = quranUseCases.getTopicsForSurah.count(surahNumber)
            _surahThematic.value = SurahThematicUiState(
                overview = overview,
                passages = themes,
                subjectCount = subjects,
                isLoading = false,
            )
        }
    }

    private fun playSurahFromInfo(surahNumber: Int) {
        viewModelScope.launch {
            quranUseCases.getSurahWithAyahs(surahNumber, translatorId())
                .first()?.let { surahWithAyahs ->
                    val audioItems = surahWithAyahs.ayahs.map { ayah ->
                        QuranAudioManager.AyahAudioItem(
                            ayahGlobalId = ayah.id,
                            surahNumber = ayah.surahNumber,
                            ayahNumber = ayah.ayahNumber
                        )
                    }
                    // SurahInfoScreen always plays full surah continuously
                    audioManager.setContinuousPlayback(true)
                    audioManager.playSurah(
                        surahNumber,
                        surahWithAyahs.surah.nameEnglish,
                        audioItems
                    )
                }
        }
    }

    private fun playSurahAudio(surahNumber: Int, surahName: String) {
        val ayahs = _readerState.value.ayahs.ifEmpty {
            _readerState.value.surahWithAyahs?.ayahs ?: emptyList()
        }
        if (ayahs.isEmpty()) return
        val audioItems = ayahs.map { ayah ->
            QuranAudioManager.AyahAudioItem(
                ayahGlobalId = ayah.id,
                surahNumber = ayah.surahNumber,
                ayahNumber = ayah.ayahNumber
            )
        }
        // Respect continuousReading setting from QuranReaderScreen
        audioManager.setContinuousPlayback(_readerState.value.continuousReading)
        audioManager.playSurah(surahNumber, surahName, audioItems)
    }

    private fun playAyahAudio(ayahGlobalId: Int, surahNumber: Int, ayahNumber: Int) {
        val ayahs = _readerState.value.ayahs.ifEmpty {
            _readerState.value.surahWithAyahs?.ayahs ?: emptyList()
        }
        val audioItems = ayahs.map { ayah ->
            QuranAudioManager.AyahAudioItem(
                ayahGlobalId = ayah.id,
                surahNumber = ayah.surahNumber,
                ayahNumber = ayah.ayahNumber
            )
        }
        val title = _readerState.value.title.ifEmpty {
            context.getString(
                R.string.quran_home_surah_fallback,
                surahNumber
            )
        }
        // Respect continuousReading setting from QuranReaderScreen
        audioManager.setContinuousPlayback(_readerState.value.continuousReading)
        audioManager.playFromAyah(ayahGlobalId, audioItems, title)
    }

    private fun loadSurahs() {
        viewModelScope.launch {
            val thematic = quranUseCases.hasThematicContent()
            _homeState.update { it.copy(hasThematicContent = thematic) }
        }
        viewModelScope.launch {
            // Collected directly. Wrapping it in `stateIn(…, emptyList())` first published the
            // seed — surahs = [], isLoading = false — before Room had produced a row, so the
            // list rendered its "nothing here" state for a frame on every cold open.
            quranUseCases.getSurahList()
                .collect { surahs ->
                    _homeState.update { state ->
                        state.copy(
                            surahs = surahs,
                            filteredSurahs = filterSurahs(surahs, state.searchQuery),
                            isLoading = false
                        )
                    }
                }
        }
    }

    /**
     * Rukūʿ counts for the surah list's structure badges. Independent of the Mushaf edition —
     * a surah's sections are a property of the surah, not of a pagination.
     */
    private fun loadRukuCounts() {
        viewModelScope.launch {
            quranUseCases.getSurahRukuCounts().collect { counts ->
                _homeState.update { it.copy(rukuCounts = counts) }
            }
        }
    }

    /**
     * Keeps the page↔ayah mapping in step with the Mushaf layout setting (#325).
     *
     * Switching editions repaginates the whole Quran, so this re-derives the mapping rather
     * than leaving the Page tab and khatam page progress on the Madani 604. The fallback is
     * published first so the default edition renders immediately while the real ranges load
     * (for the 16-line edition that load also triggers its one-time seeding).
     */
    private fun observeMushafPagination() {
        viewModelScope.launch {
            settingsRepository.quranMushafScript
                .map { MushafScript.fromName(it) }
                .distinctUntilChanged()
                .collect { script ->
                    val fallback = MushafPagination.fallback(script)
                    _homeState.update { it.copy(pagination = fallback) }
                    _readerState.update { it.copy(pagination = fallback) }
                    val pagination = quranUseCases.getMushafPagination(script)
                    _homeState.update { it.copy(pagination = pagination) }
                    // The reader's pager bounds read off this too, so it must not be left on
                    // the declared count once the real ranges are in.
                    _readerState.update { it.copy(pagination = pagination) }
                }
        }
    }

    /**
     * The translation content is fetched with, read from where it is persisted.
     *
     * [QuranReaderUiState.selectedTranslatorId] carries a compiled-in default —
     * `sahih_international` — and is only replaced once [observeQuranSettings]' first emission
     * lands, which is after DataStore's first read and so generally *after* the screen has
     * already asked for a surah. Reading it off the state handed every non-default user English
     * first, then `translationChanged` re-issued the entire query: a visible flash **and** a
     * wasted full-surah read on every reader open. [loadVerseOfTheDay] already resolved the
     * preference this way; the reader paths did not.
     */
    private suspend fun translatorId(): String = settingsRepository.quranTranslatorId.first()

    /**
     * The Mushaf edition content is fetched with, read from where it is persisted — the same
     * pre-hydration hazard as [translatorId], and worse in page mode, because an edition
     * decides which ayahs page N even holds.
     */
    private suspend fun mushafScript(): MushafScript =
        MushafScript.fromName(settingsRepository.quranMushafScript.first())

    private fun loadVerseOfTheDay() {
        viewModelScope.launch {
            val translatorId = translatorId()
            val epochDay = java.time.LocalDate.now().toEpochDay()
            val verse = quranUseCases.getVerseOfTheDay(epochDay, translatorId)
            _homeState.update { it.copy(verseOfTheDay = verse) }
        }
    }

    private fun loadReadingProgress() {
        viewModelScope.launch {
            quranUseCases.getReadingProgress()
                .collect { progress ->
                    _homeState.update { it.copy(readingProgress = progress) }
                }
        }
    }

    private fun loadBookmarks() {
        viewModelScope.launch {
            quranUseCases.getBookmarks()
                .collect { bookmarks ->
                    _bookmarksState.update { it.copy(bookmarks = bookmarks, isLoading = false) }
                }
        }
    }

    private fun loadSurah(surahNumber: Int) {
        readerTarget = ReaderTarget.Surah(surahNumber)
        _readerState.update {
            it.copy(
                isLoading = true,
                error = null,
                readingMode = ReadingMode.SURAH,
                surahWithAyahs = null,
                ayahs = emptyList(),
                title = "",
                subtitle = "",
                passages = emptyList()
            )
        }
        loadReaderPassages(surahNumber)
        contentJob?.cancel()
        cancelPageJobs()
        contentJob = viewModelScope.launch {
            quranUseCases.getSurahWithAyahs(surahNumber, translatorId())
                .collect { surahWithAyahs ->
                    _readerState.update {
                        it.copy(
                            surahWithAyahs = surahWithAyahs,
                            ayahs = surahWithAyahs?.ayahs ?: emptyList(),
                            title = surahWithAyahs?.surah?.nameEnglish ?: "",
                            subtitle = surahWithAyahs?.let { s ->
                                context.resources.getQuantityString(
                                    R.plurals.quran_surah_ayahs_format,
                                    s.surah.numberOfAyahs,
                                    s.surah.number,
                                    s.surah.numberOfAyahs
                                )
                            } ?: "",
                            isLoading = false,
                            readingMode = ReadingMode.SURAH
                        )
                    }
                }
        }
    }

    private fun loadJuz(juzNumber: Int) {
        readerTarget = ReaderTarget.Juz(juzNumber)
        _readerState.update {
            it.copy(
                isLoading = true,
                error = null,
                readingMode = ReadingMode.JUZ,
                surahWithAyahs = null,
                ayahs = emptyList(),
                title = context.getString(R.string.quran_juz_number_format, juzNumber),
                subtitle = ""
            )
        }
        contentJob?.cancel()
        cancelPageJobs()
        contentJob = viewModelScope.launch {
            quranUseCases.getAyahsByJuz(juzNumber, translatorId())
                .collect { ayahs ->
                    _readerState.update {
                        it.copy(
                            ayahs = ayahs,
                            title = context.getString(R.string.quran_juz_number_format, juzNumber),
                            subtitle = "${ayahs.size} Ayahs",
                            isLoading = false
                        )
                    }
                }
        }
    }

    /**
     * Fetches [pageNumber] into the page cache, and — when [makeActive] — makes it the page
     * the reader is on (title, `ayahs`, reading position, the target a settings change
     * re-issues). Neighbouring pager pages prefetch with `makeActive = false` so a page the
     * user has not swiped to yet cannot retitle the reader.
     */
    private fun loadPage(pageNumber: Int, makeActive: Boolean = true) {
        if (makeActive) {
            readerTarget = ReaderTarget.Page(pageNumber)
            // Page mode is not surah/juz mode: stop the single-target collector rather than
            // let it keep writing its ayahs over the reader state.
            contentJob?.cancel()
            contentJob = null
            pruneDistantPageJobs(pageNumber)
        }

        // Check cache first - no loading needed if already cached
        val cached = _readerState.value.pageCache[pageNumber]
        if (cached != null) {
            if (makeActive) {
                _readerState.update {
                    it.copy(
                        ayahs = cached,
                        readingMode = ReadingMode.PAGE,
                        title = context.getString(R.string.page_single_format, pageNumber),
                        subtitle = "${cached.size} Ayahs",
                        isLoading = false
                    )
                }
            }
            return
        }

        if (makeActive) {
            // Load from database - DON'T clear ayahs to prevent flicker
            _readerState.update {
                it.copy(
                    error = null,
                    readingMode = ReadingMode.PAGE,
                    surahWithAyahs = null,
                    // Keep existing ayahs to prevent flicker during page transition
                    title = context.getString(R.string.page_single_format, pageNumber),
                    subtitle = ""
                )
            }
        }

        // One collector per page: a page already being fetched (typically requested a frame
        // earlier as a neighbour's prefetch) needs no second subscription, and re-launching
        // would throw away the emissions of the first.
        if (pageJobs[pageNumber]?.isActive == true) return

        pageJobs[pageNumber] = viewModelScope.launch {
            // Resolved through the active edition: in the 16-line view page N holds a
            // different span of ayahs than Madani page N, and this cache feeds the page
            // info bar, "mark page read" for khatam and the ayah-action lookups (#325).
            quranUseCases.getAyahsByPage(
                pageNumber = pageNumber,
                translatorId = translatorId(),
                script = mushafScript()
            )
                .collect { ayahs ->
                    // Re-read at emission time: a prefetch started while the user was on this
                    // page may only land after they have swiped on, and vice versa.
                    val isActivePage =
                        (readerTarget as? ReaderTarget.Page)?.number == pageNumber
                    _readerState.update {
                        it.copy(
                            ayahs = if (isActivePage) ayahs else it.ayahs,
                            pageCache = it.pageCache + (pageNumber to ayahs),
                            subtitle = if (isActivePage) "${ayahs.size} Ayahs" else it.subtitle,
                            isLoading = if (isActivePage) false else it.isLoading
                        )
                    }
                }
        }
    }

    /**
     * Drops every in-flight page collector. Room flows never complete, so a page fetched
     * under the previous script/translation would otherwise stay subscribed and re-populate
     * the cache the settings change just cleared.
     */
    private fun cancelPageJobs() {
        pageJobs.values.forEach { it.cancel() }
        pageJobs.clear()
    }

    /**
     * Drops the collectors of pages the reader has left behind, keeping [pageJobs] bounded by
     * [PAGE_JOB_WINDOW] rather than by how far the user has read. Cached content survives —
     * only the live subscription goes.
     */
    private fun pruneDistantPageJobs(activePage: Int) {
        val stale = pageJobs.entries.iterator()
        while (stale.hasNext()) {
            val (page, job) = stale.next()
            if (!job.isActive || abs(page - activePage) > PAGE_JOB_WINDOW) {
                job.cancel()
                stale.remove()
            }
        }
    }

    /**
     * Loads the active edition's line-accurate layout for [pageNumber] into the reader
     * state. First invocation triggers that edition's one-time seeding inside the
     * repository, so this only runs when a line-accurate view is actually used — not on
     * every Quran page open.
     */
    private fun loadMushafPageLayout(pageNumber: Int) {
        // Already cached (e.g. a neighbouring pager page pre-loaded it) — nothing to do.
        if (_readerState.value.mushafPageLayoutCache.containsKey(pageNumber)) return
        viewModelScope.launch {
            val layout = quranUseCases.getMushafPageLayout(pageNumber, mushafScript())
            _readerState.update {
                it.copy(
                    mushafPageLayoutCache = it.mushafPageLayoutCache + (pageNumber to layout)
                )
            }
        }
    }

    private fun search(query: String) {
        _homeState.update { it.copy(searchQuery = query) }

        // Always update filtered surahs immediately (no debounce needed for filtering)
        _homeState.update { state ->
            state.copy(filteredSurahs = filterSurahs(state.surahs, query))
        }

        if (query.isBlank()) {
            _searchState.update { QuranSearchUiState() }
            searchQueryFlow.value = ""
            return
        }

        // Mark as searching and trigger debounced search
        _searchState.update { it.copy(query = query, isSearching = true) }
        searchQueryFlow.value = query
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            _searchState.update { QuranSearchUiState() }
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            quranUseCases.searchQuran(query, translatorId())
                .collect { results ->
                    // Populate surah names and limit results to 50 for performance
                    val surahs = _homeState.value.surahs
                    val enrichedResults = results.take(50).map { result ->
                        if (result.surahName.isEmpty()) {
                            val surahName =
                                surahs.find { it.number == result.ayah.surahNumber }?.nameEnglish
                                    ?: context.getString(
                                        R.string.quran_home_surah_fallback,
                                        result.ayah.surahNumber
                                    )
                            result.copy(surahName = surahName)
                        } else {
                            result
                        }
                    }
                    _searchState.update { it.copy(results = enrichedResults, isSearching = false) }
                }
        }
    }

    private fun filterSurahs(surahs: List<Surah>, query: String): List<Surah> {
        return surahs.filter { surah ->
            query.isBlank() ||
                    surah.nameEnglish.contains(query, ignoreCase = true) ||
                    surah.nameTransliteration.contains(query, ignoreCase = true) ||
                    surah.nameArabic.contains(query)
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            quranUseCases.getFavorites()
                .collect { favorites ->
                    // Enrich each favourite with its Arabic text so the Favourites tab can
                    // render the same rich card as the Bookmarks screen.
                    val enriched = favorites.map { fav ->
                        FavoriteAyahUi(
                            ayahId = fav.ayahId,
                            surahNumber = fav.surahNumber,
                            ayahNumber = fav.ayahNumber,
                            arabicText = quranUseCases.getAyahById(fav.ayahId)?.textArabic,
                            createdAt = fav.createdAt,
                        )
                    }
                    _homeState.update { it.copy(favorites = enriched) }
                }
        }
    }

    // toggleFavorite both removes and (on undo) re-adds, so the same call drives the
    // swipe-to-delete removal and its Undo restore.
    private fun removeFavorite(favorite: FavoriteAyahUi) {
        viewModelScope.launch {
            quranUseCases.toggleFavorite(
                favorite.ayahId,
                favorite.surahNumber,
                favorite.ayahNumber
            )
        }
        _homeState.update { it.copy(recentlyRemovedFavorite = favorite) }
    }

    private fun undoRemoveFavorite() {
        val favorite = _homeState.value.recentlyRemovedFavorite ?: return
        viewModelScope.launch {
            quranUseCases.toggleFavorite(
                favorite.ayahId,
                favorite.surahNumber,
                favorite.ayahNumber
            )
        }
        _homeState.update { it.copy(recentlyRemovedFavorite = null) }
    }

    private fun loadFavoriteAyahIds() {
        viewModelScope.launch {
            quranUseCases.getFavoriteAyahIds()
                .collect { ids ->
                    _readerState.update { it.copy(favoriteAyahIds = ids.toSet()) }
                }
        }
    }

    private fun toggleFavorite(ayahId: Int, surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            quranUseCases.toggleFavorite(ayahId, surahNumber, ayahNumber)
        }
    }

    private fun toggleBookmark(ayahId: Int, surahNumber: Int, ayahNumber: Int) {
        // Optimistic local update so the icon changes immediately
        _readerState.update { state ->
            val updatedSurah = state.surahWithAyahs?.let { swa ->
                swa.copy(
                    ayahs = swa.ayahs.map { a ->
                        if (a.id == ayahId) a.copy(isBookmarked = !a.isBookmarked) else a
                    }
                )
            }
            val updatedAyahs = state.ayahs.map { a ->
                if (a.id == ayahId) a.copy(isBookmarked = !a.isBookmarked) else a
            }
            state.copy(surahWithAyahs = updatedSurah, ayahs = updatedAyahs)
        }
        viewModelScope.launch {
            quranUseCases.toggleBookmark(ayahId, surahNumber, ayahNumber)
        }
    }

    private fun updateReadingPosition(surah: Int, ayah: Int, page: Int, juz: Int) {
        viewModelScope.launch {
            quranUseCases.updateReadingPosition(surah, ayah, page, juz)
        }
    }

    /**
     * Active khatam plus its read-ayah set, as one stream shared by the reader and home.
     *
     * Previously each caller nested `observeReadAyahIds(...).collect` *inside*
     * `observeActiveKhatam().collect`. The inner collect on a Room Flow never returns, so
     * the outer flow could never process a second emission — switching the active khatam
     * left both surfaces pinned to the first khatam's ayahs until the process restarted.
     * [flatMapLatest] cancels the inner subscription when the active khatam changes.
     */

    private fun observeActiveKhatam() {
        viewModelScope.launch {
            activeKhatamStream.collect { snapshot ->
                _readerState.update {
                    it.copy(
                        activeKhatamId = snapshot?.khatam?.id,
                        khatamReadAyahIds = snapshot?.readAyahIds ?: emptySet(),
                    )
                }
                val khatam = snapshot?.khatam
                if (khatam != null && snapshot.readAyahIds.size >= Khatam.TOTAL_QURAN_AYAHS) {
                    khatamUseCases.completeKhatam(khatam.id)
                }
            }
        }
    }

    private fun observeActiveKhatamForHome() {
        viewModelScope.launch {
            activeKhatamStream.collect { snapshot ->
                _homeState.update {
                    it.copy(
                        activeKhatam = snapshot?.khatam,
                        activeKhatamInsights = snapshot?.insights,
                        khatamReadAyahIds = snapshot?.readAyahIds ?: emptySet(),
                    )
                }
            }
        }
        viewModelScope.launch {
            khatamUseCases.observeCompletedKhatams().collect { completed ->
                _homeState.update { it.copy(completedKhatamCount = completed.size) }
            }
        }
    }

    private fun toggleKhatamAyah(ayahId: Int) {
        val khatamId = _readerState.value.activeKhatamId ?: return
        val isRead = ayahId in _readerState.value.khatamReadAyahIds
        viewModelScope.launch {
            if (isRead) {
                khatamUseCases.unmarkAyahRead(khatamId, ayahId)
            } else {
                khatamUseCases.markAyahsRead(khatamId, listOf(ayahId))
            }
        }
    }

    private fun markSurahAsReadForKhatam(surahNumber: Int) {
        val khatamId = _readerState.value.activeKhatamId ?: return
        viewModelScope.launch {
            khatamUseCases.markSurahAsRead(khatamId, surahNumber)
        }
    }

    private fun togglePageKhatam(ayahIds: List<Int>) {
        val khatamId = _readerState.value.activeKhatamId ?: return
        val readIds = _readerState.value.khatamReadAyahIds
        val unreadIds = ayahIds.filter { it !in readIds }
        if (unreadIds.isEmpty()) return
        viewModelScope.launch {
            khatamUseCases.markAyahsRead(khatamId, unreadIds)
        }
    }

    private fun unmarkAyahReadForKhatam(ayahId: Int) {
        val khatamId = _readerState.value.activeKhatamId ?: return
        viewModelScope.launch {
            khatamUseCases.unmarkAyahRead(khatamId, ayahId)
        }
    }

    private fun markAyahsReadForKhatam(ayahIds: List<Int>) {
        val khatamId = _readerState.value.activeKhatamId ?: return
        val readIds = _readerState.value.khatamReadAyahIds
        val newIds = ayahIds.filter { it !in readIds }
        if (newIds.isEmpty()) return

        viewModelScope.launch {
            khatamUseCases.markAyahsRead(khatamId, newIds)
        }
    }

    // Intentionally do NOT release the audio manager here. It is @Singleton
    // and outlives any single screen's ViewModel — the foreground service
    // (QuranAudioService) owns the playback lifecycle. Releasing on every
    // NavBackStackEntry pop killed audio whenever the user navigated away
    // from the screen that started it.

    private companion object {
        /**
         * How far from the page the reader is on a page collector is kept alive.
         *
         * The pager composes at most three pages either side of the current one (a dual-page
         * spread plus the spreads on each side), so anything further is a page the user has
         * swiped past. Their flows are Room flows and never complete, so without this a
         * long reading session would leave one live subscription per page visited.
         */
        const val PAGE_JOB_WINDOW = 4
    }
}
