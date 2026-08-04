package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranSurahInfoSectionsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    // ── BottomActions ───────────────────────────────────────────────────────

    private fun bottomActions(
        isAudioActive: Boolean = false,
        isPlaying: Boolean = false,
        onPlayAudio: () -> Unit = {},
        onStartReading: () -> Unit = {},
        onResumeAudio: () -> Unit = {},
        onPauseAudio: () -> Unit = {},
        onStopAudio: () -> Unit = {}
    ): @androidx.compose.runtime.Composable () -> Unit = {
        BottomActions(
            isAudioActive = isAudioActive,
            isPlaying = isPlaying,
            isDownloading = false,
            isPreparing = false,
            downloadProgress = 0f,
            downloadedCount = 0,
            totalToDownload = 0,
            currentAyah = 0,
            totalAyahs = 7,
            surahProgress = 0f,
            onPlayAudio = onPlayAudio,
            onResumeAudio = onResumeAudio,
            onPauseAudio = onPauseAudio,
            onStopAudio = onStopAudio,
            onStartReading = onStartReading
        )
    }

    @Test
    fun `BottomActions shows Listen and Start Reading when audio inactive`() {
        composeRule.setThemedContent {
            bottomActions(isAudioActive = false).invoke()
        }

        composeRule.onNodeWithText("Listen").assertExists()
        composeRule.onNodeWithText("Start Reading").assertExists()
    }

    @Test
    fun `BottomActions hides Listen when audio is active`() {
        composeRule.setThemedContent {
            bottomActions(isAudioActive = true, isPlaying = true).invoke()
        }

        composeRule.onNodeWithText("Listen").assertDoesNotExist()
        // Audio control bar shows playback status
        composeRule.onNodeWithText("Now Playing").assertExists()
        composeRule.onNodeWithText("Start Reading").assertExists()
    }

    @Test
    fun `BottomActions active paused state shows Paused`() {
        composeRule.setThemedContent {
            bottomActions(isAudioActive = true, isPlaying = false).invoke()
        }

        composeRule.onNodeWithText("Paused").assertExists()
    }

    @Test
    fun `BottomActions Listen click invokes onPlayAudio`() {
        var played = false
        composeRule.setThemedContent {
            bottomActions(isAudioActive = false, onPlayAudio = { played = true }).invoke()
        }

        composeRule.onNodeWithText("Listen").performClick()
        assertThat(played).isTrue()
    }

    @Test
    fun `BottomActions Start Reading click invokes onStartReading`() {
        var reading = false
        composeRule.setThemedContent {
            bottomActions(isAudioActive = false, onStartReading = { reading = true }).invoke()
        }

        composeRule.onNodeWithText("Start Reading").performClick()
        assertThat(reading).isTrue()
    }
}
