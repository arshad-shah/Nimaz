package com.arshadshah.nimaz.presentation.components.organisms

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The expanding search bar — the one that drops a panel of recent searches and suggestions below
 * itself.
 *
 * Its whole behaviour is a state machine on one private `isExpanded` flag crossed with whether the
 * query is empty, and the two panels it can show are **mutually exclusive**: the recents/popular
 * panel appears only while the field is empty, and the type-ahead list only once something is
 * typed. Both showing at once, or neither, is the failure — and it is invisible from outside
 * because the flag is internal to the composable. Every assertion here therefore drives it the way
 * a user would: tap, type, submit.
 *
 * The collapse points are the other half. Picking a recent search, picking a suggestion, or
 * pressing the keyboard's search key each has to close the panel; one that stays open leaves the
 * results the user asked for hidden behind it.
 *
 * A tall viewport, because the panel is below the field and a phone-height screen clips it (#604).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class ExpandableSearchBarTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val recents = listOf("Al-Fatihah", "patience", "Ayat al-Kursi")
    // Deliberately disjoint from [recents]: a word in both lists renders twice while the panel is
    // open, and every assertion about either half then fails on an ambiguous match rather than on
    // the behaviour under test.
    private val suggestions = listOf("sabr", "prayer", "zakat")

    /** The `BasicTextField`, which carries no content description of its own here. */
    private fun field() = composeRule.onNode(hasSetTextAction())

    /**
     * Opens the panel the way a user does — by tapping the bar itself.
     *
     * Not by tapping the placeholder: that text sits inside the `BasicTextField`'s decoration box,
     * and the field consumes the click as a focus request, so the `Surface`'s `onClick` never
     * fires and the panel stays shut. The tap has to land on the bar outside the field, which is
     * where the search icon is.
     */
    private fun openPanel() {
        composeRule.onRoot().performTouchInput { click(Offset(10f, 30f)) }
    }

    @Test
    fun `the panel is closed until the bar is touched`() {
        composeRule.setThemedContent {
            ExpandableSearchBar(
                query = "",
                onQueryChange = {},
                onSearch = {},
                recentSearches = recents,
                suggestions = suggestions,
            )
        }

        composeRule.onNodeWithText(context.getString(R.string.recent_searches))
            .assertDoesNotExist()
    }

    @Test
    fun `tapping the bar opens the recents panel`() {
        composeRule.setThemedContent {
            ExpandableSearchBar(
                query = "",
                onQueryChange = {},
                onSearch = {},
                recentSearches = recents,
                suggestions = suggestions,
            )
        }

        openPanel()

        composeRule.onNodeWithText(context.getString(R.string.recent_searches)).assertExists()
        recents.forEach { composeRule.onNodeWithText(it).assertExists() }
    }

    @Test
    fun `the popular row is shown alongside the recents`() {
        composeRule.setThemedContent {
            ExpandableSearchBar(
                query = "",
                onQueryChange = {},
                onSearch = {},
                recentSearches = recents,
                suggestions = suggestions,
            )
        }

        openPanel()

        composeRule.onNodeWithText(context.getString(R.string.popular_searches)).assertExists()
    }

    @Test
    fun `with no history the panel shows only the popular row`() {
        // `recentSearches.isNotEmpty()` gates the heading, the clear control and the rows. A first
        // run has none, and a heading over an empty list is the visible bug.
        composeRule.setThemedContent {
            ExpandableSearchBar(
                query = "",
                onQueryChange = {},
                onSearch = {},
                recentSearches = emptyList(),
                suggestions = suggestions,
            )
        }

        openPanel()

        composeRule.onNodeWithText(context.getString(R.string.recent_searches))
            .assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.popular_searches)).assertExists()
    }

    @Test
    fun `only the five most recent searches are offered`() {
        // `take(5)` — the panel sits over the results, so an unbounded history would push the
        // popular row and everything under it off the screen.
        val many = (1..9).map { "search $it" }
        composeRule.setThemedContent {
            ExpandableSearchBar(
                query = "",
                onQueryChange = {},
                onSearch = {},
                recentSearches = many,
            )
        }

        openPanel()

        composeRule.onNodeWithText("search 5").assertExists()
        composeRule.onNodeWithText("search 6").assertDoesNotExist()
    }

    @Test
    fun `picking a recent search reports it and closes the panel`() {
        var picked: String? = null
        composeRule.setThemedContent {
            ExpandableSearchBar(
                query = "",
                onQueryChange = {},
                onSearch = {},
                recentSearches = recents,
                onRecentSearchClick = { picked = it },
            )
        }

        openPanel()
        composeRule.onNodeWithText("patience").performClick()

        assertThat(picked).isEqualTo("patience")
        composeRule.onNodeWithText(context.getString(R.string.recent_searches))
            .assertDoesNotExist()
    }

    @Test
    fun `clearing the history is offered and reported`() {
        var cleared = 0
        composeRule.setThemedContent {
            ExpandableSearchBar(
                query = "",
                onQueryChange = {},
                onSearch = {},
                recentSearches = recents,
                onClearRecentSearches = { cleared++ },
            )
        }

        openPanel()
        composeRule.onNodeWithText(context.getString(R.string.cd_clear)).performClick()

        assertThat(cleared).isEqualTo(1)
    }

    @Test
    fun `picking a suggestion searches for it and closes the panel`() {
        var queried: String? = null
        var searched: String? = null
        composeRule.setThemedContent {
            ExpandableSearchBar(
                query = "",
                onQueryChange = { queried = it },
                onSearch = { searched = it },
                suggestions = suggestions,
            )
        }

        openPanel()
        composeRule.onNodeWithText("zakat").performClick()

        assertThat(queried).isEqualTo("zakat")
        assertThat(searched).isEqualTo("zakat")
        composeRule.onNodeWithText(context.getString(R.string.popular_searches))
            .assertDoesNotExist()
    }

    @Test
    fun `typing swaps the recents panel for the type-ahead list`() {
        // The two panels are mutually exclusive: one is gated on `query.isEmpty()` and the other
        // on `query.isNotEmpty()`. Both visible at once is the failure a single-state test misses.
        composeRule.setThemedContent {
            var query by remember { mutableStateOf("") }
            ExpandableSearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = {},
                recentSearches = recents,
                suggestions = suggestions,
            )
        }

        field().performTextInput("pra")

        composeRule.onNodeWithText(context.getString(R.string.recent_searches))
            .assertDoesNotExist()
        composeRule.onNodeWithText("prayer").assertExists()
    }

    @Test
    fun `the type-ahead list offers only suggestions that match what was typed`() {
        // `filter { it.contains(query, ignoreCase = true) }` — a list that ignored the query would
        // suggest "zakat" to somebody typing "pra".
        composeRule.setThemedContent {
            var query by remember { mutableStateOf("") }
            ExpandableSearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = {},
                suggestions = suggestions,
            )
        }

        field().performTextInput("PRA")

        composeRule.onNodeWithText("prayer").assertExists()
        composeRule.onNodeWithText("zakat").assertDoesNotExist()
    }

    @Test
    fun `picking a type-ahead suggestion searches for it`() {
        var searched: String? = null
        composeRule.setThemedContent {
            var query by remember { mutableStateOf("") }
            ExpandableSearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = { searched = it },
                suggestions = suggestions,
            )
        }

        field().performTextInput("pra")
        composeRule.onNodeWithText("prayer").performClick()

        assertThat(searched).isEqualTo("prayer")
    }

    @Test
    fun `the keyboard's search key submits and closes the panel`() {
        var searched: String? = null
        composeRule.setThemedContent {
            var query by remember { mutableStateOf("") }
            ExpandableSearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = { searched = it },
                suggestions = suggestions,
            )
        }

        field().performTextInput("prayer")
        field().performImeAction()

        assertThat(searched).isEqualTo("prayer")
    }

    @Test
    fun `a query offers a clear control that empties the field`() {
        var query: String? = null
        composeRule.setThemedContent {
            ExpandableSearchBar(
                query = "patience",
                onQueryChange = { query = it },
                onSearch = {},
            )
        }

        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_clear))
            .performClick()

        assertThat(query).isEmpty()
    }

    @Test
    fun `a search in flight shows a spinner instead of the clear control`() {
        // Mutually exclusive by construction — clearing a search that is still running would leave
        // the result of a query that is no longer on screen. The clock is pinned because an
        // indeterminate indicator never lets it idle (#604).
        composeRule.mainClock.autoAdvance = false
        composeRule.setThemedContent {
            ExpandableSearchBar(
                query = "patience",
                onQueryChange = {},
                onSearch = {},
                isLoading = true,
            )
        }

        composeRule.onAllNodesWithContentDescription(context.getString(R.string.cd_clear))
            .assertCountEquals(0)
    }
}
