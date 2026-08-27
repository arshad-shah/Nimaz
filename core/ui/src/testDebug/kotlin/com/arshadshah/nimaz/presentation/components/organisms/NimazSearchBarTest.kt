package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazSearchBarTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

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
    fun `ask button is hidden when askEnabled is false`() {
        composeRule.setThemedContent {
            NimazSearchBar(
                query = "patience",
                onQueryChange = {},
                showAskButton = true,
                askEnabled = false,
                onAsk = {}
            )
        }

        composeRule.onNodeWithText("Ask").assertDoesNotExist()
    }

    @Test
    fun `ask button is hidden when query is blank`() {
        composeRule.setThemedContent {
            NimazSearchBar(
                query = "",
                onQueryChange = {},
                showAskButton = true,
                askEnabled = true,
                onAsk = {}
            )
        }

        composeRule.onNodeWithText("Ask").assertDoesNotExist()
    }

    @Test
    fun `ask button is shown and invokes onAsk when enabled with text`() {
        var asked = false
        composeRule.setThemedContent {
            NimazSearchBar(
                query = "patience",
                onQueryChange = {},
                showAskButton = true,
                askEnabled = true,
                onAsk = { asked = true }
            )
        }

        composeRule.onNodeWithText("Ask").assertIsDisplayed().performClick()
        assertThat(asked).isTrue()
    }

    @Test
    fun `clear button still works alongside the ask button`() {
        var cleared = false
        composeRule.setThemedContent {
            NimazSearchBar(
                query = "patience",
                onQueryChange = {},
                onClear = { cleared = true },
                showAskButton = true,
                askEnabled = true,
                onAsk = {}
            )
        }

        composeRule.onNodeWithText("Ask").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clear search").performClick()
        assertThat(cleared).isTrue()
    }

    @Test
    fun `ime search action invokes onAsk while the ask affordance is live`() {
        var asked = false
        var searched: String? = null
        composeRule.setThemedContent {
            NimazSearchBar(
                query = "patience",
                onQueryChange = {},
                onSearch = { searched = it },
                showAskButton = true,
                askEnabled = true,
                onAsk = { asked = true }
            )
        }

        composeRule.onNodeWithContentDescription("Search...").performImeAction()
        assertThat(asked).isTrue()
        assertThat(searched).isNull()
    }

    @Test
    fun `ime search action falls back to onSearch when ask is unavailable`() {
        var searched: String? = null
        composeRule.setThemedContent {
            NimazSearchBar(
                query = "patience",
                onQueryChange = {},
                onSearch = { searched = it },
                showAskButton = true,
                askEnabled = false,
                onAsk = {}
            )
        }

        composeRule.onNodeWithContentDescription("Search...").performImeAction()
        assertThat(searched).isEqualTo("patience")
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
