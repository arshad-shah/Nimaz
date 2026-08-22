package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazDividerTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `divider renders with defaults`() {
        composeRule.setThemedContent {
            NimazDivider(modifier = Modifier.testTag("divider"))
        }
        composeRule.onNodeWithTag("divider").assertExists()
    }

    @Test
    fun `divider renders with custom thickness and alpha`() {
        composeRule.setThemedContent {
            NimazDivider(
                modifier = Modifier.testTag("divider2"),
                thickness = 2.dp,
                alpha = 0.8f
            )
        }
        composeRule.onNodeWithTag("divider2").assertExists()
    }
}
