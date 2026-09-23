package com.arshadshah.nimaz.presentation.screens.learnpray

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.data.audio.PrayerAudioManager
import com.arshadshah.nimaz.domain.model.ContentTarget
import com.arshadshah.nimaz.domain.model.PrayerAudioState
import com.arshadshah.nimaz.presentation.viewmodel.learnpray.LearnPrayViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The lesson's entry point: the wiring between the screen, its ViewModel and the audio player.
 *
 * Recitation audio must never outlive the lesson. It stops when the app goes to the background
 * (ON_STOP), when the screen leaves composition, and before a reference opens the Qur'an or hadith
 * reader — otherwise a recitation keeps playing over whatever the reader opens next.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LearnToPrayEntryTest {
    @get:Rule val composeRule = createComponentComposeRule()
    private fun string(id: Int) = ApplicationProvider.getApplicationContext<Context>().getString(id)

    private val audio = mockk<PrayerAudioManager>(relaxed = true) {
        every { state } returns MutableStateFlow(PrayerAudioState())
    }
    private val viewModel = LearnPrayViewModel(SavedStateHandle(), audio)
    /** A lifecycle the test can drive; the screen reads it through [LocalLifecycleOwner]. */
    private class DrivenLifecycle : LifecycleOwner {
        val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        fun handleLifecycleEvent(event: Lifecycle.Event) = registry.handleLifecycleEvent(event)
    }
    private val lifecycle = DrivenLifecycle()
    private val shown = mutableStateOf(true)
    private val opened = mutableListOf<ContentTarget>()
    private var exited = 0

    private fun launch() = composeRule.setThemedContent {
        CompositionLocalProvider(LocalLifecycleOwner provides (lifecycle as LifecycleOwner)) {
            if (shown.value) {
                LearnToPrayScreen(
                    onNavigateBack = { exited++ },
                    onOpenReference = { opened += it },
                    viewModel = viewModel,
                )
            }
        }
    }

    @Test fun `the lesson opens on its preparation page`() {
        launch()
        composeRule.onNodeWithText(string(R.string.learn_pray_prepare)).assertExists()
    }

    @Test fun `audio stops when the app goes to the background`() {
        launch()
        composeRule.runOnIdle { lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP) }
        composeRule.waitForIdle()
        verify { audio.stop() }
    }

    @Test fun `audio stops when the lesson leaves the screen`() {
        launch()
        composeRule.runOnIdle { shown.value = false }
        composeRule.waitForIdle()
        verify { audio.stop() }
    }

    @Test fun `opening a reference stops audio first, then opens it`() {
        launch()
        composeRule.onNodeWithText(string(R.string.learn_pray_sources)).performScrollTo().performClick()
        val label = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.learn_pray_reference_quran, "5:6")
        composeRule.onNodeWithText(label).performScrollTo().performClick()
        verify { audio.stop() }
        assertThat(opened).containsExactly(ContentTarget.Ayah(5, 6))
    }

    @Test fun `the top bar's back leaves the lesson`() {
        launch()
        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()
        assertThat(exited).isEqualTo(1)
    }
}
