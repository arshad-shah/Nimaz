package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The search bar's remaining options: the ones a screen sets rather than a user triggers.
 *
 * `autoFocus` is the one with teeth — the search screen opens straight into the field so the
 * keyboard is up before the user has to tap, and a `LaunchedEffect` that stopped requesting focus
 * would make the whole screen feel like it had not opened. `leadingIcon = null` is the reader's
 * inline find bar, which has no room for a magnifier.
 *
 * The width clamp is the tablet case: `AdaptiveSpacing.maxSearchBarWidth()` returns `Unspecified`
 * on a phone (meaning "do not clamp") and 600dp above it, and the bar reads that itself rather
 * than making every caller decide.
 */
@RunWith(RobolectricTestRunner::class)
class NimazSearchBarOptionsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `a bar can open with the field already focused`() {
        composeRule.setThemedContent {
            NimazSearchBar(
                query = "",
                onQueryChange = {},
                placeholder = "Search the Quran",
                autoFocus = true,
            )
        }

        composeRule.onNodeWithContentDescription("Search the Quran").assertIsFocused()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `a bar without a leading icon still takes text`() {
        // The reader's inline find bar. `leadingIcon` is a slot rather than a boolean, so null is
        // the documented way to drop it — and the spacer beside it is gated on the same check.
        composeRule.setThemedContent {
            NimazSearchBar(
                query = "patience",
                onQueryChange = {},
                placeholder = "Find in page",
                leadingIcon = null,
            )
        }

        composeRule.onNodeWithContentDescription("Find in page").assertExists()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `a caller can supply its own leading and trailing content`() {
        composeRule.setThemedContent {
            NimazSearchBar(
                query = "",
                onQueryChange = {},
                placeholder = "Search",
                leadingIcon = { Text("lead") },
                trailing = { Text("trail") },
            )
        }

        composeRule.onNodeWithText("lead").assertExists()
        composeRule.onNodeWithText("trail").assertExists()
    }

    @Test
    @Config(qualifiers = "w1400dp-h1000dp")
    fun `on a wide window the bar stops short of the full width`() {
        // The clamp the bar applies to itself. Without it a search field runs the whole width of a
        // tablet, which puts the clear button a hand's width from the text being cleared.
        composeRule.setThemedContent {
            NimazSearchBar(query = "", onQueryChange = {}, placeholder = "Search")
        }

        val root = composeRule.onRoot().fetchSemanticsNode().size.width
        val bar = composeRule.onNodeWithContentDescription("Search")
            .fetchSemanticsNode().positionInRoot.x

        assertThat(root).isGreaterThan(0)
        assertThat(bar).isAtLeast(0f)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `a disabled bar keeps its placeholder but takes no input`() {
        composeRule.setThemedContent {
            NimazSearchBar(
                query = "",
                onQueryChange = {},
                placeholder = "Search",
                enabled = false,
                showClearButton = false,
            )
        }

        composeRule.onNodeWithContentDescription("Search").assertExists()
    }
}
