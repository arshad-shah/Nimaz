package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazDropdownFieldTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    private val items = listOf(
        NimazDropdownItem("amiri", "Amiri"),
        NimazDropdownItem("scheherazade", "Scheherazade New"),
    )

    @Test
    fun `collapsed field shows selected label and hides options`() {
        composeRule.setThemedContent {
            NimazDropdownField(
                label = "Arabic Font",
                items = items,
                selected = "amiri",
                onSelected = {}
            )
        }
        composeRule.onNodeWithText("Arabic Font").assertExists()
        composeRule.onNodeWithText("Amiri").assertExists()
        // The other option lives in the popup, which is closed initially.
        composeRule.onNodeWithText("Scheherazade New").assertDoesNotExist()
    }

    @Test
    fun `shows placeholder when nothing selected`() {
        composeRule.setThemedContent {
            NimazDropdownField(
                items = items,
                selected = null,
                placeholder = "Select font",
                onSelected = {}
            )
        }
        composeRule.onNodeWithText("Select font").assertExists()
    }

    @Test
    fun `tapping field opens menu and selecting reports value`() {
        var selected: String? = null
        composeRule.setThemedContent {
            NimazDropdownField(
                items = items,
                selected = "amiri",
                onSelected = { selected = it }
            )
        }
        // Open the popup via the trigger (shows the current value).
        composeRule.onNodeWithText("Amiri").performClick()
        // Pick the other option from the now-open menu.
        composeRule.onNodeWithText("Scheherazade New").performClick()
        assertThat(selected).isEqualTo("scheherazade")
    }
}
