package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranVerseOfTheDayCardTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders header arabic translation and reference`() {
        composeRule.setThemedContent {
            VerseOfTheDayCard(
                arabicText = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ",
                translation = "Allah - there is no deity except Him.",
                reference = "Al-Baqarah · 2:255",
                onClick = {},
            )
        }
        composeRule.onNodeWithText("VERSE OF THE DAY").assertExists()
        composeRule.onNodeWithText("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ").assertExists()
        composeRule.onNodeWithText("Allah - there is no deity except Him.").assertExists()
        composeRule.onNodeWithText("Al-Baqarah · 2:255").assertExists()
    }

    @Test
    fun `tapping card invokes onClick`() {
        var fired = false
        composeRule.setThemedContent {
            VerseOfTheDayCard(
                arabicText = "بِسْمِ اللَّهِ",
                translation = null,
                reference = "Al-Fatiha · 1:1",
                onClick = { fired = true },
            )
        }
        composeRule.onNodeWithText("Al-Fatiha · 1:1").performClick()
        assertThat(fired).isTrue()
    }
}
