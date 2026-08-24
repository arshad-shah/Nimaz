package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.click
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.presentation.foundation.text.BISMILLAH_TEXT
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The verse row drawn with tajweed colouring, which is a different renderer from the plain one.
 *
 * The colouring is not decoration — it is the second text the reader is looking at, and the rules
 * are tappable so the colour can explain itself (#294). That tap used to exist only on the
 * continuous mushaf page, which left the verse-list reader with coloured words that did nothing.
 *
 * The surah-opening verses are the trap. Their stored tajweed text carries the bismillah, which
 * the header above the list already prints; without stripping it the reader sees it twice.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class QuranAyahItemTajweedTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    /** The stored form: segments of text, each optionally carrying a rule code. */
    private fun tajweed(vararg segments: Pair<String, String?>): String =
        segments.joinToString(",", "[", "]") { (text, rule) ->
            if (rule == null) """{"t":"$text"}""" else """{"t":"$text","r":"$rule"}"""
        }

    private fun ayah(
        ayahNumber: Int = 5,
        textArabic: String = "نص عربي",
        textTajweed: String? = null,
    ) = Ayah(
        id = 2,
        surahNumber = 2,
        ayahNumber = ayahNumber,
        textArabic = textArabic,
        textSimple = "nass",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 0,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
        textTajweed = textTajweed,
    )

    private fun render(
        ayah: Ayah,
        showTajweed: Boolean = true,
        tajweedUnderline: Boolean = false,
    ) {
        composeRule.setThemedContent {
            AyahItem(
                ayah = ayah,
                showTranslation = false,
                arabicFontSize = 28f,
                fontSize = 16f,
                showTajweed = showTajweed,
                tajweedUnderline = tajweedUnderline,
            )
        }
    }

    @Test
    fun `the coloured text is what the reader sees when tajweed is on`() {
        render(ayah(textTajweed = tajweed("نص" to "g", " عربي" to null)))

        composeRule.onAllNodes(hasText("نص", substring = true)).onFirst().assertIsDisplayed()
    }

    @Test
    fun `tajweed off falls back to the plain verse text`() {
        // Same ayah, colouring switched off: still one verse, drawn by the other renderer.
        render(
            ayah(textTajweed = tajweed("نص" to "g", " عربي" to null)),
            showTajweed = false,
        )

        composeRule.onAllNodes(hasText("نص", substring = true)).onFirst().assertIsDisplayed()
    }

    @Test
    fun `a verse with no stored tajweed falls back rather than rendering nothing`() {
        render(ayah(textTajweed = null))

        composeRule.onAllNodes(hasText("نص", substring = true)).onFirst().assertIsDisplayed()
    }

    @Test
    fun `underlining the rules is still the same text`() {
        render(
            ayah(textTajweed = tajweed("نص" to "qs", " عربي" to null)),
            tajweedUnderline = true,
        )

        composeRule.onAllNodes(hasText("نص", substring = true)).onFirst().assertIsDisplayed()
    }

    @Test
    fun `tapping a coloured word explains its rule`() {
        // The whole verse is one rule, so any offset the tap resolves to is annotated.
        render(ayah(textTajweed = tajweed("نص عربي" to "g")))

        // RTL: the verse starts at the right edge, so the first character is there. The centre
        // of a full-width node is past the end of a short line and resolves to no rule at all.
        composeRule.onAllNodes(hasText("نص", substring = true), useUnmergedTree = true)
            .onFirst()
            .performTouchInput { click(Offset(right - left - 4f, centerY)) }
        composeRule.waitForIdle()

        // "Ghunnah" — the sheet names the rule the colour stands for.
        composeRule.onNodeWithText(str(R.string.tajweed_rule_g_name)).assertIsDisplayed()
    }

    @Test
    fun `tapping plain text explains nothing`() {
        render(ayah(textTajweed = tajweed("نص عربي" to null)))

        composeRule.onAllNodes(hasText("نص", substring = true), useUnmergedTree = true)
            .onFirst()
            .performTouchInput { click(Offset(right - left - 4f, centerY)) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(str(R.string.tajweed_rule_g_name)).assertDoesNotExist()
    }

    @Test
    fun `a surah-opening verse does not print the bismillah the header already prints`() {
        val opening = ayah(
            ayahNumber = 1,
            textArabic = "$BISMILLAH_TEXT نص",
            textTajweed = tajweed(BISMILLAH_TEXT to null, " نص" to "g"),
        )

        render(opening)

        composeRule.onAllNodes(hasText(BISMILLAH_TEXT, substring = true)).assertCountEquals(0)
    }
}
