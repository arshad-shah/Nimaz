package com.arshadshah.nimaz.presentation.screens.qaida

import android.content.Context
import androidx.compose.ui.test.*
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.feature.content.R
import com.arshadshah.nimaz.data.audio.QaidaAudioState
import com.arshadshah.nimaz.domain.model.QaidaLessonContent
import com.arshadshah.nimaz.presentation.viewmodel.content.*
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The screen must never imply a tap was heard, or a self-check was machine-scored. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class QaidaReaderScreenTest {
    @get:Rule val composeRule = createComponentComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val content = MutableStateFlow<QaidaLessonContent?>(qaidaLessonContent())
    private val download = MutableStateFlow(QaidaDownloadState())
    private val audio = MutableStateFlow(QaidaAudioState())
    private val events = mutableListOf<QaidaReaderEvent>()
    private var backs = 0
    private val vm: QaidaReaderViewModel = mockk(relaxed = true) {
        every { lessonContent } returns content
        every { selectedLessonId } returns MutableStateFlow<Int?>(1)
        every { courseProgress } returns MutableStateFlow(qaidaCourse())
        every { this@mockk.download } returns this@QaidaReaderScreenTest.download
        every { audioState } returns audio
        every { completedCellIds } returns MutableStateFlow(emptySet())
        every { resumeCell(any()) } returns null
        every { dueCells(any()) } returns emptySet()
        every { onEvent(any()) } answers { events += firstArg<QaidaReaderEvent>() }
    }
    private fun text(id: Int) = composeRule.onNodeWithText(context.getString(id))
    private fun show() = composeRule.setThemedContent { QaidaReaderScreen(1, { backs++ }, vm) }
    private fun begin() { text(R.string.qaida_begin_lesson).performClick() }

    @Test fun `introduction opens the requested lesson and explains the learning loop`() {
        show()
        text(R.string.qaida_lesson_intro).assertExists()
        assertThat(events).contains(QaidaReaderEvent.SelectLesson(1))
    }
    @Test fun `practice is available without audio and play is disabled`() {
        show(); begin()
        text(R.string.qaida_play_sound).assertIsNotEnabled()
        text(R.string.qaida_audio_unavailable).assertExists()
        text(R.string.qaida_practise).performClick()
        text(R.string.qaida_reveal).performClick()
        text(R.string.qaida_confident).performClick()
        assertThat(events.filterIsInstance<QaidaReaderEvent.PractisedCell>()).containsExactly(
            QaidaReaderEvent.PractisedCell(content.value!!.lines.first().cells.first(), true))
    }
    @Test fun `verified audio enables playback and repeat`() {
        download.value = QaidaDownloadState(ready = true)
        show(); begin(); text(R.string.qaida_play_sound).performClick()
        text(R.string.qaida_repeat_sound).performClick()
        assertThat(events.filterIsInstance<QaidaReaderEvent.CellTapped>()).hasSize(1)
        assertThat(events.filterIsInstance<QaidaReaderEvent.RepeatCell>().single().times).isEqualTo(3)
    }
    @Test fun `retry dispatches download without crediting practice`() {
        download.value = QaidaDownloadState(failed = true)
        show(); text(R.string.qaida_retry_audio).performClick()
        assertThat(events).contains(QaidaReaderEvent.RetryAudio)
        assertThat(events.filterIsInstance<QaidaReaderEvent.PractisedCell>()).isEmpty()
    }
    @Test fun `next sound alone does not claim self practice at completion`() {
        show(); begin(); text(R.string.qaida_next_sound).performClick(); text(R.string.qaida_finish_session).performClick()
        text(R.string.qaida_reward_title).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.qaida_session_count, 0)).assertExists()
        text(R.string.qaida_back_journey).performClick()
        assertThat(backs).isEqualTo(1)
        assertThat(events.filterIsInstance<QaidaReaderEvent.PractisedCell>()).isEmpty()
    }
    @Test fun `review starts with only the due cells`() {
        every { vm.dueCells(1) } returns setOf(2)
        show(); text(R.string.qaida_review_lesson).performClick()
        composeRule.onNodeWithText(context.getString(R.string.qaida_cell_position, 1, 1)).assertExists()
        composeRule.onNodeWithText("ب").assertExists()
    }
    @Test fun `overview returns to the selected learning card`() {
        show(); begin(); text(R.string.qaida_all_cells).performClick()
        composeRule.onNodeWithText("ba").performClick()
        composeRule.onNodeWithText(context.getString(R.string.qaida_cell_position, 2, 2)).assertExists()
    }
}
