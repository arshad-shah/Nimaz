package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The one "voice" row, shared by the reciter picker and the muezzin section.
 *
 * Its two taps are the whole point and they must not be one: tapping the **card** selects the
 * voice, tapping the **round button** auditions it. A card that treated the button as part of
 * itself would change the user's reciter every time they pressed play, which is exactly the kind
 * of thing nobody notices until their recitation has silently changed.
 *
 * The button also has four states over three flags, and the ordering is load-bearing: downloading
 * outranks playing, playing outranks downloaded, and only the last arm offers a download. Getting
 * that wrong shows a play button on a file that has not arrived.
 *
 * `voiceInitials` looks trivial and is not — it feeds the avatar for every reciter and muezzin in
 * the app, and the names it is given carry brackets, hyphens and honorifics.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class VoiceOptionCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private var selects = 0
    private var previews = 0

    private fun setContent(
        name: String = "Mishary Rashid Alafasy",
        primaryTag: String = "Murattal",
        secondaryTag: String? = "Kuwait",
        isSelected: Boolean = false,
        isPlaying: Boolean = false,
        isDownloading: Boolean = false,
        isDownloaded: Boolean = true,
    ) {
        composeRule.setThemedContent {
            VoiceOptionCard(
                name = name,
                primaryTag = primaryTag,
                secondaryTag = secondaryTag,
                isSelected = isSelected,
                isPlaying = isPlaying,
                isDownloading = isDownloading,
                isDownloaded = isDownloaded,
                previewContentDescription = PREVIEW,
                onClick = { selects++ },
                onPreviewClick = { previews++ },
            )
        }
    }

    @Test
    fun `the card shows the name and both chips`() {
        setContent()

        composeRule.onNodeWithText("Mishary Rashid Alafasy").assertIsDisplayed()
        composeRule.onNodeWithText("Murattal").assertExists()
        composeRule.onNodeWithText("Kuwait").assertExists()
    }

    @Test
    fun `a voice with one chip renders without an empty second one`() {
        setContent(secondaryTag = null)

        composeRule.onNodeWithText("Murattal").assertExists()
        composeRule.onNodeWithText("Kuwait").assertDoesNotExist()
    }

    @Test
    fun `tapping the card selects the voice without auditioning it`() {
        setContent()

        composeRule.onNodeWithText("Mishary Rashid Alafasy").performClick()

        assertThat(selects).isEqualTo(1)
        assertThat(previews).isEqualTo(0)
    }

    @Test
    fun `tapping the preview button auditions without changing the selection`() {
        // The button sits inside a clickable card. If its tap fell through, every press of play
        // would also switch the user's reciter — and the row would look identical.
        setContent()

        composeRule.onNodeWithContentDescription(PREVIEW).performClick()

        assertThat(previews).isEqualTo(1)
        assertThat(selects).isEqualTo(0)
    }

    @Test
    fun `the avatar shows the monogram when the voice is idle`() {
        setContent(name = "Mishary Rashid Alafasy")

        composeRule.onNodeWithText("MR").assertExists()
    }

    @Test
    fun `the avatar swaps the monogram for an equalizer while playing`() {
        // The equalizer is an infinite animation, so the clock is pinned before `setContent`;
        // `waitForIdle` on a running one never returns.
        composeRule.mainClock.autoAdvance = false
        setContent(isPlaying = true)

        composeRule.onNodeWithText("MR").assertDoesNotExist()
    }

    @Test
    fun `a playing voice offers a pause, not another play`() {
        composeRule.mainClock.autoAdvance = false
        setContent(isPlaying = true)

        composeRule.onNodeWithContentDescription(PREVIEW).performClick()

        assertThat(previews).isEqualTo(1)
    }

    @Test
    fun `a downloading voice shows a spinner rather than a play button`() {
        // Downloading outranks playing in the `when`. A play glyph on a file that has not
        // arrived invites a second tap and a second download.
        composeRule.mainClock.autoAdvance = false
        setContent(isDownloading = true, isDownloaded = false)

        composeRule.onNodeWithText("MR").assertExists()
    }

    @Test
    fun `a voice that is not downloaded still offers its control`() {
        setContent(isDownloaded = false)

        composeRule.onNodeWithContentDescription(PREVIEW).performClick()

        assertThat(previews).isEqualTo(1)
    }

    @Test
    fun `the initials come from the first two words`() {
        assertThat(voiceInitials("Mishary Rashid Alafasy")).isEqualTo("MR")
    }

    @Test
    fun `a single-word name uses its first two letters`() {
        // "M" alone in a 48dp circle reads as a placeholder rather than a name.
        assertThat(voiceInitials("Sudais")).isEqualTo("SU")
    }

    @Test
    fun `punctuation between names is a separator, not part of a word`() {
        // Reciter names arrive with brackets, hyphens and en dashes, and each is split on —
        // "Mishary (Mujawwad)" must not produce "M(", and a bracketed style must not become
        // the second initial by accident of position.
        assertThat(voiceInitials("Mishary (Mujawwad)")).isEqualTo("MM")
        assertThat(voiceInitials("Saad — Al-Ghamdi")).isEqualTo("SA")
        // A hyphenated given name is two words to this splitter, so "Abdul-Basit Abdus-Samad"
        // is AB rather than AA. Pinned as the behaviour it is: the avatar is a monogram, not a
        // transliteration, and changing the split would change every hyphenated reciter's badge.
        assertThat(voiceInitials("Abdul-Basit Abdus-Samad")).isEqualTo("AB")
    }

    @Test
    fun `a name with no letters falls back rather than rendering blank`() {
        // An empty monogram is an empty circle, which reads as a rendering failure.
        assertThat(voiceInitials("")).isEqualTo("?")
        assertThat(voiceInitials("123 456")).isEqualTo("?")
    }

    @Test
    fun `initials are uppercased whatever case the name arrives in`() {
        assertThat(voiceInitials("mishary rashid")).isEqualTo("MR")
    }

    private companion object {
        const val PREVIEW = "Preview"
    }
}
