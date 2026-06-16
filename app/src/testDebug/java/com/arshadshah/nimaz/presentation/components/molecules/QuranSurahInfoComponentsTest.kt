package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranSurahInfoComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun surah(
        number: Int = 1,
        nameEnglish: String = "Al-Fatihah",
        revelationType: RevelationType = RevelationType.MECCAN,
        ayahCount: Int = 7,
        orderInMushaf: Int = 5
    ) = Surah(
        number = number,
        nameArabic = "الفاتحة",
        nameEnglish = nameEnglish,
        nameTransliteration = "The Opening",
        revelationType = revelationType,
        ayahCount = ayahCount,
        juzStart = 1,
        orderInMushaf = orderInMushaf,
        startPage = 1
    )

    // ----- HeroHeader -----

    @Test
    fun heroHeader_meccan_showsNamesStatsAndMakki() {
        composeRule.setThemedContent {
            HeroHeader(surah = surah(), onNavigateBack = {})
        }

        composeRule.onNodeWithText("Al-Fatihah").assertExists()
        composeRule.onNodeWithText("\"The Opening\"").assertExists()
        composeRule.onNodeWithText("Verses").assertExists()
        composeRule.onNodeWithText("Revelation").assertExists()
        composeRule.onNodeWithText("Order").assertExists()
        composeRule.onNodeWithText("Makki").assertExists()
        // number badge + verse count + order values
        composeRule.onNodeWithText("7").assertExists()
        composeRule.onNodeWithText("5").assertExists()
        composeRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun heroHeader_medinan_showsMadani() {
        composeRule.setThemedContent {
            HeroHeader(
                surah = surah(
                    number = 2,
                    nameEnglish = "Al-Baqarah",
                    revelationType = RevelationType.MEDINAN,
                    ayahCount = 286
                ),
                onNavigateBack = {}
            )
        }

        composeRule.onNodeWithText("Madani").assertExists()
    }

    @Test
    fun heroHeader_backClick_invokesCallback() {
        var back = false
        composeRule.setThemedContent {
            HeroHeader(surah = surah(), onNavigateBack = { back = true })
        }

        composeRule.onNodeWithContentDescription("Back").performClick()
        assertThat(back).isTrue()
    }

    // ----- DetailCard -----

    @Test
    fun detailCard_showsLabelAndValue() {
        composeRule.setThemedContent {
            DetailCard(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = "Juz",
                value = "1"
            )
        }

        composeRule.onNodeWithText("Juz").assertExists()
        composeRule.onNodeWithText("1").assertExists()
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
