package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.assertIsDisplayed
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
class NimazSearchBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders default placeholder when query empty`() {
        composeRule.setThemedContent {
            NimazSearchBar(query = "", onQueryChange = {})
        }

        // Default placeholder is "Search..." and also the field contentDescription
        composeRule.onNodeWithText("Search...").assertExists()
    }

    @Test
    fun `renders custom placeholder when query empty`() {
        composeRule.setThemedContent {
            NimazSearchBar(
                query = "",
                onQueryChange = {},
                placeholder = "Search city or address..."
            )
        }

        composeRule.onNodeWithText("Search city or address...").assertExists()
    }

    @Test
    fun `displays the current query text`() {
        composeRule.setThemedContent {
            NimazSearchBar(query = "Al-Fatiha", onQueryChange = {})
        }

        composeRule.onNodeWithText("Al-Fatiha").assertExists()
    }

    @Test
    fun `typing into field invokes onQueryChange with typed text`() {
        var changed: String? = null
        composeRule.setThemedContent {
            NimazSearchBar(query = "", onQueryChange = { changed = it })
        }

        // The text field exposes the placeholder as its contentDescription
        composeRule.onNodeWithContentDescription("Search...").performTextInput("Mecca")
        assertThat(changed).isEqualTo("Mecca")
    }

    @Test
    fun `clear button is shown when query is not empty`() {
        composeRule.setThemedContent {
            NimazSearchBar(query = "Mecca", onQueryChange = {}, onClear = {})
        }

        composeRule.onNodeWithContentDescription("Clear search").assertExists()
    }

    @Test
    fun `clear button is hidden when query is empty`() {
        composeRule.setThemedContent {
            NimazSearchBar(query = "", onQueryChange = {})
        }

        composeRule.onNodeWithContentDescription("Clear search").assertDoesNotExist()
    }

    @Test
    fun `clear button click invokes onClear`() {
        var cleared = false
        composeRule.setThemedContent {
            NimazSearchBar(query = "Mecca", onQueryChange = {}, onClear = { cleared = true })
        }

        composeRule.onNodeWithContentDescription("Clear search").performClick()
        assertThat(cleared).isTrue()
    }

    @Test
    fun `loading state hides the clear button`() {
        composeRule.setThemedContent {
            NimazSearchBar(
                query = "Mecca",
                onQueryChange = {},
                isLoading = true
            )
        }

        composeRule.onNodeWithContentDescription("Clear search").assertDoesNotExist()
    }

    @Test
    fun `showClearButton false hides clear button even with query`() {
        composeRule.setThemedContent {
            NimazSearchBar(
                query = "Mecca",
                onQueryChange = {},
                showClearButton = false
            )
        }

        composeRule.onNodeWithContentDescription("Clear search").assertDoesNotExist()
    }

    @Test
    fun `trailing slot content is rendered`() {
        composeRule.setThemedContent {
            NimazSearchBar(
                query = "",
                onQueryChange = {},
                trailing = {
                    androidx.compose.material3.Text("FILTER")
                }
            )
        }

        composeRule.onNodeWithText("FILTER").assertIsDisplayed()
    }
}
