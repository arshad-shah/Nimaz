package com.arshadshah.nimaz.presentation.screens.qaida

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.MakhrajArea
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.presentation.viewmodel.content.QaidaReaderEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.QaidaReaderViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The letter explorer: a board of the twenty-nine letters, and a sheet per letter.
 *
 * The screen itself is small, and what it owns is **the selection**: `selected` is local state,
 * the sheet exists only while it is non-null, and dismissing must clear it. A sheet that stays
 * open after dismissal, or one whose play button fires the *previous* letter, is a state bug
 * that no component test can see — `QaidaLetterBoard` and `QaidaLetterDetailSheet` are tested
 * separately in `src/testDebug`, each in isolation from the other.
 *
 * **The play event carries the letter, not an id.** `PlayLetter(letter)` is what reaches the
 * audio manager, so sending the wrong one plays a different sound with the right letter on
 * screen — audible, and silent to every other test.
 *
 * With the clock pinned, a bottom sheet's slide-in never finishes, so its content is attached
 * but parked below the viewport: these assert with `assertExists`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class QaidaLettersScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val letters = MutableStateFlow<List<QaidaLetter>>(emptyList())
    private val events = mutableListOf<QaidaReaderEvent>()

    private val viewModel: QaidaReaderViewModel = mockk(relaxed = true) {
        every { this@mockk.letters } returns this@QaidaLettersScreenTest.letters
        every { onEvent(any()) } answers { events += firstArg<QaidaReaderEvent>() }
    }

    private var backs = 0

    private fun setContent() {
        composeRule.setThemedContent {
            QaidaLettersScreen(
                onNavigateBack = { backs++ },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int): String = context.getString(res)

    private companion object {
        /** Five distinct glyphs, so a tap addresses exactly one tile. */
        val GLYPHS = listOf("ا", "ب", "ت", "ث", "ج")
    }

    @Test
    fun `the board renders every letter the catalogue ships`() {
        letters.value = listOf(
            qaidaLetter(1, "ا", "Alif"),
            qaidaLetter(2, "ب", "Ba", isConnecting = true),
            qaidaLetter(3, "ت", "Ta", isConnecting = true),
        )

        setContent()

        composeRule.onNodeWithText("ا").assertIsDisplayed()
        composeRule.onNodeWithText("ب").assertIsDisplayed()
        composeRule.onNodeWithText("ت").assertIsDisplayed()
    }

    @Test
    fun `an empty board renders rather than crashing`() {
        // The letters flow starts empty on every launch, and the board is composed on that
        // frame.
        setContent()

        composeRule.onNodeWithText(string(R.string.qaida_arabic_letters)).assertExists()
    }

    @Test
    fun `tapping a letter opens that letter's sheet`() {
        letters.value = listOf(
            qaidaLetter(1, "ا", "Alif"),
            qaidaLetter(2, "ب", "Ba", isConnecting = true),
        )

        setContent()
        composeRule.onNodeWithText("ب").performClick()
        composeRule.waitForIdle()

        // The sheet names the letter — the board shows the glyph alone.
        composeRule.onNodeWithText("Ba").assertExists()
    }

    @Test
    fun `no sheet is open until a letter is chosen`() {
        letters.value = listOf(qaidaLetter(1, "ا", "Alif"))

        setContent()

        composeRule.onNodeWithText("Alif").assertDoesNotExist()
    }

    @Test
    fun `the sheet shows the chosen letter's reference, not the previous one's`() {
        // `selected` is the screen's own state and the sheet is rendered from it. A stale
        // value shows one glyph's reference under another's, which nothing else can see:
        // `QaidaLetterBoard` and `QaidaLetterDetailSheet` are each tested in isolation from
        // the other in `src/testDebug`.
        letters.value = listOf(
            qaidaLetter(1, "ا", "Alif", phoneticHint = "Like the 'a' in father"),
            qaidaLetter(2, "ب", "Ba", isConnecting = true, phoneticHint = "Like 'b' in boy"),
        )

        setContent()
        composeRule.onNodeWithText("ب").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Ba").assertExists()
        composeRule.onNodeWithText("Like 'b' in boy").assertExists()
        composeRule.onNodeWithText("Like the 'a' in father").assertDoesNotExist()
    }

    @Test
    fun `every articulation point has its own words and its own cue`() {
        // `makhrajLabel` and `makhrajEmoji` are two `when`s over the same five-value enum, and
        // the sheet is the only place either runs. A missing arm is not a compile error for
        // the emoji one — it is `private` and exhaustive today — but a *duplicated* arm is
        // invisible either way, and this line is the whole of what the sheet teaches: where in
        // the mouth the letter is made.
        val areas = listOf(
            MakhrajArea.JAWF to R.string.makhraj_jawf,
            MakhrajArea.HALQ to R.string.makhraj_halq,
            MakhrajArea.LISAN to R.string.makhraj_lisan,
            MakhrajArea.SHAFATAIN to R.string.makhraj_shafatain,
            MakhrajArea.KHAYSHUM to R.string.makhraj_khayshum,
        )
        letters.value = areas.mapIndexed { index, (area, _) ->
            qaidaLetter(
                id = index + 1,
                letterArabic = GLYPHS[index],
                nameTransliteration = "Letter ${index + 1}",
            ).copy(makhrajArea = area, makhrajDetail = "Detail ${index + 1}")
        }

        setContent()

        areas.forEachIndexed { index, (_, labelRes) ->
            composeRule.onNodeWithText(GLYPHS[index]).performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithText(
                context.getString(R.string.qaida_made_with_format, string(labelRes))
            ).assertExists()
            composeRule.onNodeWithText("Detail ${index + 1}").assertExists()
        }
    }

    @Test
    fun `a letter the artifact records no articulation detail for shows only the area`() {
        // `detail.isNotBlank()` guards the second line; without it the sheet renders an empty
        // row under the heading, which reads as a missing translation.
        letters.value = listOf(qaidaLetter(1, "ا", "Alif").copy(makhrajDetail = ""))

        setContent()
        composeRule.onNodeWithText("ا").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(
            context.getString(R.string.qaida_made_with_format, string(R.string.makhraj_jawf))
        ).assertExists()
    }

    @Test
    fun `the play control is absent while the audio UI is switched off`() {
        // `QAIDA_AUDIO_UI_ENABLED` is `false` while the recordings are being regenerated, and
        // the sheet is text-only until it flips. A play button that renders anyway is a
        // control that does nothing — and the flag flipping back is exactly when this
        // assertion should be inverted rather than deleted.
        letters.value = listOf(qaidaLetter(1, "ا", "Alif"))

        setContent()
        composeRule.onNodeWithText("ا").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(string(R.string.qaida_play_letter))
            .assertDoesNotExist()
        assertThat(events).isEmpty()
    }
}
