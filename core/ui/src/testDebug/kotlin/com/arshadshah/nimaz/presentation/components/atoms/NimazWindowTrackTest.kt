package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazWindowTrackTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun window(progress: Float?): @Composable () -> Unit = {
        NimazWindowTrack(
            startLabel = "Suhoor ends",
            startValue = "04:31",
            endLabel = "Iftar",
            endValue = "20:58",
            progress = progress,
        )
    }

    @Test
    fun `both ends are labelled and valued`() {
        composeRule.setThemedContent(window(progress = 0.5f))
        composeRule.onNodeWithText("SUHOOR ENDS").assertIsDisplayed()
        composeRule.onNodeWithText("04:31").assertIsDisplayed()
        composeRule.onNodeWithText("IFTAR").assertIsDisplayed()
        composeRule.onNodeWithText("20:58").assertIsDisplayed()
    }

    @Test
    fun `a null progress renders the whole band with no marker`() {
        composeRule.setThemedContent(window(progress = null))
        composeRule.onNodeWithText("04:31").assertIsDisplayed()
        composeRule.onNodeWithText("20:58").assertIsDisplayed()
    }

    @Test
    fun `progress above one is coerced rather than thrown`() {
        composeRule.setThemedContent(window(progress = 9f))
        composeRule.onNodeWithText("20:58").assertIsDisplayed()
    }

    @Test
    fun `negative progress is coerced rather than thrown`() {
        composeRule.setThemedContent(window(progress = -3f))
        composeRule.onNodeWithText("20:58").assertIsDisplayed()
    }

    @Test
    fun `NaN progress is coerced rather than thrown`() {
        composeRule.setThemedContent(window(progress = Float.NaN))
        composeRule.onNodeWithText("20:58").assertIsDisplayed()
    }

    @Test
    fun `a described band speaks as one node instead of four`() {
        composeRule.setThemedContent {
            NimazWindowTrack(
                startLabel = "Suhoor ends",
                startValue = "04:31",
                endLabel = "Iftar",
                endValue = "20:58",
                progress = 0.5f,
                contentDescription = "Fasting window from 04:31 to 20:58",
            )
        }
        composeRule.onNodeWithContentDescription("Fasting window from 04:31 to 20:58")
            .assertIsDisplayed()
        composeRule.onNodeWithText("04:31").assertDoesNotExist()
    }
}
