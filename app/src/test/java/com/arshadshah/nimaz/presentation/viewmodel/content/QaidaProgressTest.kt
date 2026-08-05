package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.data.audio.QaidaAudioManager
import com.arshadshah.nimaz.data.audio.QaidaAudioState
import com.arshadshah.nimaz.domain.model.QaidaCell
import com.arshadshah.nimaz.domain.model.QaidaLessonContent
import com.arshadshah.nimaz.domain.model.QaidaLine
import com.arshadshah.nimaz.domain.model.QaidaLineContent
import com.arshadshah.nimaz.domain.model.LineType
import com.arshadshah.nimaz.domain.model.TokenType
import com.arshadshah.nimaz.domain.usecase.QaidaUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Qaida progress is a gate, not a counter — completing a line awards stars and unlocks the next
 * lesson. So "heard" has to mean heard.
 *
 * `playLine` wrote every one of the line's marks immediately, decoupled from playback:
 * `playSequence` is fire-and-forget and nothing observed it. Tapping "play line" and
 * immediately leaving credited the whole line.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QaidaProgressTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var useCases: QaidaUseCases
    private lateinit var audioManager: QaidaAudioManager

    /** Cells credited as heard, in order. */
    private val marked = mutableListOf<Int>()

    private val completions = MutableSharedFlow<String>(extraBufferCapacity = 32)

    private val cells = listOf(cell(1, "a"), cell(2, "b"), cell(3, "c"))

    private val content = QaidaLessonContent(
        lesson = mockk(relaxed = true),
        lines = listOf(
            QaidaLineContent(
                line = QaidaLine(
                    id = 7,
                    lessonId = 1,
                    lineNumber = 1,
                    lineType = LineType.entries.first(),
                    instructionEnglish = null,
                    instructionArabic = null,
                    displayOrder = 0,
                ),
                cells = cells,
            ),
        ),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
        audioManager = mockk(relaxed = true)

        every { audioManager.state } returns MutableStateFlow(QaidaAudioState())
        every { audioManager.completions } returns completions
        every { useCases.getLessonContent(any()) } returns flowOf(content)
        every { useCases.getCourseProgress() } returns flowOf(mockk(relaxed = true))
        every { useCases.getLetters() } returns flowOf(emptyList())
        every { useCases.getLessonProgress(any()) } returns flowOf(mockk(relaxed = true))
        every { useCases.observeCompletedCells(any()) } returns flowOf(emptySet())
        // Three matchers, not two: `MarkCellHeardUseCase.invoke` has a defaulted `now`, so the
        // call site compiles to the synthetic three-arg bridge and a two-arg stub never matches.
        coEvery { useCases.markCellHeard(any(), any(), any()) } answers {
            marked += secondArg<Int>()
        }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun reader(): QaidaReaderViewModel {
        val vm = QaidaReaderViewModel(useCases, audioManager, RecordingTelemetry())
        vm.onEvent(QaidaReaderEvent.SelectLesson(1))
        dispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    @Test
    fun `playing a line credits nothing until a clip actually finishes`() = runTest {
        val vm = reader()

        vm.onEvent(QaidaReaderEvent.PlayLine(7))
        advanceUntilIdle()

        // The shipped behaviour: all three marks written here, before a single clip had played.
        assertThat(marked).isEmpty()
    }

    @Test
    fun `each cell is credited as its own clip ends`() = runTest {
        val vm = reader()
        vm.onEvent(QaidaReaderEvent.PlayLine(7))
        advanceUntilIdle()

        completions.emit("a")
        advanceUntilIdle()
        assertThat(marked).containsExactly(1)

        completions.emit("b")
        advanceUntilIdle()
        assertThat(marked).containsExactly(1, 2).inOrder()
    }

    @Test
    fun `leaving part way through credits only what was heard`() = runTest {
        val vm = reader()
        vm.onEvent(QaidaReaderEvent.PlayLine(7))
        advanceUntilIdle()

        completions.emit("a")
        advanceUntilIdle()

        // The learner opens another lesson. `stop()` emits no completion, by design — that is
        // the distinction `state.currentKey` could not make, since it goes null either way.
        vm.onEvent(QaidaReaderEvent.SelectLesson(2))
        advanceUntilIdle()

        // Was: all three, which under QaidaProgressRules could complete the line, award its
        // stars and unlock the next lesson for content heard for half a second.
        assertThat(marked).containsExactly(1)
    }

    @Test
    fun `a key from no loaded cell credits nothing`() = runTest {
        val vm = reader()

        completions.emit("not-in-this-lesson")
        advanceUntilIdle()

        assertThat(marked).isEmpty()
    }

    private fun cell(id: Int, key: String) = QaidaCell(
        id = id,
        lineId = 7,
        lessonId = 1,
        position = id,
        textArabic = key,
        transliteration = key,
        tokenType = TokenType.entries.first(),
        audioKey = key,
        audioPath = "qaida/audio/$key.mp3",
        highlightGroup = null,
        letterId = null,
        notes = null,
    )
}
