package com.arshadshah.nimaz.presentation.screens.names

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.navigation.NamesTab
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.AsmaUlHusna
import com.arshadshah.nimaz.domain.model.AsmaUnNabi
import com.arshadshah.nimaz.domain.model.Prophet
import com.arshadshah.nimaz.presentation.viewmodel.content.AsmaUlHusnaViewModel
import com.arshadshah.nimaz.presentation.viewmodel.content.AsmaUnNabiViewModel
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogListState
import com.arshadshah.nimaz.presentation.viewmodel.content.ProphetViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Three catalogues behind one search box and one set of tabs.
 *
 * This screen replaced **three destinations** — three top bars, three search boxes, three
 * all/favourites filters — and the consolidation is the whole of what is worth testing here.
 * Two properties carry it.
 *
 * **One query reaches all three catalogues.** `search()` dispatches `CatalogEvent.Search` to
 * each ViewModel in turn, which is what makes the per-tab match counts on the segmented control
 * mean anything: you can see "Rahman" hits two of the three before tapping into either. A
 * dispatch that reaches only the visible tab's ViewModel looks identical on the tab you are on.
 *
 * **Clearing dispatches `ClearSearch`, not `Search("")`.** They leave the same filtered list, so
 * nothing on screen distinguishes them — but `Search("")` also pushes an empty query into the
 * debounced analytics flow, which exists precisely to record what people type.
 *
 * The counts are suppressed with no query, because they would otherwise just restate how big
 * each catalogue is, on every tab, forever — and the tab labels are what the assertions here
 * read, so the suppression is visible.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class NamesScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val allahState = MutableStateFlow(CatalogListState<AsmaUlHusna>(isLoading = false))
    private val prophetNamesState = MutableStateFlow(CatalogListState<AsmaUnNabi>(isLoading = false))
    private val prophetsState = MutableStateFlow(CatalogListState<Prophet>(isLoading = false))

    private val allahEvents = mutableListOf<CatalogEvent>()
    private val prophetNameEvents = mutableListOf<CatalogEvent>()
    private val prophetEvents = mutableListOf<CatalogEvent>()

    private val allahViewModel: AsmaUlHusnaViewModel = mockk(relaxed = true) {
        every { listState } returns allahState
        every { onEvent(any()) } answers { allahEvents += firstArg<CatalogEvent>() }
    }
    private val prophetNamesViewModel: AsmaUnNabiViewModel = mockk(relaxed = true) {
        every { listState } returns prophetNamesState
        every { onEvent(any()) } answers { prophetNameEvents += firstArg<CatalogEvent>() }
    }
    private val prophetsViewModel: ProphetViewModel = mockk(relaxed = true) {
        every { listState } returns prophetsState
        every { onEvent(any()) } answers { prophetEvents += firstArg<CatalogEvent>() }
    }

    private val opened = mutableListOf<String>()

    private fun setContent(initialTab: NamesTab = NamesTab.ASMA_UL_HUSNA) {
        composeRule.setThemedContent {
            NamesScreen(
                initialTab = initialTab,
                onNavigateBack = { opened += "back" },
                onNavigateToFavourites = { opened += "favourites" },
                onNavigateToAsmaUlHusna = { opened += "allah:$it" },
                onNavigateToAsmaUnNabi = { opened += "prophet-name:$it" },
                onNavigateToProphet = { opened += "prophet:$it" },
                asmaUlHusnaViewModel = allahViewModel,
                asmaUnNabiViewModel = prophetNamesViewModel,
                prophetViewModel = prophetsViewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    private fun populate() {
        allahState.value = CatalogListState(
            items = listOf(divineName(1, nameTransliteration = "Ar-Rahman")),
            filteredItems = listOf(divineName(1, nameTransliteration = "Ar-Rahman")),
            isLoading = false,
        )
        prophetNamesState.value = CatalogListState(
            items = listOf(prophetName(1, nameTransliteration = "Muhammad")),
            filteredItems = listOf(prophetName(1, nameTransliteration = "Muhammad")),
            isLoading = false,
        )
        prophetsState.value = CatalogListState(
            items = listOf(prophet(1, nameEnglish = "Abraham")),
            filteredItems = listOf(prophet(1, nameEnglish = "Abraham")),
            isLoading = false,
        )
    }

    @Test
    fun `the route's tab is the tab that opens`() {
        // `Route.Names(tab)` carries the ordinal, so a deep link or an announcement that names
        // the Prophets tab must not land on the Names of Allah.
        populate()

        setContent(initialTab = NamesTab.PROPHETS)

        composeRule.onNodeWithText("Abraham").assertIsDisplayed()
        composeRule.onNodeWithText("Ar-Rahman").assertDoesNotExist()
    }

    @Test
    fun `each tab shows its own catalogue`() {
        populate()

        setContent()
        composeRule.onNodeWithText("Ar-Rahman").assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.names_tab_prophet)).performClick()
        composeRule.onNodeWithText("Muhammad").assertIsDisplayed()
        composeRule.onNodeWithText("Ar-Rahman").assertDoesNotExist()

        composeRule.onNodeWithText(string(R.string.names_tab_prophets)).performClick()
        composeRule.onNodeWithText("Abraham").assertIsDisplayed()
    }

    @Test
    fun `one query reaches all three catalogues`() {
        // The reason the per-tab counts mean anything. A dispatch that reached only the visible
        // tab would look identical from the tab you are standing on.
        populate()

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.names_search_hint))
            .performTextInput("Rahman")

        assertThat(allahEvents).contains(CatalogEvent.Search("Rahman"))
        assertThat(prophetNameEvents).contains(CatalogEvent.Search("Rahman"))
        assertThat(prophetEvents).contains(CatalogEvent.Search("Rahman"))
    }

    @Test
    fun `clearing the box sends ClearSearch, not an empty query`() {
        // Both leave the same list on screen; only `ClearSearch` keeps an empty string out of
        // the debounced analytics flow that exists to record what people type.
        populate()

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.names_search_hint))
            .performTextInput("Rah")
        composeRule.onNodeWithContentDescription(string(R.string.cd_clear_search)).performClick()

        assertThat(allahEvents).contains(CatalogEvent.ClearSearch)
        assertThat(prophetEvents).contains(CatalogEvent.ClearSearch)
        assertThat(allahEvents).doesNotContain(CatalogEvent.Search(""))
    }

    @Test
    fun `the tab labels carry a match count only while a query is running`() {
        allahState.value = CatalogListState(
            items = List(3) { divineName(it + 1) },
            filteredItems = List(2) { divineName(it + 1) },
            isLoading = false,
        )
        prophetNamesState.value = CatalogListState(isLoading = false)
        prophetsState.value = CatalogListState(isLoading = false)

        setContent()

        // No query: the bare label, because a count here would only restate the catalogue size.
        composeRule.onNodeWithText(string(R.string.names_tab_allah)).assertExists()

        composeRule.onNodeWithContentDescription(string(R.string.names_search_hint))
            .performTextInput("Rah")

        composeRule.onNodeWithText("${string(R.string.names_tab_allah)} (2)").assertExists()
        composeRule.onNodeWithText("${string(R.string.names_tab_prophet)} (0)").assertExists()
    }

    @Test
    fun `tapping a name opens that catalogue's own detail route`() {
        // Three lists of near-identical cards and three navigate lambdas of the same type is
        // exactly the shape where a copy-paste sends a prophet's id to the names route.
        populate()

        setContent()
        composeRule.onNodeWithText("Ar-Rahman").performClick()

        composeRule.onNodeWithText(string(R.string.names_tab_prophets)).performClick()
        composeRule.onNodeWithText("Abraham").performClick()

        assertThat(opened).containsExactly("allah:1", "prophet:1").inOrder()
    }

    @Test
    fun `starring a name is dispatched to that name's own ViewModel`() {
        populate()

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.add_to_favorites)).performClick()

        assertThat(allahEvents).contains(CatalogEvent.ToggleFavorite(1))
        assertThat(prophetEvents).doesNotContain(CatalogEvent.ToggleFavorite(1))
    }

    @Test
    fun `a prophet's card carries the title and era the other two do not`() {
        // The one card that differs: the English name leads, and a title and era ride along.
        populate()

        setContent(initialTab = NamesTab.PROPHETS)

        composeRule.onNodeWithText("Abraham").assertIsDisplayed()
        composeRule.onNodeWithText("Friend of Allah").assertExists()
        composeRule.onNodeWithText("circa 2000 BCE").assertExists()
    }

    @Test
    fun `a search that matches nothing says so in that catalogue's own words`() {
        // Three empty messages, one per catalogue, and `CatalogList` is handed whichever
        // belongs to the tab. A shared string would be the easy wrong answer.
        allahState.value = CatalogListState(
            items = List(3) { divineName(it + 1) },
            filteredItems = emptyList(),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.asma_ul_husna_no_names_found)).assertExists()
        composeRule.onNodeWithText(string(R.string.prophets_no_found)).assertDoesNotExist()
    }

    @Test
    fun `a catalogue still loading shows a spinner rather than an empty message`() {
        // `CatalogList` returns early while loading — without that, every tab claims its
        // catalogue is empty for the whole first frame.
        composeRule.mainClock.autoAdvance = false
        allahState.value = CatalogListState(isLoading = true)

        setContent()

        composeRule.onNodeWithText(string(R.string.asma_ul_husna_no_names_found))
            .assertDoesNotExist()
    }

    @Test
    fun `favourites are reachable from the top bar`() {
        populate()

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.cd_open_favourites))
            .performClick()

        assertThat(opened).containsExactly("favourites")
    }
}
