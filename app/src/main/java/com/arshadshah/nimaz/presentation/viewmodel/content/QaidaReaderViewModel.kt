@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.arshadshah.nimaz.presentation.viewmodel.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.data.audio.QaidaAudioManager
import com.arshadshah.nimaz.data.audio.QaidaAudioState
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
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The state holder behind the Qaida Reader UI (epic #171, sub-issue F of #177).
 *
 * It wires together the domain "learning loop" ([QaidaUseCases]) and the
 * tap-to-hear playback engine ([QaidaAudioManager]) and exposes everything a
 * (later) Compose screen needs as reactive [StateFlow]s. There is no UI here —
 * just playback orchestration and reactive state.
 *
 * Tapping a cell both plays its clip and advances progress (`MarkCellHeard`,
 * sub-issue E). Playback is stopped on lesson change and on [onCleared].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QaidaReaderViewModel @Inject constructor(
    private val qaidaUseCases: QaidaUseCases,
    private val audioManager: QaidaAudioManager,
    private val telemetry: Telemetry,
) : ViewModel() {

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
        observeLoadedCells()
        observeHeardCells()
    }

    private fun observeLoadedCells() {
        viewModelScope.launch {
            lessonContent.collect { content ->
                loadedCells = content?.lines?.flatMap { it.cells }.orEmpty()
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
        _selectedLessonId.value = lessonId
    }

    /**
     * Handle a token tap: play its clip and mark it heard (which advances the
     * lesson's progress and may unlock the next lesson, per sub-issue E).
     */
    private fun onCellTapped(cell: QaidaCell) {
        audioManager.play(cell.audioKey)
        viewModelScope.launch {
            qaidaUseCases.markCellHeard(cell.lessonId, cell.id)
        }
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
        viewModelScope.launch {
            audioManager.completions.collect { key ->
                val cell = loadedCells.firstOrNull { it.audioKey == key } ?: return@collect
                qaidaUseCases.markCellHeard(cell.lessonId, cell.id)
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
        viewModelScope.launch {
            qaidaUseCases.resetProgress()
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
