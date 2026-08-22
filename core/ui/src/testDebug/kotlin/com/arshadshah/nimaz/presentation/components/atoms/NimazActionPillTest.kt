package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Text
import androidx.compose.ui.test.onNodeWithContentDescription
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
class NimazActionPillTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `pill renders its row content`() {
        composeRule.setThemedContent {
            NimazActionPill { Text("Inside") }
        }
        composeRule.onNodeWithText("Inside").assertExists()
    }

    @Test
    fun `action button renders with content description`() {
        composeRule.setThemedContent {
            NimazPillActionButton(
                icon = Icons.Default.Bookmark,
                contentDescription = "Bookmark",
                onClick = {},
                active = true,
            )
        }
        composeRule.onNodeWithContentDescription("Bookmark").assertExists()
    }

    @Test
    fun `action button fires onClick`() {
        var fired = false
        composeRule.setThemedContent {
            NimazPillActionButton(
                icon = Icons.Default.Bookmark,
                contentDescription = "Bookmark",
                onClick = { fired = true },
            )
        }
        composeRule.onNodeWithContentDescription("Bookmark").performClick()
        assertThat(fired).isTrue()
    }
}
