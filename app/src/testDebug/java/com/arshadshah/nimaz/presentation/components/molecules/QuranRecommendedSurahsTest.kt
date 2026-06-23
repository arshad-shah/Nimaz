package com.arshadshah.nimaz.presentation.components.molecules

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
class QuranRecommendedSurahsTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    private val alKahf =
        Surah(18, "الكهف", "Al-Kahf", "The Cave", RevelationType.MECCAN, 110, 15, 18, 293)
    private val alMulk =
        Surah(67, "الملك", "Al-Mulk", "The Sovereignty", RevelationType.MECCAN, 30, 29, 67, 562)

    @Test
    fun `renders recommended surah name and number`() {
        composeRule.setThemedContent {
            QuranRecommendedSurahs(
                surahs = listOf(alKahf, alMulk),
                isFriday = true,
                onSurahClick = {},
            )
        }
        composeRule.onNodeWithText("Al-Kahf").assertExists()
        composeRule.onNodeWithText("18").assertExists()
        composeRule.onNodeWithText("Friday Sunnah").assertExists()
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
        composeRule.onNodeWithText("Al-Kahf").performClick()
        assertThat(clicked).isEqualTo(18)
    }
}
