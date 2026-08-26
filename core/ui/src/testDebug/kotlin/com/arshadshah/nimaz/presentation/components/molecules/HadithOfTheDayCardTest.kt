package com.arshadshah.nimaz.presentation.components.molecules

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Home hadith card, which appears both standalone and as a page of the carousel.
 *
 * Two things here are worth a test. The **grade chip colours off a string**, not an enum — the
 * caller passes whatever the collection recorded, so the `when` matches on lowercased spellings
 * including three separate ways of writing *da'if*. A spelling that falls through gets the neutral
 * colour, which is right; a spelling that matched the *wrong* arm would paint a weak narration
 * green. Nothing else in the app reads this table.
 *
 * The other is the **"Read" affordance appearing only when there is somewhere to go**. The card is
 * shown on Home whether or not the hadith can be opened, and a chevron that does nothing is worse
 * than no chevron — issue #161 added the tap target precisely because the card looked tappable and
 * was not.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class HadithOfTheDayCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `the card renders the hadith and its heading`() {
        composeRule.setThemedContent {
            HadithOfTheDayCard(hadith = "The best of you are those who learn the Quran.")
        }

        composeRule.onNodeWithText(context.getString(R.string.hadith_of_the_day)).assertExists()
        composeRule.onNodeWithText("The best of you are those who learn the Quran.").assertExists()
    }

    @Test
    fun `a reference renders when there is one`() {
        composeRule.setThemedContent {
            HadithOfTheDayCard(hadith = "Actions are but by intention", reference = "Bukhari 1")
        }

        composeRule.onNodeWithText("Bukhari 1").assertExists()
    }

    @Test
    fun `a blank reference is treated as none`() {
        // `isNullOrBlank`, not `== null`. Content arrives from a fetched artifact, and an empty
        // string in a column is the shape that reaches this card — rendering it leaves a stray
        // coloured line under the hadith.
        composeRule.setThemedContent {
            HadithOfTheDayCard(hadith = "Actions are but by intention", reference = "   ")
        }

        composeRule.onNodeWithText("Actions are but by intention").assertExists()
    }

    @Test
    fun `a grade renders as a chip`() {
        composeRule.setThemedContent {
            HadithOfTheDayCard(hadith = "Actions are but by intention", grade = "Sahih")
        }

        composeRule.onNodeWithText("Sahih").assertExists()
    }

    @Test
    fun `every spelling of every grade renders`() {
        // Three spellings of da'if and three of mawdu' are matched deliberately, because the
        // collections do not agree with each other. An arm that stopped matching would not throw
        // — it would quietly paint a fabricated narration in the neutral colour.
        val spellings = listOf(
            "Sahih", "hasan", "Da'if", "daif", "dai'f", "mawdu", "mawdu'", "fabricated",
            "Unknown grade",
        )
        // A plain scrolling Column, not a LazyColumn: a lazy list composes a screenful, and the
        // unmatched spelling — the `else` arm — is the last card (#604).
        composeRule.setThemedContent {
            androidx.compose.foundation.layout.Column(
                Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                spellings.forEachIndexed { index, spelling ->
                    HadithOfTheDayCard(hadith = "hadith $index", grade = spelling)
                }
            }
        }

        spellings.forEach { composeRule.onNodeWithText(it).assertExists() }
    }

    @Test
    fun `a blank grade renders no chip`() {
        composeRule.setThemedContent {
            HadithOfTheDayCard(hadith = "Actions are but by intention", grade = " ")
        }

        composeRule.onNodeWithText(context.getString(R.string.read)).assertDoesNotExist()
    }

    @Test
    fun `the read affordance appears only when the card can be opened`() {
        // Issue #161: the card looked tappable and was not. The chevron and the label are the
        // signal, and they are gated on the callback rather than on anything about the content.
        composeRule.setThemedContent {
            HadithOfTheDayCard(hadith = "Actions are but by intention")
        }

        composeRule.onNodeWithText(context.getString(R.string.read)).assertDoesNotExist()
    }

    @Test
    fun `tapping the card opens this hadith`() {
        var opened = 0
        composeRule.setThemedContent {
            HadithOfTheDayCard(hadith = "Actions are but by intention", onClick = { opened++ })
        }

        composeRule.onNodeWithText(context.getString(R.string.read)).assertExists()
        composeRule.onNodeWithText("Actions are but by intention").performClick()
        assertThat(opened).isEqualTo(1)
    }

    @Test
    fun `carousel mode makes the card take the whole page`() {
        // `fillHeight` swaps three modifiers at once — the card, the column and the body's weight
        // — because a short hadith on a tall pager page reads as top-clumped. Asserted by drawing
        // the same hadith both ways in equally tall boxes: the carousel card fills its box and the
        // standalone one wraps its content.
        composeRule.setThemedContent {
            androidx.compose.foundation.layout.Column {
                Box(Modifier.height(300.dp)) {
                    HadithOfTheDayCard(
                        hadith = "Actions are but by intention",
                        reference = "filled",
                        grade = "Sahih",
                        fillHeight = true,
                        maxLines = 4,
                        onClick = {},
                    )
                }
                Box(Modifier.height(300.dp)) {
                    HadithOfTheDayCard(
                        hadith = "Actions are but by intention",
                        reference = "wrapped",
                        onClick = {},
                    )
                }
            }
        }

        val filled = composeRule.onNodeWithText("filled").fetchSemanticsNode().size.height
        val wrapped = composeRule.onNodeWithText("wrapped").fetchSemanticsNode().size.height

        assertThat(filled).isGreaterThan(wrapped)
    }
}
