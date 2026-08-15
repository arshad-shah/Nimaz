@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.arshadshah.nimaz.presentation.viewmodel.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchBestEffort
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.core.text.StringProvider
import com.arshadshah.nimaz.core.time.TodayProvider
import com.arshadshah.nimaz.data.audio.AudioState
import com.arshadshah.nimaz.data.audio.QuranAudioManager
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamDetailSnapshot
import com.arshadshah.nimaz.domain.model.KhatamStatus
import com.arshadshah.nimaz.domain.model.MushafPagination
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.SurahInfo
import com.arshadshah.nimaz.domain.repository.settings.QuranPreferences
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.presentation.theme.QuranArabicFont
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranViewModel.Companion.PAGE_JOB_WINDOW
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.math.abs

enum class ReadingMode { SURAH, JUZ, PAGE }

@HiltViewModel
class QuranViewModel @Inject constructor(
    private val quranUseCases: QuranUseCases,
    private val audioManager: QuranAudioManager,
    private val quranSettings: QuranPreferences,
    private val khatamUseCases: KhatamUseCases,
    private val telemetry: Telemetry,
    private val todayProvider: TodayProvider,
    private val strings: StringProvider
) : ViewModel() {

    private val _homeState = MutableStateFlow(QuranHomeUiState())
    val homeState: StateFlow<QuranHomeUiState> = _homeState.asStateFlow()

    private val _readerState = MutableStateFlow(QuranReaderUiState())
    val readerState: StateFlow<QuranReaderUiState> = _readerState.asStateFlow()

    private val _bookmarksState = MutableStateFlow(QuranBookmarksUiState())
    val bookmarksState: StateFlow<QuranBookmarksUiState> = _bookmarksState.asStateFlow()

    private val _surahInfo = MutableStateFlow<SurahInfo?>(null)
    val surahInfo: StateFlow<SurahInfo?> = _surahInfo.asStateFlow()

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
        passagesJob = launchSafely(telemetry, AppAnalytics.Feature.QURAN, "load_reader_passages") {
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
     * Khatams this instance has already asked to complete.
     *
     * `activeKhatamStream` re-emits on any change to the khatam or its read ayahs, and the
     * completion branch is evaluated on every one — so without this, an emission arriving
     * before the completed flag comes back round called `completeKhatam` again.
     */
    private val completedKhatamIds = mutableSetOf<Long>()

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
        loadFavoriteAyahIds()
        observeMushafPagination()
        loadVerseOfTheDay()
        observeQuranSettings()
        observeActiveKhatam()
        observeActiveKhatamForHome()
    }

    fun onEvent(event: QuranEvent) {
        when (event) {
            is QuranEvent.LoadSurah -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "open_surah")
                loadSurah(event.surahNumber)
            }
            // Only `LoadSurah` was logged, so the reader's usage read as "everyone browses by
            // surah" — which is what you see when the other two ways in are not counted.
            is QuranEvent.LoadJuz -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "open_juz")
                loadJuz(event.juzNumber)
            }

            is QuranEvent.LoadPage -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "open_page")
                loadPage(event.pageNumber)
            }

            is QuranEvent.PrefetchPage -> loadPage(event.pageNumber, makeActive = false)
            is QuranEvent.LoadMushafPageLayout -> loadMushafPageLayout(event.pageNumber)
            is QuranEvent.ToggleBookmark -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "toggle_bookmark")
                toggleBookmark(
                    event.ayahId,
                    event.surahNumber,
                    event.ayahNumber
                )
            }

            is QuranEvent.ToggleFavorite -> {
                telemetry.featureUsed(
                    AppAnalytics.Feature.QURAN,
                    AppAnalytics.Action.TOGGLE_FAVORITE
                )
                toggleFavorite(
                    event.ayahId,
                    event.surahNumber,
                    event.ayahNumber
                )
            }

            is QuranEvent.UpdateReadingPosition -> updateReadingPosition(
                event.surah,
                event.ayah,
                event.page,
                event.juz
            )

            QuranEvent.ToggleTranslation -> {
                // Flipped inside the update so the read and the write are one operation. Read
                // first and `observeQuranSettings` — which writes the same field from its own
                // coroutine — can land between them, and the toggle then writes a value derived
                // from a state that no longer exists.
                var newValue = false
                _readerState.update {
                    newValue = !it.showTranslation
                    it.copy(showTranslation = newValue)
                }
                launchSafely(
                    telemetry,
                    AppAnalytics.Feature.QURAN,
                    "on_event"
                ) { quranSettings.setShowTranslation(newValue) }
            }

            // Deliberately unlogged: no screen emits this. The analytics that used to sit here
            // reported zero for "surah audio played" while `PlaySurahFromInfo` below — the branch
            // the play button actually reaches — recorded nothing. A dashboard reading zero for a
            // thing users do daily is worse than no dashboard. The unreachable *handler* is #357's
            // wire-or-delete call, not this layer's.
            is QuranEvent.PlayAyahAudio -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "play_ayah_audio")
                playAyahAudio(
                    event.ayahGlobalId,
                    event.surahNumber,
                    event.ayahNumber
                )
            }

            is QuranEvent.PreviewReciter -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "preview_reciter")
                audioManager.setReciter(event.reciterId, restartIfPlaying = false)
                playAyahAudio(ayahGlobalId = 1, surahNumber = 1, ayahNumber = 1)
            }

            is QuranEvent.SeekAudioTo -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "audio_seek")
                // Whole-surah coordinates: the rail measures the recitation, not the file the
                // player happens to be on. The manager has been able to do this since the
                // playlist work; only the UI never offered it.
                audioManager.seekToTotal(event.positionMs.coerceAtLeast(0L))
            }

            QuranEvent.NextAyahAudio -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "audio_next_ayah")
                audioManager.skipToNext()
            }

            QuranEvent.PreviousAyahAudio -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "audio_previous_ayah")
                audioManager.skipToPrevious()
            }

            is QuranEvent.SetRecitationRepeat -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "audio_repeat")
                audioManager.setRepeat(event.repeat)
            }

            is QuranEvent.SetPlaybackSpeed -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "audio_speed")
                audioManager.setSpeed(event.speed)
            }

            is QuranEvent.SetFollowAlong -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "audio_follow_along")
                audioManager.setFollowAlong(event.enabled)
            }

            QuranEvent.PauseAudio -> audioManager.togglePlayPause()
            QuranEvent.ResumeAudio -> audioManager.togglePlayPause()
            QuranEvent.StopAudio -> audioManager.stop()
            is QuranEvent.PlaySurahFromInfo -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "play_surah_audio")
                playSurahFromInfo(event.surahNumber)
            }

            is QuranEvent.LoadSurahInfo -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "open_surah_info")
                loadSurahInfo(event.surahNumber)
            }
            // Khatam marking is the app's core engagement loop — a reader working through a
            // whole Qur'an — and not one of its events was recorded, so the loop the feature
            // exists for was invisible while `toggle_bookmark` was counted.
            //
            // `MarkAyahsReadForKhatam` and `UnmarkAyahReadForKhatam` were deleted rather than
            // instrumented: adding analytics surfaced them as having no producer in any
            // screen, and `ToggleKhatamAyah` — which the reader does dispatch — already calls
            // `markAyahsRead`/`unmarkAyahRead` for exactly the same effect. Instrumenting them
            // would have produced two more metrics that read zero for ever, which is the
            // defect this issue is about.
            is QuranEvent.SetAyahNote -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "set_ayah_note")
                setAyahNote(event)
            }

            is QuranEvent.ToggleKhatamAyah -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "khatam_toggle_ayah")
                toggleKhatamAyah(event.ayahId)
            }

            is QuranEvent.MarkSurahAsReadForKhatam -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "khatam_mark_surah")
                markSurahAsReadForKhatam(event.surahNumber)
            }

            is QuranEvent.TogglePageKhatam -> {
                telemetry.featureUsed(AppAnalytics.Feature.QURAN, "khatam_toggle_page")
                togglePageKhatam(event.ayahIds)
            }
        }
    }

    private fun observeQuranSettings() {
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "observe_quran_settings") {
            // DataStore's first emission is hydration, not a change the user made — and the
            // reader state it is compared against holds compiled-in defaults until it arrives.
            // Treating it as a change re-issued a load the reader had already performed with
            // the right values (see `translatorId`/`mushafScript`), and for a non-default
            // edition it repaginated a page number *from* an edition the reader was never on.
            var hydrated = false
            // Split into two groups of 4 to use typed combine overloads
            val displayFlow = combine(
                quranSettings.quranTranslatorId,
                quranSettings.showTranslation,
                quranSettings.showTransliteration,
                quranSettings.quranArabicFontSize,
                quranSettings.quranArabicFont
            ) { translatorId: String, showTrans: Boolean, showTranslit: Boolean, arabicSize: Float, fontId: String ->
                QuranDisplaySettings(translatorId, showTrans, showTranslit, arabicSize, fontId)
            }

            val behaviorFlow = combine(
                quranSettings.quranTranslationFontSize,
                quranSettings.continuousReading,
                quranSettings.keepScreenOn,
                quranSettings.selectedReciterId
            ) { transSize: Float, continuous: Boolean, keepOn: Boolean, reciter: String? ->
                QuranBehaviorSettings(transSize, continuous, keepOn, reciter)
            }

            combine(
                displayFlow,
                behaviorFlow,
                quranSettings.showTajweed,
                quranSettings.tajweedUnderline,
                quranSettings.quranMushafScript
            ) { display, behavior, showTajweed, tajweedUnderline, mushafScript ->
                QuranReaderSettings(
                    display, behavior, showTajweed, tajweedUnderline,
                    MushafScript.fromName(mushafScript)
                )
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
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "load_surah_info") {
            _surahInfo.value = quranUseCases.getSurahInfo(surahNumber)
        }
    }

    private fun playSurahFromInfo(surahNumber: Int) {
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "play_surah_from_info") {
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
            strings.get(
                R.string.quran_home_surah_fallback,
                surahNumber
            )
        }
        // Respect continuousReading setting from QuranReaderScreen
        audioManager.setContinuousPlayback(_readerState.value.continuousReading)
        audioManager.playFromAyah(ayahGlobalId, audioItems, title)
    }

    private fun loadSurahs() {
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "load_surahs") {
            val thematic = quranUseCases.hasThematicContent()
            _homeState.update { it.copy(hasThematicContent = thematic) }
        }
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "load_surahs") {
            // Collected directly. Wrapping it in `stateIn(…, emptyList())` first published the
            // seed — surahs = [], isLoading = false — before Room had produced a row, so the
            // list rendered its "nothing here" state for a frame on every cold open.
            quranUseCases.getSurahList()
                .collect { surahs ->
                    _homeState.update { state ->
                        state.copy(surahs = surahs, isLoading = false)
                    }
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
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "observe_mushaf_pagination") {
            quranSettings.quranMushafScript
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
    private suspend fun translatorId(): String = quranSettings.quranTranslatorId.first()

    /**
     * The Mushaf edition content is fetched with, read from where it is persisted — the same
     * pre-hydration hazard as [translatorId], and worse in page mode, because an edition
     * decides which ayahs page N even holds.
     */
    private suspend fun mushafScript(): MushafScript =
        MushafScript.fromName(quranSettings.quranMushafScript.first())

    private fun loadVerseOfTheDay() {
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "load_verse_of_the_day") {
            val translatorId = translatorId()
            val epochDay = todayProvider.today().toEpochDay()
            val verse = quranUseCases.getVerseOfTheDay(epochDay, translatorId)
            _homeState.update { it.copy(verseOfTheDay = verse) }
        }
    }

    private fun loadReadingProgress() {
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "load_reading_progress") {
            quranUseCases.getReadingProgress()
                .collect { progress ->
                    _homeState.update { it.copy(readingProgress = progress) }
                }
        }
    }

    private fun loadBookmarks() {
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "load_bookmarks") {
            quranUseCases.getBookmarks()
                .collect { bookmarks ->
                    _bookmarksState.update { it.copy(bookmarks = bookmarks, isLoading = false) }
                    // The annotated subset, for the reader's note editor. Derived from the
                    // stream the bookmarks screen already collects rather than a second query.
                    val notes = bookmarks
                        .mapNotNull { bm ->
                            bm.note?.takeIf { it.isNotBlank() }?.let { bm.ayahId to it }
                        }
                        .toMap()
                    _readerState.update { it.copy(ayahNotes = notes) }
                }
        }
    }

    private fun loadSurah(surahNumber: Int) {
        readerTarget = ReaderTarget.Surah(surahNumber)
        _readerState.update {
            it.copy(
                isLoading = true,
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
        contentJob = launchSafely(
            telemetry,
            AppAnalytics.Feature.QURAN,
            "load_surah",
            onFailure = { _readerState.update { it.copy(isLoading = false) } },
        ) {
            quranUseCases.getSurahWithAyahs(surahNumber, translatorId())
                .collect { surahWithAyahs ->
                    _readerState.update {
                        it.copy(
                            surahWithAyahs = surahWithAyahs,
                            ayahs = surahWithAyahs?.ayahs ?: emptyList(),
                            title = surahWithAyahs?.surah?.nameEnglish ?: "",
                            subtitle = surahWithAyahs?.let { s ->
                                strings.quantity(
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
                readingMode = ReadingMode.JUZ,
                surahWithAyahs = null,
                ayahs = emptyList(),
                title = strings.get(R.string.quran_juz_number_format, juzNumber),
                subtitle = ""
            )
        }
        contentJob?.cancel()
        cancelPageJobs()
        contentJob = launchSafely(
            telemetry,
            AppAnalytics.Feature.QURAN,
            "load_juz",
            onFailure = { _readerState.update { it.copy(isLoading = false) } },
        ) {
            quranUseCases.getAyahsByJuz(juzNumber, translatorId())
                .collect { ayahs ->
                    _readerState.update {
                        it.copy(
                            ayahs = ayahs,
                            title = strings.get(R.string.quran_juz_number_format, juzNumber),
                            subtitle = strings.get(R.string.quran_ayah_count, ayahs.size),
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
                        title = strings.get(R.string.page_single_format, pageNumber),
                        subtitle = strings.get(R.string.quran_ayah_count, cached.size),
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
                    readingMode = ReadingMode.PAGE,
                    surahWithAyahs = null,
                    // Keep existing ayahs to prevent flicker during page transition
                    title = strings.get(R.string.page_single_format, pageNumber),
                    subtitle = ""
                )
            }
        }

        // One collector per page: a page already being fetched (typically requested a frame
        // earlier as a neighbour's prefetch) needs no second subscription, and re-launching
        // would throw away the emissions of the first.
        if (pageJobs[pageNumber]?.isActive == true) return

        pageJobs[pageNumber] = launchSafely(telemetry, AppAnalytics.Feature.QURAN, "load_page") {
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
                            subtitle = if (isActivePage) strings.get(R.string.quran_ayah_count, ayahs.size) else it.subtitle,
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
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "load_mushaf_page_layout") {
            val layout = quranUseCases.getMushafPageLayout(pageNumber, mushafScript())
            _readerState.update {
                it.copy(
                    mushafPageLayoutCache = it.mushafPageLayoutCache + (pageNumber to layout)
                )
            }
        }
    }

    /**
     * Save the reader's note on a verse, bookmarking it if it was not already.
     *
     * `bookmarks` keys on `(kind, target_id)` and carries the note as a column, so there is no
     * such thing as a note without a mark — and a reader writing one is telling us the verse
     * matters, which is the same statement a bookmark makes.
     */
    private fun setAyahNote(event: QuranEvent.SetAyahNote) {
        launchBestEffort(telemetry, AppAnalytics.Feature.QURAN, "set_ayah_note") {
            val existing = quranUseCases.getBookmarks().first()
                .firstOrNull { it.ayahId == event.ayahId }
            val note = event.note?.takeIf { it.isNotBlank() }
            if (existing == null) {
                quranUseCases.insertBookmark(
                    QuranBookmark(
                        id = 0,
                        ayahId = event.ayahId,
                        surahNumber = event.surahNumber,
                        ayahNumber = event.ayahNumber,
                        note = note,
                        color = null,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            } else {
                quranUseCases.updateBookmark(existing.copy(note = note))
            }
        }
    }

    private fun loadFavoriteAyahIds() {
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "load_favorite_ayah_ids") {
            quranUseCases.getFavoriteAyahIds()
                .collect { ids ->
                    _readerState.update { it.copy(favoriteAyahIds = ids.toSet()) }
                }
        }
    }

    private fun toggleFavorite(ayahId: Int, surahNumber: Int, ayahNumber: Int) {
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "toggle_favorite") {
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
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "toggle_bookmark") {
            quranUseCases.toggleBookmark(ayahId, surahNumber, ayahNumber)
        }
    }

    private fun updateReadingPosition(surah: Int, ayah: Int, page: Int, juz: Int) {
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "update_reading_position") {
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
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "observe_active_khatam") {
            activeKhatamStream.collect { snapshot ->
                _readerState.update {
                    it.copy(
                        activeKhatamId = snapshot?.khatam?.id,
                        khatamReadAyahIds = snapshot?.readAyahIds ?: emptySet(),
                    )
                }
                val khatam = snapshot?.khatam
                // `activeKhatamStream` re-emits on any row change in the khatam or its read
                // ayahs, and this branch is reached on every one of them. Without a guard,
                // completing a khatam that does not immediately leave "active" — or any
                // unrelated re-emission afterwards — called `completeKhatam` again, and again.
                // The id set is per-ViewModel and only grows, which is all that is needed:
                // finishing is a one-way transition and a fresh instance re-reads the flag.
                if (khatam != null &&
                    khatam.status != KhatamStatus.COMPLETED &&
                    khatam.id !in completedKhatamIds &&
                    snapshot.readAyahIds.size >= Khatam.TOTAL_QURAN_AYAHS
                ) {
                    completedKhatamIds += khatam.id
                    khatamUseCases.completeKhatam(khatam.id)
                }
            }
        }
    }

    private fun observeActiveKhatamForHome() {
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "observe_active_khatam_for_home") {
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
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "observe_active_khatam_for_home") {
            khatamUseCases.observeCompletedKhatams().collect { completed ->
                _homeState.update { it.copy(completedKhatamCount = completed.size) }
            }
        }
    }

    private fun toggleKhatamAyah(ayahId: Int) {
        val khatamId = _readerState.value.activeKhatamId ?: return
        val isRead = ayahId in _readerState.value.khatamReadAyahIds
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "toggle_khatam_ayah") {
            if (isRead) {
                khatamUseCases.unmarkAyahRead(khatamId, ayahId)
            } else {
                khatamUseCases.markAyahsRead(khatamId, listOf(ayahId))
            }
        }
    }

    private fun markSurahAsReadForKhatam(surahNumber: Int) {
        val khatamId = _readerState.value.activeKhatamId ?: return
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "mark_surah_as_read_for_khatam") {
            khatamUseCases.markSurahAsRead(khatamId, surahNumber)
        }
    }

    private fun togglePageKhatam(ayahIds: List<Int>) {
        val khatamId = _readerState.value.activeKhatamId ?: return
        val readIds = _readerState.value.khatamReadAyahIds
        val unreadIds = ayahIds.filter { it !in readIds }
        if (unreadIds.isEmpty()) return
        launchSafely(telemetry, AppAnalytics.Feature.QURAN, "toggle_page_khatam") {
            khatamUseCases.markAyahsRead(khatamId, unreadIds)
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
