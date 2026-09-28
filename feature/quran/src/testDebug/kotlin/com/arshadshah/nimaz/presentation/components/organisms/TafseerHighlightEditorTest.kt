package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.*
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h1200dp")
class TafseerHighlightEditorTest {
    @get:Rule val composeRule = createComponentComposeRule()
    private var saved: Pair<String, String>? = null
    private var deleted = false
    private fun render(editing: Boolean = true) {
        composeRule.setThemedContent {
            HighlightEditorSheetContent("a selected passage", "#FDE68A", "a reflection", editing,
                { colour, text -> saved = colour to text }, if (editing) ({ deleted = true }) else null, {})
        }
    }
    @Test fun `highlight editor keeps colour and note together without a notes list`() {
        render()
        composeRule.onNodeWithText(str(R.string.save)).performClick()
        assertThat(saved).isEqualTo("#FDE68A" to "a reflection")
        composeRule.onNodeWithText(str(R.string.tafseer_tab_notes)).assertDoesNotExist()
    }
    @Test fun `deleting a highlight requires confirmation`() {
        render()
        composeRule.onNodeWithText(str(R.string.delete)).performClick()
        assertThat(deleted).isFalse()
        composeRule.onAllNodesWithText(str(R.string.delete)).onLast().performClick()
        assertThat(deleted).isTrue()
    }
    @Test fun `a new highlight has no delete action`() {
        render(false)
        composeRule.onNodeWithText(str(R.string.delete)).assertDoesNotExist()
        composeRule.onNodeWithText("a selected passage").assertIsDisplayed()
    }
}
