package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazListPickerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val shortItems = listOf(
        NimazPickerItem(value = "standard", title = "Standard", description = "Shafi'i"),
        NimazPickerItem(value = "hanafi", title = "Hanafi", description = "Twice object length"),
    )

    @Test
    fun `renders title and item titles`() {
        composeRule.setThemedContent {
            NimazListPicker(
                title = "Asr Calculation",
                items = shortItems,
                selected = "standard",
                onSelected = {},
                onDismiss = {},
            )
        }
        composeRule.onNodeWithText("Asr Calculation").assertExists()
        composeRule.onNodeWithText("Standard").assertExists()
        composeRule.onNodeWithText("Hanafi").assertExists()
    }

    @Test
    fun `renders item description`() {
        composeRule.setThemedContent {
            NimazListPicker(
                title = "Asr Calculation",
                items = shortItems,
                selected = "standard",
                onSelected = {},
                onDismiss = {},
            )
        }
        composeRule.onNodeWithText("Shafi'i").assertExists()
    }

    @Test
    fun `selecting an item triggers onSelected and onDismiss with autoDismiss`() {
        var selectedValue: String? = null
        var dismissed = false
        composeRule.setThemedContent {
            NimazListPicker(
                title = "Asr Calculation",
                items = shortItems,
                selected = "standard",
                onSelected = { selectedValue = it },
                onDismiss = { dismissed = true },
            )
        }
        composeRule.onNodeWithText("Hanafi").performClick()
        assertThat(selectedValue).isEqualTo("hanafi")
        assertThat(dismissed).isTrue()
    }

    @Test
    fun `non autoDismiss renders confirm and cancel actions`() {
        var selectedValue: String? = null
        var dismissed = false
        composeRule.setThemedContent {
            NimazListPicker(
                title = "Asr Calculation",
                items = shortItems,
                selected = "standard",
                onSelected = { selectedValue = it },
                onDismiss = { dismissed = true },
                autoDismiss = false,
                confirmText = "Done",
                cancelText = "Cancel",
            )
        }
        // Selecting does not auto-dismiss when autoDismiss = false.
        composeRule.onNodeWithText("Hanafi").performClick()
        assertThat(selectedValue).isEqualTo("hanafi")
        assertThat(dismissed).isFalse()

        // Action buttons exist and Confirm dismisses.
        composeRule.onNodeWithText("Done").assertExists()
        composeRule.onNodeWithText("Cancel").assertExists()
        composeRule.onNodeWithText("Done").performClick()
        assertThat(dismissed).isTrue()
    }

    @Test
    fun `searchable shows search bar and filters items`() {
        val items = listOf(
            NimazPickerItem(value = 1, title = "Muslim World League", description = "Europe"),
            NimazPickerItem(value = 2, title = "Karachi", description = "Pakistan"),
            NimazPickerItem(value = 3, title = "Egyptian", description = "Africa"),
        )
        composeRule.setThemedContent {
            NimazListPicker(
                title = "Calculation Method",
                items = items,
                selected = 2,
                onSelected = {},
                onDismiss = {},
                searchable = true,
                searchPlaceholder = "Search",
            )
        }
        // The picker does not forward searchPlaceholder to NimazSearchBar, so the
        // field exposes the search bar's default placeholder ("Search...") as its
        // contentDescription.
        composeRule.onNodeWithContentDescription("Search...").assertExists()

        composeRule.onNodeWithText("Karachi").assertExists()
        composeRule.onNodeWithText("Egyptian").assertExists()

        composeRule.onNodeWithContentDescription("Search...").performTextInput("Karachi")
        composeRule.onNodeWithText("Karachi").assertExists()
        composeRule.onNodeWithText("Egyptian").assertDoesNotExist()
    }

    @Test
    fun `searchable with no matches shows empty search text`() {
        val items = listOf(
            NimazPickerItem(value = 1, title = "Muslim World League"),
            NimazPickerItem(value = 2, title = "Karachi"),
        )
        composeRule.setThemedContent {
            NimazListPicker(
                title = "Calculation Method",
                items = items,
                selected = null,
                onSelected = {},
                onDismiss = {},
                searchable = true,
                searchPlaceholder = "Search",
                emptySearchText = "No matches",
            )
        }
        composeRule.onNodeWithContentDescription("Search...").performTextInput("zzzz")
        composeRule.onNodeWithText("No matches").assertExists()
    }

    @Test
    fun `grouped items render group headers`() {
        val items = listOf(
            NimazPickerItem(value = "a", title = "Egyptian", group = "Middle East"),
            NimazPickerItem(value = "b", title = "Karachi", group = "South Asia"),
        )
        composeRule.setThemedContent {
            NimazListPicker(
                title = "Calculation Method",
                items = items,
                selected = "a",
                onSelected = {},
                onDismiss = {},
                searchable = false,
            )
        }
        // GroupHeader uppercases the group name.
        composeRule.onNodeWithText("MIDDLE EAST").assertExists()
        composeRule.onNodeWithText("SOUTH ASIA").assertExists()
        composeRule.onNodeWithText("Egyptian").assertExists()
        composeRule.onNodeWithText("Karachi").assertExists()
    }
}
