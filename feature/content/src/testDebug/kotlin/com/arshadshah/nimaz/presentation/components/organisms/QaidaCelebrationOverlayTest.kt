package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onRoot
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaCelebrationOverlayTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders when visible without crashing`() {
        composeRule.setThemedContent {
            QaidaCelebrationOverlay(
                visible = true,
                stars = 2,
                lessonTitle = "The Letters",
                unlockedTitle = "Joined Letters",
                onNext = {},
                onMap = {},
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }

    @Test
    fun `renders with no unlock without crashing`() {
        composeRule.setThemedContent {
            QaidaCelebrationOverlay(
                visible = true,
                stars = 3,
                lessonTitle = "Joined Letters",
                unlockedTitle = null,
                onNext = {},
                onMap = {},
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }

    @Test
    fun `renders when not visible without crashing`() {
        composeRule.setThemedContent {
            QaidaCelebrationOverlay(
                visible = false,
                stars = 1,
                lessonTitle = "The Letters",
                unlockedTitle = null,
                onNext = {},
                onMap = {},
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }
}
