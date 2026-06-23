package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaLineProgressDotsTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders one dot per total`() {
        composeRule.setThemedContent {
            QaidaLineProgressDots(total = 6, completed = 3)
        }
        composeRule.onAllNodesWithTag("qaida_dot").assertCountEquals(6)
    }

    @Test
    fun `renders no dots when total is zero`() {
        composeRule.setThemedContent {
            QaidaLineProgressDots(total = 0, completed = 0)
        }
        composeRule.onAllNodesWithTag("qaida_dot").assertCountEquals(0)
    }
}
