package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The player's transport, and what the bar says about where the recitation is.
 *
 * The bar is the only thing on screen while a recitation runs, so what it *omits* matters as
 * much as what it shows: a page or juz of zero is "we do not know", not "page 0", and a
 * scrubber with no duration yet is a control that cannot be dragged anywhere. Both are drawn
 * from numbers that are legitimately absent early in a playback session.
 *
 * `QuranAudioBottomBarTest` covers the idle, playing and preparing states; this is the
 * transport, the position line and the settings affordance.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class QuranAudioBottomBarTransportTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private var played = false
    private var next = false
    private var previous = false
    private var expanded = false
    private val seeks = mutableListOf<Long>()

    private fun render(
        isPlaying: Boolean = true,
        isPreparing: Boolean = false,
        pageNumber: Int = 293,
        juzNumber: Int = 15,
        positionMs: Long = 30_000,
        durationMs: Long = 120_000,
        reciterName: String = "Mishary Alafasy",
        speedLabel: String? = null,
        repeatLabel: String? = null,
    ) {
        composeRule.setThemedContent {
            AudioBottomBar(
                isAudioActive = true,
                isPlaying = isPlaying,
                isDownloading = false,
                isPreparing = isPreparing,
                downloadProgress = 0f,
                downloadedCount = 0,
                totalToDownload = 0,
                surahName = "Al-Kahf",
                currentAyahInSurah = 10,
                totalAyahsInSurah = 110,
                pageNumber = pageNumber,
                juzNumber = juzNumber,
                onPlayClick = { played = true },
                positionMs = positionMs,
                durationMs = durationMs,
                reciterName = reciterName,
                speedLabel = speedLabel,
                repeatLabel = repeatLabel,
                onSeek = { seeks += it },
                onNextAyah = { next = true },
                onPreviousAyah = { previous = true },
                onExpand = { expanded = true },
            )
        }
    }

    // ---- Transport ----

    @Test
    fun `the transport steps a verse at a time in both directions`() {
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_next_ayah_audio)).performClick()
        composeRule.onNodeWithContentDescription(str(R.string.cd_previous_ayah_audio))
            .performClick()

        assertThat(next).isTrue()
        assertThat(previous).isTrue()
    }

    @Test
    fun `a playing recitation offers pause`() {
        render(isPlaying = true)

        composeRule.onNodeWithContentDescription(str(R.string.cd_pause)).performClick()

        assertThat(played).isTrue()
    }

    @Test
    fun `a paused recitation offers play`() {
        render(isPlaying = false)

        composeRule.onNodeWithContentDescription(str(R.string.cd_play)).performClick()

        assertThat(played).isTrue()
    }

    @Test
    fun `the recitation settings are one tap from the bar`() {
        render()

        composeRule.onNodeWithContentDescription(str(R.string.recitation_settings)).performClick()

        assertThat(expanded).isTrue()
    }

    // ---- Where the recitation is ----

    @Test
    fun `the bar names the surah and the reciter`() {
        render()

        composeRule.onNodeWithText("Al-Kahf", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Mishary Alafasy", substring = true).assertIsDisplayed()
    }

    @Test
    fun `the position line carries the page and juz`() {
        render(pageNumber = 293, juzNumber = 15)

        composeRule.onNodeWithText(str(R.string.audio_position_page_format, 293), substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.audio_position_juz_format, 15), substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `a page or juz we do not know is left out rather than printed as zero`() {
        render(pageNumber = 0, juzNumber = 0)

        composeRule.onNodeWithText(str(R.string.audio_position_page_format, 0), substring = true)
            .assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.audio_position_juz_format, 0), substring = true)
            .assertDoesNotExist()
        // The surah is still named — the bar does not go blank because one number is missing.
        composeRule.onNodeWithText("Al-Kahf", substring = true).assertIsDisplayed()
    }

    @Test
    fun `a repeat or a speed the reader chose is shown back to them`() {
        render(speedLabel = "1.25×", repeatLabel = "×3")

        composeRule.onNodeWithText("1.25×", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("×3", substring = true).assertIsDisplayed()
    }

    // ---- The scrubber ----

    @Test
    fun `a recitation with a known duration can be scrubbed`() {
        render(positionMs = 30_000, durationMs = 120_000)

        // Elapsed on the left and *remaining* on the right, so a reader can see how much of the
        // recitation is left without doing the subtraction.
        composeRule.onNodeWithText("0:30", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("-1:30", substring = true).assertIsDisplayed()
    }

    @Test
    fun `a recitation whose duration is not known yet has no scrubber`() {
        // A slider over an unknown length is a control that cannot be dragged anywhere.
        render(positionMs = 0, durationMs = 0)

        composeRule.onNodeWithText("0:00", substring = true).assertDoesNotExist()
    }

    @Test
    fun `a preparing recitation has no scrubber either`() {
        render(isPreparing = true, positionMs = 30_000, durationMs = 120_000)

        composeRule.onNodeWithText("0:30", substring = true).assertDoesNotExist()
    }

    @Test
    fun `the clock reads minutes for a short surah and hours for a long one`() {
        // Formatted here rather than by a locale: a duration formatter would print "1 hr 1 min"
        // in the width of a transport bar.
        assertThat(formatClock(0)).isEqualTo("0:00")
        assertThat(formatClock(30_000)).isEqualTo("0:30")
        assertThat(formatClock(120_000)).isEqualTo("2:00")
        assertThat(formatClock(3_661_000)).isEqualTo("1:01:01")
        // A negative position is clamped rather than printing a negative clock.
        assertThat(formatClock(-5_000)).isEqualTo("0:00")
    }
}
