package com.arshadshah.nimaz.presentation.viewmodel.learnpray

import androidx.lifecycle.SavedStateHandle
import com.arshadshah.nimaz.data.audio.PrayerAudioManager
import com.arshadshah.nimaz.domain.model.PrayerAudio
import com.arshadshah.nimaz.domain.model.PrayerAudioState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import com.arshadshah.nimaz.presentation.model.PrayerFigure
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LearnPrayViewModelTest {
    private val audio = mockk<PrayerAudioManager>(relaxed = true).also {
        every { it.state } returns MutableStateFlow(PrayerAudioState())
    }
    private fun newViewModel(handle: SavedStateHandle) = LearnPrayViewModel(handle, audio)

    @Test fun navigationStopsAudioAndPlaybackDoesNotAdvanceLesson() {
        val vm = newViewModel(SavedStateHandle())
        vm.onEvent(LearnPrayEvent.ToggleAudio(PrayerAudio.FATIHAH))
        verify { audio.toggle(PrayerAudio.FATIHAH) }
        assertThat(vm.uiState.value.page).isEqualTo(-1)
        vm.onEvent(LearnPrayEvent.Next)
        verify { audio.stop() }
    }

    @Test fun illustrationChoicePersistsWithoutChangingTheLessonPosition() {
        val handle = SavedStateHandle()
        val vm = newViewModel(handle)
        repeat(5) { vm.onEvent(LearnPrayEvent.Next) }
        vm.onEvent(LearnPrayEvent.SelectFigure(PrayerFigure.WOMAN))
        assertThat(vm.uiState.value.page).isEqualTo(4)
        assertThat(vm.uiState.value.figure).isEqualTo(PrayerFigure.WOMAN)
        val restored = SavedStateHandle(mapOf(
            LearnPrayViewModel.PAGE_KEY to handle.get<Int>(LearnPrayViewModel.PAGE_KEY),
            LearnPrayViewModel.FIGURE_KEY to handle.get<String>(LearnPrayViewModel.FIGURE_KEY),
        ))
        assertThat(newViewModel(restored).uiState.value).isEqualTo(vm.uiState.value)
        vm.onEvent(LearnPrayEvent.Restart)
        assertThat(vm.uiState.value.figure).isEqualTo(PrayerFigure.WOMAN)
        assertThat(vm.uiState.value.preparing).isTrue()
    }

    @Test fun unknownSavedFigureFallsBackSafely() {
        val vm = newViewModel(SavedStateHandle(mapOf(LearnPrayViewModel.FIGURE_KEY to "unknown")))
        assertThat(vm.uiState.value.figure).isEqualTo(PrayerFigure.MAN)
    }

    @Test fun startsWithPreparation() {
        assertThat(newViewModel(SavedStateHandle()).uiState.value.preparing).isTrue()
    }

    @Test fun visitsEveryMovementBeforeCompleting() {
        val vm = newViewModel(SavedStateHandle())
        repeat(LearnPrayUiState.STEP_COUNT) { index ->
            vm.onEvent(LearnPrayEvent.Next)
            assertThat(vm.uiState.value.page).isEqualTo(index)
            assertThat(vm.uiState.value.complete).isFalse()
        }
        vm.onEvent(LearnPrayEvent.Next)
        assertThat(vm.uiState.value.complete).isTrue()
        vm.onEvent(LearnPrayEvent.Next)
        assertThat(vm.uiState.value.page).isEqualTo(LearnPrayUiState.STEP_COUNT)
    }

    @Test fun previousAndRestartStayInBounds() {
        val vm = newViewModel(SavedStateHandle())
        vm.onEvent(LearnPrayEvent.Previous)
        assertThat(vm.uiState.value.preparing).isTrue()
        repeat(10) { vm.onEvent(LearnPrayEvent.Next) }
        vm.onEvent(LearnPrayEvent.Previous)
        assertThat(vm.uiState.value.page).isEqualTo(8)
        vm.onEvent(LearnPrayEvent.Restart)
        assertThat(vm.uiState.value.preparing).isTrue()
    }

    @Test fun restoresSavedLessonPosition() {
        val handle = SavedStateHandle()
        val vm = newViewModel(handle)
        repeat(8) { vm.onEvent(LearnPrayEvent.Next) }
        val restored = SavedStateHandle(mapOf(LearnPrayViewModel.PAGE_KEY to handle.get<Int>(LearnPrayViewModel.PAGE_KEY)))
        assertThat(newViewModel(restored).uiState.value.page).isEqualTo(7)
    }

    @Test fun clampsOutOfRangeSavedState() {
        listOf(-999 to -1, 999 to LearnPrayUiState.STEP_COUNT).forEach { (saved, expected) ->
            val vm = newViewModel(SavedStateHandle(mapOf(LearnPrayViewModel.PAGE_KEY to saved)))
            assertThat(vm.uiState.value.page).isEqualTo(expected)
        }
    }
}
