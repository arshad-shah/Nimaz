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

    /**
     * The bar is a *player*, not furniture: with nothing playing and nothing being fetched it
     * draws nothing at all, so the reader keeps the bottom of the page.
     */
    @Test
    fun idleState_rendersNothing() {
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
                onPlayClick = {}
            )
        }

        composeRule.onNodeWithText("Al-Baqarah").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Play").assertDoesNotExist()
    }

    @Test
    fun activeState_showsSurahNameAndPosition_andTransport() {
        composeRule.setThemedContent {
            AudioBottomBar(
                isAudioActive = true,
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
                onPlayClick = {}
            )
        }

        composeRule.onNodeWithText("Al-Baqarah").assertExists()
        // Position is a single combined line ("Ayah X / Y · Juz · p.").
        composeRule.onNodeWithText("Ayah 47 / 286", substring = true).assertExists()
        composeRule.onNodeWithText("p. 8", substring = true).assertExists()
        composeRule.onNodeWithText("Juz 1", substring = true).assertExists()
        composeRule.onNodeWithContentDescription("Play").assertExists()
        composeRule.onNodeWithContentDescription("Previous verse").assertExists()
        composeRule.onNodeWithContentDescription("Next verse").assertExists()
    }

    @Test
    fun playingState_showsPauseIcon() {
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
                onPlayClick = {}
            )
        }

        composeRule.onNodeWithContentDescription("Pause").assertExists()
        // Stop is not on the bar: it lives in the recitation sheet behind the settings button,
        // out of mis-tap range of next/previous.
        composeRule.onNodeWithContentDescription("Stop audio").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Recitation").assertExists()
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
                onPlayClick = {}
            )
        }

        composeRule.onNodeWithText("Downloading 5 of 7").assertExists()
        // The download strip sits *above* the now-playing line rather than replacing it: what
        // is being fetched is the thing being named, and hiding it left a progress bar with no
        // subject.
        composeRule.onNodeWithText("Al-Baqarah").assertExists()
    }

    @Test
    fun playClick_invokesCallback() {
        var clicked = false
        composeRule.setThemedContent {
            AudioBottomBar(
                isAudioActive = true,
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
                onPlayClick = { clicked = true }
            )
        }

        composeRule.onNodeWithContentDescription("Play").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun zeroTotalAyahs_stillRendersSurahName() {
        composeRule.setThemedContent {
            AudioBottomBar(
                isAudioActive = true,
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
                onPlayClick = {}
            )
        }

        composeRule.onNodeWithText("Al-Ikhlas").assertExists()
        composeRule.onNodeWithText("Ayah 0 / 0", substring = true).assertExists()
    }
}
