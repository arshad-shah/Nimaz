package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.junit4.createComposeRule
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
class QuranSurahInfoSectionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun surah(
        revelationType: RevelationType = RevelationType.MECCAN,
        ayahCount: Int = 7,
        juzStart: Int = 1,
        orderInMushaf: Int = 5
    ) = Surah(
        number = 1,
        nameArabic = "الفاتحة",
        nameEnglish = "Al-Fatihah",
        nameTransliteration = "The Opening",
        revelationType = revelationType,
        ayahCount = ayahCount,
        juzStart = juzStart,
        orderInMushaf = orderInMushaf,
        startPage = 1
    )

    // ── DetailGrid ──────────────────────────────────────────────────────────

    @Test
    fun `DetailGrid shows Makkah for a Meccan surah`() {
        composeRule.setThemedContent {
            DetailGrid(surah = surah(revelationType = RevelationType.MECCAN))
        }

        composeRule.onNodeWithText("Revelation").assertExists()
        composeRule.onNodeWithText("Makkah").assertExists()
    }

    @Test
    fun `DetailGrid shows Madinah for a Medinan surah`() {
        composeRule.setThemedContent {
            DetailGrid(surah = surah(revelationType = RevelationType.MEDINAN))
        }

        composeRule.onNodeWithText("Madinah").assertExists()
    }

    @Test
    fun `DetailGrid shows juz order and verses values`() {
        composeRule.setThemedContent {
            DetailGrid(surah = surah(ayahCount = 7, juzStart = 1, orderInMushaf = 5))
        }

        composeRule.onNodeWithText("Juz").assertExists()
        composeRule.onNodeWithText("1").assertExists()
        composeRule.onNodeWithText("5 in Mushaf").assertExists()
        composeRule.onNodeWithText("7 ayahs").assertExists()
    }

    // ── ThemesList ──────────────────────────────────────────────────────────

    @Test
    fun `ThemesList renders each theme chip`() {
        composeRule.setThemedContent {
            ThemesList(themes = listOf("Praise", "Guidance", "Mercy"))
        }

        composeRule.onNodeWithText("Praise").assertExists()
        composeRule.onNodeWithText("Guidance").assertExists()
        composeRule.onNodeWithText("Mercy").assertExists()
    }

    @Test
    fun `ThemesList with empty list renders nothing`() {
        composeRule.setThemedContent {
            ThemesList(themes = emptyList())
        }

        composeRule.onNodeWithText("Praise").assertDoesNotExist()
    }

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
