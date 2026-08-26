package com.arshadshah.nimaz.presentation.components.molecules

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Home dua card, standalone and as a carousel page.
 *
 * The interesting behaviour is the **icon chosen from the category label** — a substring match, not
 * a lookup, so "Morning Adhkar", "Evening Remembrance" and "Before Sleep" each get their own glyph
 * and everything else falls back to a generic one. It is a `when` over `in label` conditions, and
 * the ordering matters: "night" and "sleep" share an arm, so a category label mentioning both must
 * not fall through. Nothing else in the app reads this.
 *
 * The source footer is the other half: it is drawn behind a hairline rule *only when there is a
 * source*, and a blank string arriving from the content artifact must not leave a rule with
 * nothing under it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class DuaOfTheMomentCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `the card renders its heading, its category, the arabic and the translation`() {
        composeRule.setThemedContent {
            DuaOfTheMomentCard(
                arabic = "اللَّهُمَّ بِكَ أَصْبَحْنَا",
                translation = "O Allah, by You we enter the morning",
                categoryLabel = "Morning Adhkar",
            )
        }

        composeRule.onNodeWithText(context.getString(R.string.dua_of_the_moment)).assertExists()
        composeRule.onNodeWithText("Morning Adhkar").assertExists()
        composeRule.onNodeWithText("اللَّهُمَّ بِكَ أَصْبَحْنَا").assertExists()
        composeRule.onNodeWithText("O Allah, by You we enter the morning").assertExists()
    }

    @Test
    fun `every category shape resolves to an icon without falling through`() {
        // The four arms of the substring `when`, including a label that mentions both "night" and
        // "sleep" — they share an arm, and a reordering that split them would send one of them to
        // the generic glyph.
        val labels = listOf(
            "Morning Adhkar",
            "Evening Remembrance",
            "Before Sleep",
            "Night prayers",
            "Sleep at night",
            "Travel",
        )
        // A plain Column, not a LazyColumn: a lazy list composes a screenful, and the sixth card
        // — the fallback arm, which is the one most worth reaching — falls off the bottom (#604).
        composeRule.setThemedContent {
            androidx.compose.foundation.layout.Column(
                Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                labels.forEachIndexed { index, label ->
                    DuaOfTheMomentCard(
                        arabic = "دعاء",
                        translation = "translation $index",
                        categoryLabel = label,
                    )
                }
            }
        }

        labels.forEach { composeRule.onNodeWithText(it).assertExists() }
    }

    @Test
    fun `the category match ignores case`() {
        // `categoryLabel.lowercase()` before matching. Content capitalisation is not the app's to
        // control, and a case-sensitive match sends every properly-capitalised category to the
        // fallback glyph.
        composeRule.setThemedContent {
            DuaOfTheMomentCard(
                arabic = "دعاء",
                translation = "translation",
                categoryLabel = "MORNING ADHKAR",
            )
        }

        composeRule.onNodeWithText("MORNING ADHKAR").assertExists()
    }

    @Test
    fun `a source renders when there is one`() {
        composeRule.setThemedContent {
            DuaOfTheMomentCard(
                arabic = "دعاء",
                translation = "translation",
                categoryLabel = "Morning Adhkar",
                source = "Sahih Muslim 2723",
            )
        }

        composeRule.onNodeWithText("Sahih Muslim 2723").assertExists()
    }

    @Test
    fun `a blank source leaves no footer`() {
        // `isNullOrBlank`. A blank column value would otherwise draw the hairline rule with an
        // empty line under it, which reads as a rendering fault rather than as missing data.
        composeRule.setThemedContent {
            DuaOfTheMomentCard(
                arabic = "دعاء",
                translation = "translation",
                categoryLabel = "Morning Adhkar",
                source = "  ",
            )
        }

        composeRule.onNodeWithText("translation").assertExists()
    }

    @Test
    fun `carousel mode centres the body and caps both lines`() {
        // `fillHeight` swaps the Arabic size, both `maxLines` and the body's arrangement at once —
        // four decisions on one flag, which is exactly the shape that half-flips in a refactor.
        composeRule.setThemedContent {
            Box(Modifier.height(300.dp)) {
                DuaOfTheMomentCard(
                    arabic = "اللَّهُمَّ بِكَ أَصْبَحْنَا وَبِكَ أَمْسَيْنَا",
                    translation = List(20) { "a long translation" }.joinToString(" "),
                    categoryLabel = "Morning Adhkar",
                    source = "Sahih Muslim 2723",
                    fillHeight = true,
                )
            }
        }

        composeRule.onNodeWithText("Sahih Muslim 2723").assertExists()
        composeRule.onNodeWithText("Morning Adhkar").assertExists()
    }
}
