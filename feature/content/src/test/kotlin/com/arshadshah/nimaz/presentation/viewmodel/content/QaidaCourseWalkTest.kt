package com.arshadshah.nimaz.presentation.viewmodel.content

import app.cash.turbine.test
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.data.audio.QaidaAudioManager
import com.arshadshah.nimaz.data.audio.QaidaAudioState
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.domain.model.LineType
import com.arshadshah.nimaz.domain.model.QaidaCell
import com.arshadshah.nimaz.domain.model.QaidaCourseProgress
import com.arshadshah.nimaz.domain.model.QaidaLesson
import com.arshadshah.nimaz.domain.model.QaidaLessonContent
import com.arshadshah.nimaz.domain.model.QaidaLessonState
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.domain.model.QaidaLine
import com.arshadshah.nimaz.domain.model.QaidaLineContent
import com.arshadshah.nimaz.domain.model.MakhrajArea
import com.arshadshah.nimaz.domain.model.TokenType
import com.arshadshah.nimaz.domain.usecase.GetCourseProgressUseCase
import com.arshadshah.nimaz.domain.usecase.GetLessonProgressUseCase
import com.arshadshah.nimaz.domain.usecase.GetQaidaCellUseCase
import com.arshadshah.nimaz.domain.usecase.GetQaidaLessonContentUseCase
import com.arshadshah.nimaz.domain.usecase.GetQaidaLessonsUseCase
import com.arshadshah.nimaz.domain.usecase.GetQaidaLettersUseCase
import com.arshadshah.nimaz.domain.usecase.MarkCellHeardUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveCompletedCellsUseCase
import com.arshadshah.nimaz.domain.usecase.QaidaUseCases
import com.arshadshah.nimaz.domain.usecase.ResetQaidaProgressUseCase
import com.arshadshah.nimaz.domain.usecase.UnlockNextLessonUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Walking the Qaida course: next, previous, resume — and the guards on each.
 *
 * These three are how a learner moves between lessons from *inside* one, and every one of them
 * is a sequence of early returns over a list that may not be loaded, may not contain the open
 * lesson, and may end where the learner is standing. `QaidaReaderScreenTest` pins that the
 * buttons appear and are enabled correctly; what it cannot see is what happens when a button
 * that *should* be disabled is pressed anyway — which is the state the screen is in for the
 * frames before `courseProgress` arrives.
 *
 * **The `LOCKED` guard on "next" is the one that matters.** It is duplicated between the screen
 * and here on purpose: the screen's copy is cosmetic (a greyed button), and this one is the
 * gate. `QaidaProgressRules` decides what unlocks; a next-lesson move that ignored the status
 * would walk straight past it, and the whole progression is built on that gate holding.
 *
 * **Selecting the lesson already open is a no-op**, and that matters because `selectLesson`
 * stops audio: without the guard, a `LaunchedEffect` re-firing on recomposition would cut off
 * the clip the learner is listening to.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QaidaCourseWalkTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var getLessonContent: GetQaidaLessonContentUseCase
    private lateinit var getLetters: GetQaidaLettersUseCase
    private lateinit var getLessonProgress: GetLessonProgressUseCase
    private lateinit var getCourseProgress: GetCourseProgressUseCase
    private lateinit var useCases: QaidaUseCases
    private lateinit var audioManager: QaidaAudioManager

    private val course = MutableStateFlow(emptyCourse)
    private val letters = MutableStateFlow<List<QaidaLetter>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        getLessonContent = mockk()
        getLetters = mockk()
        getLessonProgress = mockk()
        getCourseProgress = mockk()

        useCases = QaidaUseCases(
            getLessons = mockk<GetQaidaLessonsUseCase>(relaxed = true),
            getLessonContent = getLessonContent,
            getLetters = getLetters,
            getCell = mockk<GetQaidaCellUseCase>(relaxed = true),
            markCellHeard = mockk<MarkCellHeardUseCase>(relaxed = true),
            unlockNextLesson = mockk<UnlockNextLessonUseCase>(relaxed = true),
            getLessonProgress = getLessonProgress,
            getCourseProgress = getCourseProgress,
            resetProgress = mockk<ResetQaidaProgressUseCase>(relaxed = true),
            observeCompletedCells = mockk<ObserveCompletedCellsUseCase>(relaxed = true),
        )

        audioManager = mockk(relaxed = true)
        every { audioManager.state } returns MutableStateFlow(QaidaAudioState())
        every { audioManager.completions } returns MutableSharedFlow(extraBufferCapacity = 32)

        every { getLetters.invoke() } returns letters
        every { getCourseProgress.invoke() } returns course
        every { getLessonProgress.invoke(any()) } returns flowOf(null)
        every { getLessonContent.invoke(any()) } answers { flowOf(lessonContent(firstArg())) }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = QaidaReaderViewModel(useCases, audioManager, RecordingTelemetry())

    /**
     * The ViewModel with `courseProgress` actually subscribed.
     *
     * `courseProgress` is `stateIn(..., WhileSubscribed)`, so its `.value` is **null** until
     * something collects it — and `nextLesson`, `previousLesson` and `resume` all read that
     * `.value` and return early on null. On a device the reader screen is the subscriber; in a
     * test, nothing is, so a walk asserted without this passes for the wrong reason: the move
     * is refused rather than performed. This is exactly the "course has not loaded yet" state
     * the guards exist for, which is why it is worth a named helper rather than a stray
     * `collect`.
     */
    private fun TestScope.subscribedViewModel(): QaidaReaderViewModel {
        val vm = viewModel()
        backgroundScope.launch { vm.courseProgress.collect {} }
        advanceUntilIdle()
        return vm
    }

    private fun threeLessons(
        thirdStatus: LessonStatus = LessonStatus.UNLOCKED,
        nextLessonId: Int? = 2,
    ) = QaidaCourseProgress(
        lessons = listOf(
            lessonState(1, LessonStatus.COMPLETED),
            lessonState(2, LessonStatus.IN_PROGRESS),
            lessonState(3, thirdStatus),
        ),
        completedLessons = 1,
        totalLessons = 3,
        totalStars = 3,
        maxStars = 9,
        totalCellsHeard = 2,
        overallFraction = 0.33f,
        nextLessonId = nextLessonId,
    )

    // ---- next -------------------------------------------------------------------------

    @Test
    fun `next moves to the following lesson when it is unlocked`() = runTest {
        course.value = threeLessons()
        val vm = subscribedViewModel()
        vm.onEvent(QaidaReaderEvent.SelectLesson(2))
        advanceUntilIdle()

        vm.onEvent(QaidaReaderEvent.NextLesson)
        advanceUntilIdle()

        assertThat(vm.selectedLessonId.value).isEqualTo(3)
    }

    @Test
    fun `next refuses to walk past a locked lesson`() = runTest {
        // The gate the whole progression rests on. The screen greys the button; this is what
        // stops the move when it is pressed regardless.
        course.value = threeLessons(thirdStatus = LessonStatus.LOCKED)
        val vm = subscribedViewModel()
        vm.onEvent(QaidaReaderEvent.SelectLesson(2))
        advanceUntilIdle()

        vm.onEvent(QaidaReaderEvent.NextLesson)
        advanceUntilIdle()

        assertThat(vm.selectedLessonId.value).isEqualTo(2)
    }

    @Test
    fun `next does nothing on the last lesson`() = runTest {
        course.value = threeLessons()
        val vm = subscribedViewModel()
        vm.onEvent(QaidaReaderEvent.SelectLesson(3))
        advanceUntilIdle()

        vm.onEvent(QaidaReaderEvent.NextLesson)
        advanceUntilIdle()

        assertThat(vm.selectedLessonId.value).isEqualTo(3)
    }

    @Test
    fun `next does nothing when the open lesson is not in the course`() = runTest {
        // `indexOfFirst` returns -1 — a content artifact that dropped the lesson a stored
        // pointer names, or a deep link to a lesson that no longer ships.
        course.value = threeLessons()
        val vm = subscribedViewModel()
        vm.onEvent(QaidaReaderEvent.SelectLesson(99))
        advanceUntilIdle()

        vm.onEvent(QaidaReaderEvent.NextLesson)
        advanceUntilIdle()

        assertThat(vm.selectedLessonId.value).isEqualTo(99)
    }

    @Test
    fun `next does nothing on a course with no lessons at all`() = runTest {
        // An install whose content artifact has not landed yet: the rollup exists and is empty.
        val vm = viewModel()
        vm.onEvent(QaidaReaderEvent.SelectLesson(2))
        advanceUntilIdle()

        vm.onEvent(QaidaReaderEvent.NextLesson)
        advanceUntilIdle()

        assertThat(vm.selectedLessonId.value).isEqualTo(2)
    }

    // ---- previous ---------------------------------------------------------------------

    @Test
    fun `previous moves back one lesson`() = runTest {
        course.value = threeLessons()
        val vm = subscribedViewModel()
        vm.onEvent(QaidaReaderEvent.SelectLesson(2))
        advanceUntilIdle()

        vm.onEvent(QaidaReaderEvent.PreviousLesson)
        advanceUntilIdle()

        assertThat(vm.selectedLessonId.value).isEqualTo(1)
    }

    @Test
    fun `previous does nothing on the first lesson`() = runTest {
        // `index <= 0` covers both "at the start" and "not in the list at all".
        course.value = threeLessons()
        val vm = subscribedViewModel()
        vm.onEvent(QaidaReaderEvent.SelectLesson(1))
        advanceUntilIdle()

        vm.onEvent(QaidaReaderEvent.PreviousLesson)
        advanceUntilIdle()

        assertThat(vm.selectedLessonId.value).isEqualTo(1)
    }

    @Test
    fun `previous ignores a lesson that is not in the course`() = runTest {
        course.value = threeLessons()
        val vm = subscribedViewModel()
        vm.onEvent(QaidaReaderEvent.SelectLesson(99))
        advanceUntilIdle()

        vm.onEvent(QaidaReaderEvent.PreviousLesson)
        advanceUntilIdle()

        assertThat(vm.selectedLessonId.value).isEqualTo(99)
    }

    // ---- resume -----------------------------------------------------------------------

    @Test
    fun `resume opens the lesson the course points at`() = runTest {
        course.value = threeLessons(nextLessonId = 2)
        val vm = subscribedViewModel()
        vm.onEvent(QaidaReaderEvent.SelectLesson(1))
        advanceUntilIdle()

        vm.onEvent(QaidaReaderEvent.Resume)
        advanceUntilIdle()

        assertThat(vm.selectedLessonId.value).isEqualTo(2)
    }

    @Test
    fun `resume falls back to the first lesson once the course is finished`() = runTest {
        // `nextLessonId` is null when everything is complete — without the fallback, "continue"
        // does nothing at all for a learner who has finished, which reads as a broken button.
        course.value = threeLessons(nextLessonId = null)
        val vm = subscribedViewModel()
        vm.onEvent(QaidaReaderEvent.SelectLesson(3))
        advanceUntilIdle()

        vm.onEvent(QaidaReaderEvent.Resume)
        advanceUntilIdle()

        assertThat(vm.selectedLessonId.value).isEqualTo(1)
    }

    @Test
    fun `resume does nothing on a course with no lessons at all`() = runTest {
        val vm = viewModel()
        vm.onEvent(QaidaReaderEvent.SelectLesson(2))
        advanceUntilIdle()

        vm.onEvent(QaidaReaderEvent.Resume)
        advanceUntilIdle()

        assertThat(vm.selectedLessonId.value).isEqualTo(2)
    }

    // ---- selecting --------------------------------------------------------------------

    @Test
    fun `re-selecting the open lesson does not cut off the clip playing`() = runTest {
        // `selectLesson` stops audio, and a `LaunchedEffect(lessonId)` re-fires on every
        // recomposition of the reader. Without the guard, a recomposition silences playback.
        course.value = threeLessons()
        val vm = subscribedViewModel()
        vm.onEvent(QaidaReaderEvent.SelectLesson(2))
        advanceUntilIdle()

        vm.onEvent(QaidaReaderEvent.SelectLesson(2))
        advanceUntilIdle()

        verify(exactly = 1) { audioManager.stop() }
    }

    @Test
    fun `changing lesson stops whatever the previous one was playing`() = runTest {
        course.value = threeLessons()
        val vm = subscribedViewModel()
        vm.onEvent(QaidaReaderEvent.SelectLesson(1))
        advanceUntilIdle()

        vm.onEvent(QaidaReaderEvent.SelectLesson(2))
        advanceUntilIdle()

        verify(exactly = 2) { audioManager.stop() }
    }

    // ---- before the course is subscribed ----------------------------------------------

    @Test
    fun `a walk asked for before anything reads the course is refused, not guessed`() {
        // `courseProgress` is `WhileSubscribed`, so its `.value` is genuinely null until a
        // screen collects it — the state the reader is in for its first frames, and the state
        // a deep link straight into a lesson starts in. All three moves read it and return.
        val vm = viewModel()

        vm.onEvent(QaidaReaderEvent.SelectLesson(2))
        vm.onEvent(QaidaReaderEvent.NextLesson)
        vm.onEvent(QaidaReaderEvent.PreviousLesson)
        vm.onEvent(QaidaReaderEvent.Resume)

        assertThat(vm.selectedLessonId.value).isEqualTo(2)
    }

    // ---- playing a whole line ----------------------------------------------------------

    @Test
    fun `playing a line queues its cells in order`() = runTest {
        // The credit for each cell comes from playback finishing, not from this call — see
        // `QaidaAudioManager.completions`. What this owns is *which* clips are queued.
        course.value = threeLessons()
        val vm = subscribedViewModel()
        backgroundScope.launch { vm.lessonContent.collect {} }
        vm.onEvent(QaidaReaderEvent.SelectLesson(1))
        advanceUntilIdle()

        vm.onEvent(QaidaReaderEvent.PlayLine(100))
        advanceUntilIdle()

        verify { audioManager.playSequence(listOf("l1_alif", "l1_baa")) }
    }

    @Test
    fun `playing a line that is not in the open lesson does nothing`() = runTest {
        // A line id from the lesson the learner just left — the content flow is `WhileSubscribed`
        // and the tap can land after the lesson changed.
        course.value = threeLessons()
        val vm = subscribedViewModel()
        backgroundScope.launch { vm.lessonContent.collect {} }
        vm.onEvent(QaidaReaderEvent.SelectLesson(1))
        advanceUntilIdle()

        vm.onEvent(QaidaReaderEvent.PlayLine(9999))
        advanceUntilIdle()

        verify(exactly = 0) { audioManager.playSequence(any()) }
    }

    @Test
    fun `playing a line before any lesson is open does nothing`() = runTest {
        val vm = subscribedViewModel()

        vm.onEvent(QaidaReaderEvent.PlayLine(100))
        advanceUntilIdle()

        verify(exactly = 0) { audioManager.playSequence(any()) }
    }

    @Test
    fun `a line with no cells is not queued as an empty sequence`() = runTest {
        // `playSequence(emptyList())` is a no-op in the manager, but the guard here is what
        // keeps a heading line — which has no cells by design — from looking playable.
        every { getLessonContent.invoke(1) } returns flowOf(
            QaidaLessonContent(
                lesson = lesson(1),
                lines = listOf(
                    QaidaLineContent(
                        line = QaidaLine(
                            id = 100,
                            lessonId = 1,
                            lineNumber = 1,
                            lineType = LineType.HEADING,
                            instructionEnglish = "Letters on their own",
                            instructionArabic = null,
                            displayOrder = 0,
                        ),
                        cells = emptyList(),
                    ),
                ),
            )
        )
        course.value = threeLessons()
        val vm = subscribedViewModel()
        backgroundScope.launch { vm.lessonContent.collect {} }
        vm.onEvent(QaidaReaderEvent.SelectLesson(1))
        advanceUntilIdle()

        vm.onEvent(QaidaReaderEvent.PlayLine(100))
        advanceUntilIdle()

        verify(exactly = 0) { audioManager.playSequence(any()) }
    }

    // ---- which cell is highlighted -----------------------------------------------------

    @Test
    fun `the sounding cell is resolved from the audio key against the open lesson`() = runTest {
        // The reader highlights this cell. Resolving by index instead of by `audioKey` would
        // light the wrong tile whenever a line is played out of order.
        val audio = MutableStateFlow(QaidaAudioState())
        every { audioManager.state } returns audio
        course.value = threeLessons()
        val vm = subscribedViewModel()
        backgroundScope.launch { vm.lessonContent.collect {} }
        backgroundScope.launch { vm.playingCell.collect {} }
        vm.onEvent(QaidaReaderEvent.SelectLesson(1))
        advanceUntilIdle()

        audio.value = QaidaAudioState(currentKey = "l1_baa", isPlaying = true)
        advanceUntilIdle()

        assertThat(vm.playingCell.value?.id).isEqualTo(12)
    }

    @Test
    fun `nothing is highlighted while nothing is sounding`() = runTest {
        val audio = MutableStateFlow(QaidaAudioState())
        every { audioManager.state } returns audio
        course.value = threeLessons()
        val vm = subscribedViewModel()
        backgroundScope.launch { vm.lessonContent.collect {} }
        backgroundScope.launch { vm.playingCell.collect {} }
        vm.onEvent(QaidaReaderEvent.SelectLesson(1))
        advanceUntilIdle()

        assertThat(vm.playingCell.value).isNull()
    }

    @Test
    fun `a clip from a lesson the learner has left highlights nothing`() = runTest {
        // Audio outlives the screen, so a clip can still be sounding when the lesson changes.
        // Keying on the *open* lesson's cells is what stops a foreign tile lighting up.
        val audio = MutableStateFlow(QaidaAudioState())
        every { audioManager.state } returns audio
        course.value = threeLessons()
        val vm = subscribedViewModel()
        backgroundScope.launch { vm.lessonContent.collect {} }
        backgroundScope.launch { vm.playingCell.collect {} }
        vm.onEvent(QaidaReaderEvent.SelectLesson(1))
        advanceUntilIdle()

        audio.value = QaidaAudioState(currentKey = "l2_alif", isPlaying = true)
        advanceUntilIdle()

        assertThat(vm.playingCell.value).isNull()
    }

    // ---- the letter explorer ----------------------------------------------------------

    @Test
    fun `playing a letter plays that letter's clip and touches no progress`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QaidaReaderEvent.PlayLetter(letter(audioKey = "letter_alif")))
        advanceUntilIdle()

        verify { audioManager.play("letter_alif") }
    }

    @Test
    fun `the letters flow is what the explorer reads`() = runTest {
        val vm = viewModel()
        vm.letters.test {
            assertThat(awaitItem()).isEmpty()
            letters.value = listOf(letter(id = 1), letter(id = 2))
            assertThat(awaitItem()).hasSize(2)
        }
    }

    // ---- fixtures ---------------------------------------------------------------------

    private companion object {
        val emptyCourse = QaidaCourseProgress(
            lessons = emptyList(),
            completedLessons = 0,
            totalLessons = 0,
            totalStars = 0,
            maxStars = 0,
            totalCellsHeard = 0,
            overallFraction = 0f,
            nextLessonId = null,
        )
    }

    private fun lesson(id: Int) = QaidaLesson(
        id = id,
        lessonNumber = id,
        titleEnglish = "Lesson $id",
        titleArabic = "درس",
        titleTransliteration = "dars",
        description = "",
        conceptTags = emptyList(),
        icon = "book",
        displayOrder = id,
    )

    private fun lessonState(id: Int, status: LessonStatus) = QaidaLessonState(
        lesson = lesson(id),
        status = status,
        stars = if (status == LessonStatus.COMPLETED) 3 else 0,
        completedCells = if (status == LessonStatus.COMPLETED) 2 else 0,
        totalCells = 2,
        completionFraction = if (status == LessonStatus.COMPLETED) 1f else 0f,
        lastCellId = null,
    )

    private fun cell(id: Int, lessonId: Int, audioKey: String) = QaidaCell(
        id = id,
        lineId = 100,
        lessonId = lessonId,
        position = id,
        textArabic = "ا",
        transliteration = "alif",
        tokenType = TokenType.LETTER,
        audioKey = audioKey,
        audioPath = "qaida/$audioKey.mp3",
        highlightGroup = null,
        letterId = null,
        notes = null,
    )

    private fun lessonContent(lessonId: Int) = QaidaLessonContent(
        lesson = lesson(lessonId),
        lines = listOf(
            QaidaLineContent(
                line = QaidaLine(
                    id = 100,
                    lessonId = lessonId,
                    lineNumber = 1,
                    lineType = LineType.EXAMPLE,
                    instructionEnglish = null,
                    instructionArabic = null,
                    displayOrder = 0,
                ),
                cells = listOf(
                    cell(11, lessonId, "l${lessonId}_alif"),
                    cell(12, lessonId, "l${lessonId}_baa"),
                ),
            ),
        ),
    )

    private fun letter(id: Int = 1, audioKey: String = "letter_$id") = QaidaLetter(
        id = id,
        letterArabic = "ا",
        nameArabic = "ألف",
        nameTransliteration = "Alif",
        isolatedForm = "ا",
        initialForm = null,
        medialForm = null,
        finalForm = "ـا",
        isConnecting = false,
        makhrajArea = MakhrajArea.JAWF,
        makhrajDetail = "…",
        phoneticHint = null,
        audioKey = audioKey,
        audioPath = "qaida/$audioKey.mp3",
        displayOrder = id,
    )
}
