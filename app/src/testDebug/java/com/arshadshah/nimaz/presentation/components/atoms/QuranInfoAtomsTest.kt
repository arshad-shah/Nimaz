package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranInfoAtomsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `stat item renders value and label`() {
        composeRule.setThemedContent {
            StatItem(value = "7", label = "Verses")
        }
        composeRule.onNodeWithText("7").assertExists()
        composeRule.onNodeWithText("Verses").assertExists()
    }

    @Test
    fun `info card renders text`() {
        composeRule.setThemedContent {
            InfoCard(text = "Al-Fatihah is the first chapter of the Quran.")
        }
        composeRule.onNodeWithText("Al-Fatihah is the first chapter of the Quran.").assertExists()
    }
}
