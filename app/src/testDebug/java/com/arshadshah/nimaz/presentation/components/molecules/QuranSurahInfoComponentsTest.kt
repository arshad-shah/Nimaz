package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Layers
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranSurahInfoComponentsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    // ----- SurahMetaStrip -----

    @Test
    fun metaStrip_showsEveryStatsLabelAndValue() {
        // Three elevated cards became one strip; the figures still have to be readable,
        // and each label still has to sit with its own value.
        composeRule.setThemedContent {
            SurahMetaStrip(
                stats = listOf(
                    SurahMetaStat(Icons.Default.FormatListNumbered, "Verses", "286"),
                    SurahMetaStat(Icons.Default.Layers, "Juz", "1"),
                    SurahMetaStat(Icons.AutoMirrored.Filled.MenuBook, "Page", "2"),
                )
            )
        }

        composeRule.onNodeWithText("Verses").assertExists()
        composeRule.onNodeWithText("286").assertExists()
        composeRule.onNodeWithText("Juz").assertExists()
        composeRule.onNodeWithText("Page").assertExists()
        composeRule.onNodeWithText("2").assertExists()
    }

    // ----- SurahAudioControlBar -----

    @Test
    fun audioControlBar_playing_showsNowPlaying_andPauseIcon() {
        composeRule.setThemedContent {
            SurahAudioControlBar(
                isPlaying = true,
                isDownloading = false,
                isPreparing = false,
                downloadProgress = 0f,
                downloadedCount = 0,
                totalToDownload = 0,
                currentAyah = 3,
                totalAyahs = 7,
                surahProgress = 0.43f,
                onPlayPauseClick = {},
                onStopClick = {}
            )
        }

        composeRule.onNodeWithText("Now Playing").assertExists()
        composeRule.onNodeWithText("Ayah 3 of 7").assertExists()
        composeRule.onNodeWithText("43% complete").assertExists()
        composeRule.onNodeWithContentDescription("Pause").assertExists()
        composeRule.onNodeWithContentDescription("Stop").assertExists()
    }

    @Test
    fun audioControlBar_paused_showsPaused_andResumeIcon() {
        composeRule.setThemedContent {
            SurahAudioControlBar(
                isPlaying = false,
                isDownloading = false,
                isPreparing = false,
                downloadProgress = 0f,
                downloadedCount = 0,
                totalToDownload = 0,
                currentAyah = 2,
                totalAyahs = 7,
                surahProgress = 0.14f,
                onPlayPauseClick = {},
                onStopClick = {}
            )
        }

        composeRule.onNodeWithText("Paused").assertExists()
        composeRule.onNodeWithContentDescription("Resume").assertExists()
    }

    @Test
    fun audioControlBar_preparing_showsDownloadStatus() {
        // While preparing, the control shows an indeterminate
        // CircularProgressIndicator whose infinite animation would stall
        // waitForIdle; pause the clock before composing.
        composeRule.mainClock.autoAdvance = false
        composeRule.setThemedContent {
            SurahAudioControlBar(
                isPlaying = false,
                isDownloading = true,
                isPreparing = true,
                downloadProgress = 0.5f,
                downloadedCount = 4,
                totalToDownload = 8,
                currentAyah = 1,
                totalAyahs = 8,
                surahProgress = 0f,
                onPlayPauseClick = {},
                onStopClick = {}
            )
        }

        composeRule.onNodeWithText("Preparing Audio").assertExists()
        composeRule.onNodeWithText("Downloading 4 of 8 ayahs").assertExists()
        composeRule.onNodeWithText("50% complete").assertExists()
        // Spinner shown instead of play/pause icon
        composeRule.onNodeWithContentDescription("Pause").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Resume").assertDoesNotExist()
    }

    @Test
    fun audioControlBar_playPauseClick_invokesCallback() {
        var clicked = false
        composeRule.setThemedContent {
            SurahAudioControlBar(
                isPlaying = true,
                isDownloading = false,
                isPreparing = false,
                downloadProgress = 0f,
                downloadedCount = 0,
                totalToDownload = 0,
                currentAyah = 3,
                totalAyahs = 7,
                surahProgress = 0.43f,
                onPlayPauseClick = { clicked = true },
                onStopClick = {}
            )
        }

        composeRule.onNodeWithContentDescription("Pause").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun audioControlBar_stopClick_invokesCallback() {
        var stopped = false
        composeRule.setThemedContent {
            SurahAudioControlBar(
                isPlaying = true,
                isDownloading = false,
                isPreparing = false,
                downloadProgress = 0f,
                downloadedCount = 0,
                totalToDownload = 0,
                currentAyah = 3,
                totalAyahs = 7,
                surahProgress = 0.43f,
                onPlayPauseClick = {},
                onStopClick = { stopped = true }
            )
        }

        composeRule.onNodeWithContentDescription("Stop").performClick()
        assertThat(stopped).isTrue()
    }
}
