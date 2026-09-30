@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import app.cash.turbine.test
import com.arshadshah.nimaz.data.audio.QaidaAudioManager
import com.arshadshah.nimaz.data.audio.QaidaAudioState
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.domain.model.LineType
import com.arshadshah.nimaz.domain.model.QaidaCell
import com.arshadshah.nimaz.domain.model.QaidaCourseProgress
import com.arshadshah.nimaz.domain.model.QaidaLesson
import com.arshadshah.nimaz.domain.model.QaidaLessonContent
import com.arshadshah.nimaz.domain.model.QaidaLessonState
import com.arshadshah.nimaz.domain.model.QaidaLine
import com.arshadshah.nimaz.domain.model.QaidaLineContent
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
import io.mockk.coEvery
import io.mockk.coVerify
import com.arshadshah.nimaz.data.audio.QaidaLessonAudioStore
import com.arshadshah.nimaz.data.qaida.QaidaJourneyStore
import com.arshadshah.nimaz.data.qaida.QaidaLearningSettings
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

@OptIn(ExperimentalCoroutinesApi::class)
class QaidaReaderViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var getLessonContent: GetQaidaLessonContentUseCase
    private lateinit var getLetters: GetQaidaLettersUseCase
    private lateinit var getLessonProgress: GetLessonProgressUseCase
    private lateinit var getCourseProgress: GetCourseProgressUseCase
    private lateinit var markCellHeard: MarkCellHeardUseCase
    private lateinit var useCases: QaidaUseCases
    private lateinit var audioManager: QaidaAudioManager
    private lateinit var audioStateFlow: MutableStateFlow<QaidaAudioState>
    private lateinit var completionsFlow: MutableSharedFlow<String>

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        getLessonContent = mockk()
        getLetters = mockk()
        getLessonProgress = mockk()
        getCourseProgress = mockk()
        markCellHeard = mockk(relaxed = true)

        useCases = QaidaUseCases(
            getLessons = mockk<GetQaidaLessonsUseCase>(relaxed = true),
            getLessonContent = getLessonContent,
            getLetters = getLetters,
            getCell = mockk<GetQaidaCellUseCase>(relaxed = true),
            markCellHeard = markCellHeard,
            unlockNextLesson = mockk<UnlockNextLessonUseCase>(relaxed = true),
            getLessonProgress = getLessonProgress,
            getCourseProgress = getCourseProgress,
            resetProgress = mockk<ResetQaidaProgressUseCase>(relaxed = true),
            observeCompletedCells = mockk<ObserveCompletedCellsUseCase>(relaxed = true)
        )

        audioManager = mockk(relaxed = true)
        audioStateFlow = MutableStateFlow(QaidaAudioState())
        completionsFlow = MutableSharedFlow(extraBufferCapacity = 32)
        every { audioManager.state } returns audioStateFlow
        every { audioManager.completions } returns completionsFlow


        // Default stubs — individual tests override the per-lesson ones.
        every { getLetters.invoke() } returns flowOf(emptyList())
        every { getCourseProgress.invoke() } returns flowOf(courseProgress())
        every { getLessonProgress.invoke(any()) } returns flowOf(lessonState(1))
        every { getLessonContent.invoke(1) } returns flowOf(lessonContent(1))
        every { getLessonContent.invoke(2) } returns flowOf(lessonContent(2))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel() = QaidaReaderViewModel(useCases, audioManager, RecordingTelemetry())

    @Test
    fun `tapping a cell plays but does not credit unheard audio`() = runTest {
        val vm = createViewModel()
        val cell = cell(id = 11, lessonId = 1, audioKey = "l1_alif")

        vm.onEvent(QaidaReaderEvent.CellTapped(cell))
        advanceUntilIdle()

        verify { audioManager.play("l1_alif") }
        coVerify(exactly = 0) { markCellHeard.invoke(1, 11, any()) }
    }

    @Test
    fun `selecting a lesson loads its content`() = runTest {
        val vm = createViewModel()

        vm.lessonContent.test {
            assertThat(awaitItem()).isNull() // nothing selected yet
            vm.onEvent(QaidaReaderEvent.SelectLesson(1))
            assertThat(awaitItem()?.lesson?.id).isEqualTo(1)
        }
    }

    @Test
    fun `switching lessons stops audio and swaps content`() = runTest {
        val vm = createViewModel()

        vm.lessonContent.test {
            assertThat(awaitItem()).isNull()
            vm.onEvent(QaidaReaderEvent.SelectLesson(1))
            assertThat(awaitItem()?.lesson?.id).isEqualTo(1)
            vm.onEvent(QaidaReaderEvent.SelectLesson(2))
            assertThat(awaitItem()?.lesson?.id).isEqualTo(2)
        }

        // stop() is called on every lesson change.
        verify(atLeast = 2) { audioManager.stop() }
    }

    @Test
    fun `resume opens the course resume pointer`() = runTest {
        every { getCourseProgress.invoke() } returns flowOf(courseProgress(nextLessonId = 2))
        val vm = createViewModel()

        vm.courseProgress.test {
            awaitItem() // initial null
            awaitItem() // emitted rollup
            vm.onEvent(QaidaReaderEvent.Resume)
        }
        advanceUntilIdle()

        assertThat(vm.selectedLessonId.value).isEqualTo(2)
    }

    @Test
    fun `playLine plays the whole line and credits each cell as its clip ends`() = runTest {
        val vm = createViewModel()

        vm.lessonContent.test {
            assertThat(awaitItem()).isNull()
            vm.onEvent(QaidaReaderEvent.SelectLesson(1))
            assertThat(awaitItem()?.lesson?.id).isEqualTo(1)

            vm.onEvent(QaidaReaderEvent.PlayLine(lineId = 100))
            advanceUntilIdle()

            verify { audioManager.playSequence(listOf("l1_alif", "l1_baa")) }
            // This test previously asserted both marks **here**, which is the defect #364 R9
            // describes: progress was written from the intent to play, not from playback. A
            // learner who tapped and immediately left was credited with the whole line.
            coVerify(exactly = 0) { markCellHeard.invoke(1, 11, any()) }

            completionsFlow.emit("l1_alif")
            advanceUntilIdle()
            coVerify { markCellHeard.invoke(1, 11, any()) }
            coVerify(exactly = 0) { markCellHeard.invoke(1, 12, any()) }

            completionsFlow.emit("l1_baa")
            advanceUntilIdle()
            coVerify { markCellHeard.invoke(1, 12, any()) }
        }
    }

    @Test
    fun `playingCell resolves the cell for the currently sounding key`() = runTest {
        val vm = createViewModel()

        vm.playingCell.test {
            assertThat(awaitItem()).isNull()
            vm.onEvent(QaidaReaderEvent.SelectLesson(1))
            audioStateFlow.value = QaidaAudioState(currentKey = "l1_baa", isPlaying = true)
            advanceUntilIdle()
            assertThat(expectMostRecentItem()?.id).isEqualTo(12)
        }
    }

    @Test
    fun `nextLesson does not advance into a locked lesson`() = runTest {
        every { getCourseProgress.invoke() } returns flowOf(
            courseProgress(secondStatus = LessonStatus.LOCKED)
        )
        val vm = createViewModel()

        vm.courseProgress.test {
            awaitItem()
            awaitItem()
            vm.onEvent(QaidaReaderEvent.SelectLesson(1))
            vm.onEvent(QaidaReaderEvent.NextLesson)
        }
        advanceUntilIdle()

        assertThat(vm.selectedLessonId.value).isEqualTo(1)
    }

    @Test fun `navigation handles no course no selection and both ends without leaving bounds`() = runTest {
        val vm = createViewModel()
        vm.onEvent(QaidaReaderEvent.NextLesson)
        vm.onEvent(QaidaReaderEvent.PreviousLesson)
        vm.onEvent(QaidaReaderEvent.Resume)
        assertThat(vm.selectedLessonId.value).isNull()
        vm.courseProgress.test {
            awaitItem(); awaitItem()
            vm.onEvent(QaidaReaderEvent.NextLesson)
            vm.onEvent(QaidaReaderEvent.PreviousLesson)
            assertThat(vm.selectedLessonId.value).isNull()
            vm.onEvent(QaidaReaderEvent.SelectLesson(1))
            vm.onEvent(QaidaReaderEvent.PreviousLesson)
            assertThat(vm.selectedLessonId.value).isEqualTo(1)
            vm.onEvent(QaidaReaderEvent.NextLesson)
            assertThat(vm.selectedLessonId.value).isEqualTo(2)
            vm.onEvent(QaidaReaderEvent.NextLesson)
            assertThat(vm.selectedLessonId.value).isEqualTo(2)
            vm.onEvent(QaidaReaderEvent.PreviousLesson)
            assertThat(vm.selectedLessonId.value).isEqualTo(1)
        }
    }

    @Test fun `resume falls back to first lesson and leaves an empty course unselected`() = runTest {
        for (course in listOf(courseProgress(nextLessonId = null), courseProgress(nextLessonId = null).copy(lessons = emptyList()))) {
            every { getCourseProgress() } returns flowOf(course)
            val vm = createViewModel()
            vm.courseProgress.test {
                awaitItem(); awaitItem()
                vm.onEvent(QaidaReaderEvent.Resume)
                assertThat(vm.selectedLessonId.value).isEqualTo(course.lessons.firstOrNull()?.lesson?.id)
            }
        }
    }

    @Test fun `practice only credits selected lesson and repeats clamp to one through three`() = runTest {
        val vm = createViewModel()
        val selected = cell(11, 1, "alif")
        vm.onEvent(QaidaReaderEvent.PractisedCell(selected, true))
        vm.onEvent(QaidaReaderEvent.SelectLesson(1))
        vm.onEvent(QaidaReaderEvent.PractisedCell(cell(22, 2, "baa"), false))
        vm.onEvent(QaidaReaderEvent.PractisedCell(selected, false))
        advanceUntilIdle()
        coVerify(exactly = 1) { markCellHeard.markPractised(1, 11) }
        coVerify(exactly = 0) { markCellHeard.markPractised(2, any()) }
        vm.onEvent(QaidaReaderEvent.RepeatCell(selected, 0))
        vm.onEvent(QaidaReaderEvent.RepeatCell(selected, 2))
        vm.onEvent(QaidaReaderEvent.RepeatCell(selected, 99))
        verify { audioManager.playSequence(listOf("alif")) }
        verify { audioManager.playSequence(listOf("alif", "alif")) }
        verify { audioManager.playSequence(listOf("alif", "alif", "alif")) }
    }

    @Test fun `fallback settings review and cache work without optional stores`() = runTest {
        val vm = createViewModel()
        vm.onEvent(QaidaReaderEvent.SetSlow(true))
        vm.onEvent(QaidaReaderEvent.SetTransliteration(false))
        assertThat(vm.settings.value).isEqualTo(QaidaLearningSettings(false, true))
        vm.onEvent(QaidaReaderEvent.SetSlow(false))
        vm.onEvent(QaidaReaderEvent.SetTransliteration(true))
        assertThat(vm.settings.value).isEqualTo(QaidaLearningSettings())
        vm.refreshReview(); vm.refreshCacheSize()
        vm.onEvent(QaidaReaderEvent.RetryAudio)
        vm.onEvent(QaidaReaderEvent.ClearAudio)
        vm.onEvent(QaidaReaderEvent.StopAudio)
        advanceUntilIdle()
        assertThat(vm.cacheBytes.value).isEqualTo(0L)
        assertThat(vm.dueCells(1)).isEmpty()
        assertThat(vm.resumeCell(1)).isNull()
        assertThat(vm.download.value.ready).isFalse()
        vm.dueLessons.test { assertThat(awaitItem()).isEmpty() }
        vm.todayCount.test { assertThat(awaitItem()).isEqualTo(0) }
    }

    @Test fun `download reports progress failure retry and clear through the existing store`() = runTest {
        val store = mockk<QaidaLessonAudioStore>(relaxed = true)
        coEvery { store.sizeBytes() } returns 512L
        coEvery { store.prepare(any(), any(), any()) } coAnswers {
            thirdArg<(Int, Int) -> Unit>()(1, 2)
            awaitCancellation()
        }
        val vm = QaidaReaderViewModel(useCases, audioManager, RecordingTelemetry(), store)
        vm.onEvent(QaidaReaderEvent.SelectLesson(1)); runCurrent()
        assertThat(vm.download.value).isEqualTo(QaidaDownloadState(loading = true, completed = 1, total = 2))
        coEvery { store.prepare(any(), true, any()) } throws java.io.IOException("private server detail")
        vm.onEvent(QaidaReaderEvent.RetryAudio); runCurrent()
        assertThat(vm.download.value).isEqualTo(QaidaDownloadState(failed = true))
        assertThat(vm.cacheBytes.value).isEqualTo(512L)
        coEvery { store.prepare(any(), true, any()) } returns true
        vm.onEvent(QaidaReaderEvent.RetryAudio); runCurrent()
        assertThat(vm.download.value).isEqualTo(QaidaDownloadState(ready = true))
        coEvery { store.sizeBytes() } returns 0L
        vm.onEvent(QaidaReaderEvent.ClearAudio); runCurrent()
        coVerify { store.clear() }
        assertThat(vm.download.value).isEqualTo(QaidaDownloadState())
        assertThat(vm.cacheBytes.value).isEqualTo(0L)
    }

    @Test fun `lesson change cancels an in-flight download without showing an error`() = runTest {
        val store = mockk<QaidaLessonAudioStore>(relaxed = true)
        var cancelled = false
        coEvery { store.prepare(any(), any(), any()) } coAnswers {
            if (firstArg<QaidaLessonContent>().lesson.id == 1) {
                try { awaitCancellation() } finally { cancelled = true }
            } else false
        }
        val vm = QaidaReaderViewModel(useCases, audioManager, RecordingTelemetry(), store)
        vm.onEvent(QaidaReaderEvent.SelectLesson(1)); runCurrent()
        assertThat(vm.download.value.loading).isTrue()
        vm.onEvent(QaidaReaderEvent.SelectLesson(2)); runCurrent()
        assertThat(cancelled).isTrue()
        assertThat(vm.download.value).isEqualTo(QaidaDownloadState())
    }

    @Test fun `persistent journey receives settings practice listening resume and reset`() = runTest {
        val journey = mockk<QaidaJourneyStore>(relaxed = true)
        val preferences = MutableStateFlow(QaidaLearningSettings())
        val revision = MutableStateFlow(0)
        every { journey.settings } returns preferences
        every { journey.revision } returns revision
        every { journey.updateSettings(any()) } answers { preferences.value = firstArg() }
        every { journey.dueLessonIds(any()) } returns setOf(1)
        every { journey.todayCount(any()) } returns 3
        every { journey.dueCellIds(1, any()) } returns setOf(11)
        every { journey.resumeCell(1) } returns 12
        val vm = QaidaReaderViewModel(useCases, audioManager, RecordingTelemetry(), journey = journey)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.dueLessons.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.todayCount.collect() }
        runCurrent()
        assertThat(vm.dueLessons.value).containsExactly(1)
        assertThat(vm.todayCount.value).isEqualTo(3)
        assertThat(vm.dueCells(1)).containsExactly(11)
        assertThat(vm.resumeCell(1)).isEqualTo(12)
        vm.refreshReview(); verify { journey.refresh() }
        every { journey.todayCount(any()) } returns 4
        revision.value++; runCurrent()
        assertThat(vm.todayCount.value).isEqualTo(4)
        vm.onEvent(QaidaReaderEvent.SetSlow(true))
        vm.onEvent(QaidaReaderEvent.SetTransliteration(false))
        assertThat(preferences.value).isEqualTo(QaidaLearningSettings(false, true))
        vm.onEvent(QaidaReaderEvent.SelectLesson(1)); runCurrent()
        val token = lessonContent(1).lines.first().cells.first()
        vm.onEvent(QaidaReaderEvent.CellTapped(token))
        verify { journey.setResume(1, token.id) }
        vm.onEvent(QaidaReaderEvent.PractisedCell(token, true)); runCurrent()
        verify { journey.record(1, token.id, true, any()) }
        completionsFlow.emit(token.audioKey); runCurrent()
        verify { journey.recordActivity(token.id, any()) }
        assertThat(vm.sessionHeard.value).containsExactly(token.id)
        vm.onEvent(QaidaReaderEvent.SelectLesson(1)); runCurrent()
        assertThat(vm.sessionHeard.value).containsExactly(token.id)
        vm.onEvent(QaidaReaderEvent.SelectLesson(2)); runCurrent()
        assertThat(vm.sessionHeard.value).isEmpty()
        completionsFlow.emit("unknown"); runCurrent()
        assertThat(vm.sessionHeard.value).isEmpty()
        vm.onEvent(QaidaReaderEvent.ResetJourney); runCurrent()
        coVerify { useCases.resetProgress() }
        verify { journey.reset() }
        assertThat(vm.selectedLessonId.value).isNull()
    }

    @Test fun `missing and empty lines never start playback and reset works without journey store`() = runTest {
        val vm = createViewModel()
        vm.onEvent(QaidaReaderEvent.PlayLine(100))
        every { getLessonContent(1) } returns flowOf(lessonContent(1).let { it.copy(lines = it.lines.map { line -> line.copy(cells = emptyList()) }) })
        vm.onEvent(QaidaReaderEvent.SelectLesson(1)); runCurrent()
        vm.onEvent(QaidaReaderEvent.PlayLine(999))
        vm.onEvent(QaidaReaderEvent.PlayLine(100))
        verify(exactly = 0) { audioManager.playSequence(any()) }
        vm.onEvent(QaidaReaderEvent.ResetJourney); runCurrent()
        coVerify { useCases.resetProgress() }
        assertThat(vm.selectedLessonId.value).isNull()
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private fun cell(
        id: Int,
        lessonId: Int,
        audioKey: String,
        lineId: Int = 100,
        position: Int = 0
    ) = QaidaCell(
        id = id,
        lineId = lineId,
        lessonId = lessonId,
        position = position,
        textArabic = "ا",
        transliteration = "alif",
        tokenType = TokenType.LETTER,
        audioKey = audioKey,
        audioPath = "file:///android_asset/qaida/audio/$audioKey.mp3",
        highlightGroup = null,
        letterId = null,
        notes = null
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
                    displayOrder = 0
                ),
                cells = listOf(
                    cell(id = 11, lessonId = lessonId, audioKey = "l${lessonId}_alif", position = 0),
                    cell(id = 12, lessonId = lessonId, audioKey = "l${lessonId}_baa", position = 1)
                )
            )
        )
    )

    private fun lesson(id: Int) = QaidaLesson(
        id = id,
        lessonNumber = id,
        titleEnglish = "Lesson $id",
        titleArabic = "درس",
        titleTransliteration = "dars",
        description = "",
        conceptTags = emptyList(),
        icon = "book",
        displayOrder = id
    )

    private fun lessonState(id: Int, status: LessonStatus = LessonStatus.IN_PROGRESS) =
        QaidaLessonState(
            lesson = lesson(id),
            status = status,
            stars = 1,
            completedCells = 1,
            totalCells = 2,
            completionFraction = 0.5f,
            lastCellId = 11
        )

    private fun courseProgress(
        nextLessonId: Int? = 1,
        secondStatus: LessonStatus = LessonStatus.UNLOCKED
    ) = QaidaCourseProgress(
        lessons = listOf(
            lessonState(1, LessonStatus.IN_PROGRESS),
            lessonState(2, secondStatus)
        ),
        completedLessons = 0,
        totalLessons = 2,
        totalStars = 1,
        maxStars = 6,
        totalCellsHeard = 1,
        overallFraction = 0f,
        nextLessonId = nextLessonId
    )
}
