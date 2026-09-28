@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.arshadshah.nimaz.presentation.viewmodel.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.data.audio.QaidaAudioManager
import com.arshadshah.nimaz.data.audio.QaidaAudioState
import com.arshadshah.nimaz.data.audio.QaidaLessonAudioStore
import com.arshadshah.nimaz.data.qaida.QaidaJourneyStore
import com.arshadshah.nimaz.data.qaida.QaidaLearningSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.domain.model.QaidaCell
import com.arshadshah.nimaz.domain.model.QaidaCourseProgress
import com.arshadshah.nimaz.domain.model.QaidaLessonContent
import com.arshadshah.nimaz.domain.model.QaidaLessonState
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.domain.usecase.QaidaUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The state holder behind the Qaida Reader UI (epic #171, sub-issue F of #177).
 *
 * It wires together the domain "learning loop" ([QaidaUseCases]) and the
 * tap-to-hear playback engine ([QaidaAudioManager]) and exposes everything a
 * (later) Compose screen needs as reactive [StateFlow]s. There is no UI here —
 * just playback orchestration and reactive state.
 *
 * Completed playback credits listening; explicit self-checks credit practice.
 * Playback is stopped on lesson change and on [onCleared].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QaidaReaderViewModel @Inject constructor(
    private val qaidaUseCases: QaidaUseCases,
    private val audioManager: QaidaAudioManager,
    private val telemetry: Telemetry,
    private val lessonAudio: QaidaLessonAudioStore? = null,
    private val journey: QaidaJourneyStore? = null,
) : ViewModel() {

    private val fallbackSettings = MutableStateFlow(QaidaLearningSettings())
    val settings: StateFlow<QaidaLearningSettings> = journey?.settings ?: fallbackSettings
    private fun updateSettings(value: QaidaLearningSettings) {
        if (journey != null) journey.updateSettings(value) else fallbackSettings.value = value
        audioManager.setSlow(value.slowPlayback)
    }
    private val _sessionHeard = MutableStateFlow<Set<Int>>(emptySet())
    val sessionHeard: StateFlow<Set<Int>> = _sessionHeard.asStateFlow()
    private val _download = MutableStateFlow(QaidaDownloadState())
    val download: StateFlow<QaidaDownloadState> = _download.asStateFlow()
    private var downloadJob: Job? = null
    private val _cacheBytes = MutableStateFlow(0L)
    val cacheBytes: StateFlow<Long> = _cacheBytes.asStateFlow()
    val dueLessons: StateFlow<Set<Int>> = (journey?.let { store -> store.revision.map { store.dueLessonIds() } }
        ?: flowOf(emptySet())).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val todayCount: StateFlow<Int> = (journey?.let { store -> store.revision.map { store.todayCount() } }
        ?: flowOf(0)).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    fun refreshReview() { journey?.refresh() }
    fun dueCells(lessonId: Int): Set<Int> = journey?.dueCellIds(lessonId).orEmpty()
    fun resumeCell(lessonId: Int): Int? = journey?.resumeCell(lessonId)
    fun savePosition(cell: QaidaCell) { journey?.setResume(cell.lessonId,cell.id) }
    fun refreshCacheSize() { viewModelScope.launch { _cacheBytes.value = lessonAudio?.sizeBytes() ?: 0L } }

    private val sharing = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS)

    private val _selectedLessonId = MutableStateFlow<Int?>(null)

    /** The lesson currently open in the reader, or null before one is chosen. */
    val selectedLessonId: StateFlow<Int?> = _selectedLessonId.asStateFlow()

    /**
     * Whole-course rollup: every lesson with its gated status and stars, overall
     * completion and the "continue where you left off" pointer.
     */
    val courseProgress: StateFlow<QaidaCourseProgress?> =
        qaidaUseCases.getCourseProgress()
            .stateIn(viewModelScope, sharing, null)

    /** The 29-letter reference table shown alongside the lessons. */
    val letters: StateFlow<List<QaidaLetter>> =
        qaidaUseCases.getLetters()
            .stateIn(viewModelScope, sharing, emptyList())

    /** The selected lesson's full content: ordered lines, each with its cells. */
    val lessonContent: StateFlow<QaidaLessonContent?> =
        _selectedLessonId.flatMapLatest { id ->
            if (id == null) flowOf(null) else qaidaUseCases.getLessonContent(id)
        }.stateIn(viewModelScope, sharing, null)

    /** Derived progress/stars for the selected lesson. */
    val lessonProgress: StateFlow<QaidaLessonState?> =
        _selectedLessonId.flatMapLatest { id ->
            if (id == null) flowOf(null) else qaidaUseCases.getLessonProgress(id)
        }.stateIn(viewModelScope, sharing, null)

    /**
     * Cell ids already heard in the open lesson, so the reader can mark done
     * tiles and a returning learner can see where they left off.
     */
    val completedCellIds: StateFlow<Set<Int>> =
        _selectedLessonId.flatMapLatest { id ->
            if (id == null) flowOf(emptySet()) else qaidaUseCases.observeCompletedCells(id)
        }.stateIn(viewModelScope, sharing, emptySet())

    /** Raw audio engine state (loading/playing flags + current key). */
    val audioState: StateFlow<QaidaAudioState> = audioManager.state

    /**
     * The [QaidaCell] currently sounding, resolved from the audio engine's
     * current key against the loaded lesson content, so the UI can highlight it.
     */
    val playingCell: StateFlow<QaidaCell?> =
        combine(audioManager.state, lessonContent) { audio, content ->
            val key = audio.currentKey ?: return@combine null
            content?.lines
                ?.firstNotNullOfOrNull { line -> line.cells.firstOrNull { it.audioKey == key } }
        }.stateIn(viewModelScope, sharing, null)

    /**
     * The open lesson's cells, flattened, kept current by [observeLoadedCells].
     *
     * A snapshot rather than a read of [lessonContent] at the moment a clip ends: that flow is
     * `WhileSubscribed`, so it is cold whenever no screen is collecting it — and audio outlives
     * the screen. Crediting progress must not depend on something being on screen.
     */
    private var loadedCells: List<QaidaCell> = emptyList()

    init {
        audioManager.setSlow(settings.value.slowPlayback)
        observeLoadedCells()
        observeHeardCells()
    }

    private fun observeLoadedCells() {
        launchSafely(telemetry, AppAnalytics.Feature.QAIDA, "observe_loaded_cells") {
            lessonContent.collect { content ->
                loadedCells = content?.lines?.flatMap { it.cells }.orEmpty()
                if (content != null) prepareAudio(content)
            }
        }
    }

    fun onEvent(event: QaidaReaderEvent) {
        when (event) {
            is QaidaReaderEvent.SelectLesson -> {
                telemetry.featureUsed(AppAnalytics.Feature.QAIDA, "select_lesson")
                selectLesson(event.lessonId)
            }

            is QaidaReaderEvent.CellTapped -> {
                telemetry.featureUsed(AppAnalytics.Feature.QAIDA, "play_cell")
                onCellTapped(event.cell)
            }

            is QaidaReaderEvent.PlayLine -> {
                telemetry.featureUsed(AppAnalytics.Feature.QAIDA, "play_line")
                playLine(event.lineId)
            }

            is QaidaReaderEvent.PlayLetter -> {
                telemetry.featureUsed(AppAnalytics.Feature.QAIDA, "play_letter")
                playLetter(event.letter)
            }
            // Lesson advancement is the core progression signal of the whole feature and was
            // logged nowhere, while a single cell tap was. Both directions are recorded: a
            // learner going backwards is repeating a lesson, which is the shape that says the
            // gating is too tight.
            QaidaReaderEvent.NextLesson -> {
                telemetry.featureUsed(AppAnalytics.Feature.QAIDA, "next_lesson")
                nextLesson()
            }

            QaidaReaderEvent.PreviousLesson -> {
                telemetry.featureUsed(AppAnalytics.Feature.QAIDA, "previous_lesson")
                previousLesson()
            }

            QaidaReaderEvent.Resume -> {
                telemetry.featureUsed(AppAnalytics.Feature.QAIDA, "resume")
                resume()
            }

            is QaidaReaderEvent.PractisedCell -> {
                if (event.cell.lessonId != _selectedLessonId.value) return
                launchSafely(telemetry, AppAnalytics.Feature.QAIDA, "practice_cell") {
                    journey?.record(event.cell.lessonId, event.cell.id, event.confident)
                    qaidaUseCases.markCellHeard.markPractised(event.cell.lessonId, event.cell.id)
                }
            }
            is QaidaReaderEvent.RepeatCell -> audioManager.playSequence(List(event.times.coerceIn(1,3)) { event.cell.audioKey })
            is QaidaReaderEvent.SetSlow -> updateSettings(settings.value.copy(slowPlayback = event.enabled))
            is QaidaReaderEvent.SetTransliteration -> updateSettings(settings.value.copy(showTransliteration = event.enabled))
            QaidaReaderEvent.RetryAudio -> lessonContent.value?.let { prepareAudio(it, refresh = true) }
            QaidaReaderEvent.StopAudio -> audioManager.stop()
            QaidaReaderEvent.ClearAudio -> {
                downloadJob?.cancel()
                audioManager.stop()
                viewModelScope.launch {
                    lessonAudio?.clear()
                    _download.value = QaidaDownloadState()
                    refreshCacheSize()
                }
            }
            QaidaReaderEvent.ResetJourney -> {
                telemetry.featureUsed(AppAnalytics.Feature.QAIDA, "reset_journey")
                resetJourney()
            }
        }
    }

    /** Open a lesson, stopping any audio still playing from the previous one. */
    private fun selectLesson(lessonId: Int) {
        if (_selectedLessonId.value == lessonId) return
        audioManager.stop()
        downloadJob?.cancel()
        _download.value = QaidaDownloadState()
        _sessionHeard.value = emptySet()
        _selectedLessonId.value = lessonId
    }

    /**
     * Handle a token tap: play its clip and mark it heard (which advances the
     * lesson's progress and may unlock the next lesson, per sub-issue E).
     */
    private fun onCellTapped(cell: QaidaCell) {
        audioManager.play(cell.audioKey)
        savePosition(cell)
        // Only actual playback completion credits listening. A tap can fail or be cancelled.
    }

    /**
     * Play a whole line back-to-back. Each cell is credited as **its own clip finishes**, by
     * [observeHeardCells] — not here.
     *
     * This used to write all of the line's marks immediately and unconditionally, decoupled
     * from playback: `playSequence` is fire-and-forget and nothing observed it. So tapping
     * "play line" on an eight-cell line and immediately opening another lesson (which calls
     * `audioManager.stop()`) recorded **all eight cells as heard** — enough, under
     * `QaidaProgressRules`, to complete the line, award its stars and unlock the next lesson,
     * for content the learner heard half a second of. The gating the whole Qaida progression
     * rests on was bypassable with one tap.
     */
    private fun playLine(lineId: Int) {
        val line = lessonContent.value?.lines?.firstOrNull { it.line.id == lineId } ?: return
        if (line.cells.isEmpty()) return
        audioManager.playSequence(line.cells.map { it.audioKey })
    }

    /**
     * Credits a cell once its clip has actually played to the end.
     *
     * Driven by [QaidaAudioManager.completions], which emits only on a natural end — never on
     * [QaidaAudioManager.stop], a lesson change, or a tap that replaces what is playing.
     *
     * Keyed against the loaded lesson so a clip cannot credit a cell from a lesson the learner
     * has left; the same resolution [playingCell] does for highlighting.
     */
    private fun observeHeardCells() {
        launchSafely(telemetry, AppAnalytics.Feature.QAIDA, "observe_heard_cells") {
            audioManager.completions.collect { key ->
                val cell = loadedCells.firstOrNull { it.audioKey == key } ?: return@collect
                qaidaUseCases.markCellHeard(cell.lessonId, cell.id)
                _sessionHeard.value += cell.id
                journey?.recordActivity(cell.id)
            }
        }
    }

    /** Play a single letter's clip from the letter explorer (no progress change). */
    private fun playLetter(letter: QaidaLetter) {
        audioManager.play(letter.audioKey)
    }

    /** Move to the next lesson in display order, if it exists and is not locked. */
    private fun nextLesson() {
        val lessons = courseProgress.value?.lessons ?: return
        val index = lessons.indexOfFirst { it.lesson.id == _selectedLessonId.value }
        if (index < 0 || index >= lessons.lastIndex) return
        val next = lessons[index + 1]
        if (next.status != LessonStatus.LOCKED) selectLesson(next.lesson.id)
    }

    /** Move to the previous lesson in display order, if there is one. */
    private fun previousLesson() {
        val lessons = courseProgress.value?.lessons ?: return
        val index = lessons.indexOfFirst { it.lesson.id == _selectedLessonId.value }
        if (index <= 0) return
        selectLesson(lessons[index - 1].lesson.id)
    }

    /**
     * Open the "continue where you left off" lesson: the first unlocked-but-
     * incomplete lesson, falling back to the first lesson if everything is done.
     */
    private fun resume() {
        val course = courseProgress.value ?: return
        val target = course.nextLessonId ?: course.lessons.firstOrNull()?.lesson?.id
        if (target != null) selectLesson(target)
    }

    /**
     * Wipe all Qaida progress and start the journey over from Lesson 1. The
     * course rollup is reactive, so the home screen refreshes itself once the
     * rows are cleared. Stops any audio first.
     */
    private fun resetJourney() {
        audioManager.stop()
        _selectedLessonId.value = null
        launchSafely(telemetry, AppAnalytics.Feature.QAIDA, "reset_journey") {
            qaidaUseCases.resetProgress()
            journey?.reset()
        }
    }

    private fun prepareAudio(content: QaidaLessonContent, refresh: Boolean = false) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _download.value = QaidaDownloadState(loading = true)
            try {
                val ready = lessonAudio?.prepare(content, refresh) { done, total ->
                    _download.value = QaidaDownloadState(loading = true, completed = done, total = total)
                } ?: false
                _download.value = QaidaDownloadState(ready = ready)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _download.value = QaidaDownloadState(failed = true) }
            refreshCacheSize()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.stop()
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** User-facing status contains no server details or raw exceptions. */
data class QaidaDownloadState(
    val loading: Boolean = false,
    val ready: Boolean = false,
    val failed: Boolean = false,
    val completed: Int = 0,
    val total: Int = 0,
)
