package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazSectionHeaderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `section header renders title only`() {
        composeRule.setThemedContent {
            NimazSectionHeader(title = "Daily Practice")
        }
        composeRule.onNodeWithText("Daily Practice").assertExists()
    }

    @Test
    fun `section header renders custom trailing content`() {
        composeRule.setThemedContent {
            NimazSectionHeader(
                title = "Custom",
                trailingContent = { Text("Trailing") }
            )
        }
        composeRule.onNodeWithText("Trailing").assertExists()
    }

    @Test
    fun `section header see all fires click`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazSectionHeader(
                title = "Prayer Times",
                showSeeAll = true,
                seeAllText = "View All",
                onSeeAllClick = { clicked = true }
            )
        }
        composeRule.onNodeWithText("View All").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `section header renders trailing text`() {
        composeRule.setThemedContent {
            NimazSectionHeader(title = "Bookmarks", trailingText = "12")
        }
        composeRule.onNodeWithText("12").assertExists()
    }
}
