package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranRecommendedSurahsTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    private val alKahf =
        Surah(18, "الكهف", "Al-Kahf", "The Cave", RevelationType.MECCAN, 110, 18, 293)
    private val alMulk =
        Surah(67, "الملك", "Al-Mulk", "The Sovereignty", RevelationType.MECCAN, 30, 67, 562)

    @Test
    fun `announces surah name, number and reason as one label`() {
        composeRule.setThemedContent {
            QuranRecommendedSurahs(
                surahs = listOf(alKahf, alMulk),
                isFriday = true,
                onSurahClick = {},
            )
        }
        // The number renders as a decorative ghost numeral, so it is not its own
        // node — but it must still reach a screen reader via the card's label.
        composeRule.onNodeWithContentDescription("Al-Kahf, surah 18, Friday Sunnah")
            .assertExists()
    }

    @Test
    fun `renders the surah name, reason and verse metadata`() {
        composeRule.setThemedContent {
            QuranRecommendedSurahs(
                surahs = listOf(alKahf),
                isFriday = true,
                onSurahClick = {},
            )
        }
        composeRule.onNodeWithText("Al-Kahf", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Friday Sunnah", useUnmergedTree = true).assertExists()
        // Juz 15 comes from the *pagination* now — Al-Kahf opens on Madani page 293 — rather
        // than from a Surah.juzStart the mapper filled with a literal 1 for all 114 rows,
        // which is why every card on this strip used to read "Juz 1".
        composeRule.onNodeWithText("110 Verses · Juz 15", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `clicking a surah card reports its number`() {
        var clicked = -1
        composeRule.setThemedContent {
            QuranRecommendedSurahs(
                surahs = listOf(alKahf),
                isFriday = true,
                onSurahClick = { clicked = it },
            )
        }
        composeRule.onNodeWithContentDescription("Al-Kahf, surah 18, Friday Sunnah").performClick()
        assertThat(clicked).isEqualTo(18)
    }
}
