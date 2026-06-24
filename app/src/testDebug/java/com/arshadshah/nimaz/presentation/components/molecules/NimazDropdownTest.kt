package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers the consolidated dropdown system in one place: the shared [NimazDropdownRow]
 * (selection + action), the [NimazDropdownMenu] action menu, and the [NimazDropdownField]
 * selector built from both.
 */
@RunWith(RobolectricTestRunner::class)
class NimazDropdownTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    // ── NimazDropdownRow ───────────────────────────────────────────────────

    @Test
    fun `row renders text and forwards clicks`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazDropdownRow(text = "Amiri", onClick = { clicked = true })
        }
        composeRule.onNodeWithText("Amiri").assertHasClickAction().performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `selected row renders its description`() {
        composeRule.setThemedContent {
            NimazDropdownRow(
                text = "Scheherazade New",
                selected = true,
                description = "Classic naskh typeface",
                onClick = {}
            )
        }
        composeRule.onNodeWithText("Scheherazade New").assertExists()
        composeRule.onNodeWithText("Classic naskh typeface").assertExists()
    }

    @Test
    fun `disabled row does not forward clicks`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazDropdownRow(text = "Disabled", enabled = false, onClick = { clicked = true })
        }
        composeRule.onNodeWithText("Disabled").performClick()
        assertThat(clicked).isFalse()
    }

    @Test
    fun `destructive action row renders with a leading icon and still emits`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazDropdownRow(
                text = "Reset Journey",
                leadingIcon = Icons.Filled.RestartAlt,
                destructive = true,
                onClick = { clicked = true }
            )
        }
        composeRule.onNodeWithText("Reset Journey").assertIsDisplayed().performClick()
        assertThat(clicked).isTrue()
    }

    // ── NimazDropdownMenu ──────────────────────────────────────────────────

    @Test
    fun `collapsed menu shows none of its rows`() {
        composeRule.setThemedContent {
            NimazDropdownMenu(expanded = false, onDismissRequest = {}) {
                NimazDropdownRow(text = "Reset Journey", onClick = {})
            }
        }
        composeRule.onNodeWithText("Reset Journey").assertDoesNotExist()
    }

    @Test
    fun `expanded menu shows its rows and forwards clicks`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazDropdownMenu(expanded = true, onDismissRequest = {}) {
                NimazDropdownRow(text = "Reset Journey", onClick = { clicked = true })
            }
        }
        composeRule.onNodeWithText("Reset Journey").assertIsDisplayed().performClick()
        assertThat(clicked).isTrue()
    }

    // ── NimazDropdownField ─────────────────────────────────────────────────

    private val fontItems = listOf(
        NimazDropdownItem("amiri", "Amiri"),
        NimazDropdownItem("scheherazade", "Scheherazade New"),
    )

    @Test
    fun `collapsed field shows the selected label and hides options`() {
        composeRule.setThemedContent {
            NimazDropdownField(
                label = "Arabic Font",
                items = fontItems,
                selected = "amiri",
                onSelected = {}
            )
        }
        composeRule.onNodeWithText("Arabic Font").assertExists()
        composeRule.onNodeWithText("Amiri").assertExists()
        composeRule.onNodeWithText("Scheherazade New").assertDoesNotExist()
    }

    @Test
    fun `field shows the placeholder when nothing is selected`() {
        composeRule.setThemedContent {
            NimazDropdownField(
                items = fontItems,
                selected = null,
                placeholder = "Select font",
                onSelected = {}
            )
        }
        composeRule.onNodeWithText("Select font").assertExists()
    }

    @Test
    fun `tapping the field opens the menu and selecting reports the value`() {
        var selected: String? = null
        composeRule.setThemedContent {
            NimazDropdownField(
                items = fontItems,
                selected = "amiri",
                onSelected = { selected = it }
            )
        }
        composeRule.onNodeWithText("Amiri").performClick()
        composeRule.onNodeWithText("Scheherazade New").performClick()
        assertThat(selected).isEqualTo("scheherazade")
    }
}
