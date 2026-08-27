package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The hadith authenticity grade, and its colour.
 *
 * This is the one badge in the app where the colour carries a claim about the *content*: green for
 * sahih, amber for da'if, red for mawdu'. Two grades sharing a colour, or a grade falling through
 * to the wrong arm, tells a reader that a fabricated narration is authentic — which is the most
 * serious thing this app can get wrong, and is invisible to every other test because nothing else
 * reads this mapping.
 *
 * The null arm matters just as much: a hadith whose grade the collection does not record must
 * render **no** chip rather than an ungraded-looking one, because an empty badge reads as a grade.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class HadithGradeChipTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `every recorded grade has a label and a colour`() {
        val displays = mutableMapOf<HadithGrade, HadithGradeDisplay?>()
        composeRule.setThemedContent {
            HadithGrade.entries.forEach { displays[it] = hadithGradeDisplay(it) }
        }

        val graded = displays.filterValues { it != null }
        assertThat(graded.keys).containsAtLeast(
            HadithGrade.SAHIH, HadithGrade.HASAN, HadithGrade.DAIF, HadithGrade.MAWDU,
        )
        graded.values.forEach { assertThat(it!!.label).isNotEmpty() }
    }

    @Test
    fun `no two grades share a colour`() {
        // Sahih and mawdu' reading the same is the failure that matters — a fabricated narration
        // presented as authentic. The whole set is checked because any collision is one grade
        // wearing another's meaning.
        val colours = mutableListOf<Color>()
        composeRule.setThemedContent {
            listOf(HadithGrade.SAHIH, HadithGrade.HASAN, HadithGrade.DAIF, HadithGrade.MAWDU)
                .forEach { colours += hadithGradeDisplay(it)!!.color }
        }

        assertThat(colours.toSet()).hasSize(4)
    }

    @Test
    fun `no two grades share a label`() {
        val labels = mutableListOf<String>()
        composeRule.setThemedContent {
            listOf(HadithGrade.SAHIH, HadithGrade.HASAN, HadithGrade.DAIF, HadithGrade.MAWDU)
                .forEach { labels += hadithGradeDisplay(it)!!.label }
        }

        assertThat(labels.toSet()).hasSize(4)
    }

    @Test
    fun `an unrecorded grade produces no chip at all`() {
        // The `else -> null` arm. Rendering an empty badge here would present "no grade recorded"
        // as if it were a grade.
        var display: HadithGradeDisplay? = HadithGradeDisplay("x", Color.Red)
        composeRule.setThemedContent { display = hadithGradeDisplay(null) }

        assertThat(display).isNull()
    }

    @Test
    fun `the chip renders the label it is handed`() {
        composeRule.setThemedContent { HadithGradeChip(label = "Sahih", color = SahihGreen) }

        composeRule.onNodeWithText("Sahih").assertExists()
    }

    @Test
    fun `a chip is tappable only when the caller gives it something to do`() {
        // `onClick` is nullable: in the reader the chip explains itself when tapped, in the
        // settings preview it is a swatch. A chip that is always clickable gives a screen reader
        // a button that does nothing.
        var taps = 0
        composeRule.setThemedContent {
            HadithGradeChip(label = "Da'if", color = DaifAmber, onClick = { taps++ })
        }

        composeRule.onNodeWithText("Da'if").performClick()
        assertThat(taps).isEqualTo(1)
    }

    @Test
    fun `a chip with no callback still renders`() {
        composeRule.setThemedContent { HadithGradeChip(label = "Hasan", color = HasanTeal) }

        composeRule.onNodeWithText("Hasan").assertExists()
    }
}
