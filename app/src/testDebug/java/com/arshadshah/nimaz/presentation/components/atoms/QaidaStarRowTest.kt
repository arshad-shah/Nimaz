package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaStarRowTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `describes filled out of max stars`() {
        composeRule.setThemedContent {
            QaidaStarRow(filled = 2)
        }
        composeRule.onNodeWithContentDescription("2 of 3 stars").assertIsDisplayed()
    }

    @Test
    fun `renders max stars by default`() {
        composeRule.setThemedContent {
            QaidaStarRow(filled = 1)
        }
        composeRule.onAllNodesWithTag("qaida_star", useUnmergedTree = true).assertCountEquals(3)
    }

    @Test
    fun `honours custom max count`() {
        composeRule.setThemedContent {
            QaidaStarRow(filled = 3, max = 5)
        }
        composeRule.onNodeWithContentDescription("3 of 5 stars").assertIsDisplayed()
        composeRule.onAllNodesWithTag("qaida_star", useUnmergedTree = true).assertCountEquals(5)
    }
}
