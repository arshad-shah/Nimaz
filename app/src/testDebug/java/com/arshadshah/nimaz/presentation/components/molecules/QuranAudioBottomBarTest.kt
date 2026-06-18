package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranAudioBottomBarTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun idleState_showsSurahNameAndPositionChips_andPlayButton() {
        composeRule.setThemedContent {
            AudioBottomBar(
                isAudioActive = false,
                isPlaying = false,
                isDownloading = false,
                isPreparing = false,
                downloadProgress = 0f,
                downloadedCount = 0,
                totalToDownload = 0,
                surahName = "Al-Baqarah",
                currentAyahInSurah = 47,
                totalAyahsInSurah = 286,
                pageNumber = 8,
                juzNumber = 1,
                onPlayClick = {},
                onStopClick = {}
            )
        }

        composeRule.onNodeWithText("Al-Baqarah").assertExists()
        composeRule.onNodeWithText("Ayah 47 / 286").assertExists()
        composeRule.onNodeWithText("p. 8").assertExists()
        composeRule.onNodeWithText("Juz 1").assertExists()
        composeRule.onNodeWithContentDescription("Play from current ayah").assertExists()
    }

    @Test
    fun idleState_noStopButton() {
        composeRule.setThemedContent {
            AudioBottomBar(
                isAudioActive = false,
                isPlaying = false,
                isDownloading = false,
                isPreparing = false,
                downloadProgress = 0f,
                downloadedCount = 0,
                totalToDownload = 0,
                surahName = "Al-Fatihah",
                currentAyahInSurah = 1,
                totalAyahsInSurah = 7,
                pageNumber = 1,
                juzNumber = 1,
                onPlayClick = {},
                onStopClick = {}
            )
        }

        composeRule.onNodeWithContentDescription("Stop audio").assertDoesNotExist()
    }

    @Test
    fun playingState_showsPauseIcon_andStopButton() {
        composeRule.setThemedContent {
            AudioBottomBar(
                isAudioActive = true,
                isPlaying = true,
                isDownloading = false,
                isPreparing = false,
                downloadProgress = 0f,
                downloadedCount = 0,
                totalToDownload = 0,
                surahName = "Al-Fatihah",
                currentAyahInSurah = 4,
                totalAyahsInSurah = 7,
                pageNumber = 1,
                juzNumber = 1,
                onPlayClick = {},
                onStopClick = {}
            )
        }

        composeRule.onNodeWithContentDescription("Pause").assertExists()
        composeRule.onNodeWithContentDescription("Stop audio").assertExists()
    }

    @Test
    fun preparingState_showsDownloadingText() {
        // The play button shows an indeterminate CircularProgressIndicator while
        // preparing/downloading; its infinite animation would stall waitForIdle,
        // so pause the clock before composing.
        composeRule.mainClock.autoAdvance = false
        composeRule.setThemedContent {
            AudioBottomBar(
                isAudioActive = true,
                isPlaying = false,
                isDownloading = true,
                isPreparing = true,
                downloadProgress = 0.65f,
                downloadedCount = 5,
                totalToDownload = 7,
                surahName = "Al-Baqarah",
                currentAyahInSurah = 1,
                totalAyahsInSurah = 286,
                pageNumber = 2,
                juzNumber = 1,
                onPlayClick = {},
                onStopClick = {}
            )
        }

        composeRule.onNodeWithText("Downloading 5 / 7…").assertExists()
        // Surah name should not be shown in preparing state
        composeRule.onNodeWithText("Al-Baqarah").assertDoesNotExist()
        // Stop button visible while preparing
        composeRule.onNodeWithContentDescription("Stop audio").assertExists()
    }

    @Test
    fun playClick_invokesCallback() {
        var clicked = false
        composeRule.setThemedContent {
            AudioBottomBar(
                isAudioActive = false,
                isPlaying = false,
                isDownloading = false,
                isPreparing = false,
                downloadProgress = 0f,
                downloadedCount = 0,
                totalToDownload = 0,
                surahName = "Al-Fatihah",
                currentAyahInSurah = 1,
                totalAyahsInSurah = 7,
                pageNumber = 1,
                juzNumber = 1,
                onPlayClick = { clicked = true },
                onStopClick = {}
            )
        }

        composeRule.onNodeWithContentDescription("Play from current ayah").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun stopClick_invokesCallback() {
        var stopped = false
        composeRule.setThemedContent {
            AudioBottomBar(
                isAudioActive = true,
                isPlaying = true,
                isDownloading = false,
                isPreparing = false,
                downloadProgress = 0f,
                downloadedCount = 0,
                totalToDownload = 0,
                surahName = "Al-Fatihah",
                currentAyahInSurah = 4,
                totalAyahsInSurah = 7,
                pageNumber = 1,
                juzNumber = 1,
                onPlayClick = {},
                onStopClick = { stopped = true }
            )
        }

        composeRule.onNodeWithContentDescription("Stop audio").performClick()
        assertThat(stopped).isTrue()
    }

    @Test
    fun zeroTotalAyahs_stillRendersSurahName() {
        composeRule.setThemedContent {
            AudioBottomBar(
                isAudioActive = false,
                isPlaying = false,
                isDownloading = false,
                isPreparing = false,
                downloadProgress = 0f,
                downloadedCount = 0,
                totalToDownload = 0,
                surahName = "Al-Ikhlas",
                currentAyahInSurah = 0,
                totalAyahsInSurah = 0,
                pageNumber = 604,
                juzNumber = 30,
                onPlayClick = {},
                onStopClick = {}
            )
        }

        composeRule.onNodeWithText("Al-Ikhlas").assertExists()
        composeRule.onNodeWithText("Ayah 0 / 0").assertExists()
    }
}
