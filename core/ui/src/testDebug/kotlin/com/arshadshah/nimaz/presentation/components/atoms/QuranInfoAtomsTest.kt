package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranInfoAtomsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `stat item renders value and label`() {
        composeRule.setThemedContent {
            StatItem(value = "7", label = "Verses")
        }
        composeRule.onNodeWithText("7").assertExists()
        composeRule.onNodeWithText("Verses").assertExists()
    }

}
