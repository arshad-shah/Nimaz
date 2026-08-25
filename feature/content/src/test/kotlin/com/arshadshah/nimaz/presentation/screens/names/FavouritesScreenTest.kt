package com.arshadshah.nimaz.presentation.screens.names

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
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
 * Everything the reader has starred, across three catalogues, in one place.
 *
 * The screen this replaced was an all/favourites chip *inside* each of the three name screens,
 * so there was no answer to "what have I saved" — only "what have I saved in this one
 * catalogue", asked three times. Two rules make the consolidated version work, and both are
 * invisible until they break.
 *
 * **A section with nothing in it renders nothing at all** — not an empty heading. The check
 * lives in `favouriteSection` rather than at the three call sites precisely so a fourth kind of
 * favourite cannot forget it; with it forgotten, a reader who has starred one name sees three
 * headings and one card.
 *
 * **The empty state belongs to the whole screen, not to a section.** A reader with two starred
 * prophets and no starred names must see the prophets, not "no favourites yet" — which is what
 * a per-section empty state would produce, three times over.
 *
 * These sections read `listState.favorites`, a different field from the `filteredItems` the
 * Names tabs render. A section wired to the wrong one shows the whole catalogue as "favourites".
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class FavouritesScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val allahState = MutableStateFlow(CatalogListState<AsmaUlHusna>(isLoading = false))
    private val prophetNamesState = MutableStateFlow(CatalogListState<AsmaUnNabi>(isLoading = false))
    private val prophetsState = MutableStateFlow(CatalogListState<Prophet>(isLoading = false))

    private val allahEvents = mutableListOf<CatalogEvent>()
    private val prophetEvents = mutableListOf<CatalogEvent>()

    private val allahViewModel: AsmaUlHusnaViewModel = mockk(relaxed = true) {
        every { listState } returns allahState
        every { onEvent(any()) } answers { allahEvents += firstArg<CatalogEvent>() }
    }
    private val prophetNamesViewModel: AsmaUnNabiViewModel = mockk(relaxed = true) {
        every { listState } returns prophetNamesState
    }
    private val prophetsViewModel: ProphetViewModel = mockk(relaxed = true) {
        every { listState } returns prophetsState
        every { onEvent(any()) } answers { prophetEvents += firstArg<CatalogEvent>() }
    }

    private val opened = mutableListOf<String>()

    private fun setContent() {
        composeRule.setThemedContent {
            FavouritesScreen(
                onNavigateBack = { opened += "back" },
                onNavigateToAsmaUlHusna = { opened += "allah:$it" },
                onNavigateToAsmaUnNabi = { opened += "prophet-name:$it" },
                onNavigateToProphet = { opened += "prophet:$it" },
                asmaUlHusnaViewModel = allahViewModel,
                asmaUnNabiViewModel = prophetNamesViewModel,
                prophetViewModel = prophetsViewModel,
            )
        }
    }

    private fun string(@StringRes res: Int): String = context.getString(res)

    @Test
    fun `nothing starred anywhere shows one empty state, not three`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.no_favorites_yet)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.favourites_empty_message)).assertExists()
        composeRule.onNodeWithText(string(R.string.names_tab_allah)).assertDoesNotExist()
    }

    @Test
    fun `a starred name from one catalogue does not raise the other two headings`() {
        // The failure this catches is a wall of headings over a single card.
        prophetsState.value = CatalogListState(
            favorites = listOf(prophet(1, nameEnglish = "Abraham", isFavorite = true)),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("Abraham").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.names_tab_prophets)).assertExists()
        composeRule.onNodeWithText(string(R.string.names_tab_allah)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.names_tab_prophet)).assertDoesNotExist()
        // Not the whole-screen empty state either.
        composeRule.onNodeWithText(string(R.string.no_favorites_yet)).assertDoesNotExist()
    }

    @Test
    fun `all three kinds appear together, each under its own heading`() {
        allahState.value = CatalogListState(
            favorites = listOf(divineName(1, nameTransliteration = "Ar-Rahman", isFavorite = true)),
            isLoading = false,
        )
        prophetNamesState.value = CatalogListState(
            favorites = listOf(prophetName(1, nameTransliteration = "Muhammad", isFavorite = true)),
            isLoading = false,
        )
        prophetsState.value = CatalogListState(
            favorites = listOf(prophet(1, nameEnglish = "Abraham", isFavorite = true)),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("Ar-Rahman").assertExists()
        composeRule.onNodeWithText("Muhammad").assertExists()
        composeRule.onNodeWithText("Abraham").assertExists()
        composeRule.onNodeWithText(string(R.string.names_tab_allah)).assertExists()
        composeRule.onNodeWithText(string(R.string.names_tab_prophet)).assertExists()
        composeRule.onNodeWithText(string(R.string.names_tab_prophets)).assertExists()
    }

    @Test
    fun `the sections read favourites, not the whole catalogue`() {
        // `items`/`filteredItems` is what the Names tabs render; a section wired to either
        // would present the entire catalogue as the reader's favourites.
        allahState.value = CatalogListState(
            items = List(5) { divineName(it + 1, nameTransliteration = "Name ${it + 1}") },
            filteredItems = List(5) { divineName(it + 1, nameTransliteration = "Name ${it + 1}") },
            favorites = listOf(divineName(3, nameTransliteration = "Name 3", isFavorite = true)),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("Name 3").assertExists()
        composeRule.onNodeWithText("Name 1").assertDoesNotExist()
        composeRule.onNodeWithText("Name 5").assertDoesNotExist()
    }

    @Test
    fun `tapping a favourite opens its own catalogue's detail route`() {
        allahState.value = CatalogListState(
            favorites = listOf(divineName(7, nameTransliteration = "Al-Aziz", isFavorite = true)),
            isLoading = false,
        )
        prophetsState.value = CatalogListState(
            favorites = listOf(prophet(4, nameEnglish = "Moses", isFavorite = true)),
            isLoading = false,
        )

        setContent()
        composeRule.onNodeWithText("Al-Aziz").performClick()
        composeRule.onNodeWithText("Moses").performClick()

        assertThat(opened).containsExactly("allah:7", "prophet:4").inOrder()
    }

    @Test
    fun `unstarring from here goes to that item's own ViewModel`() {
        // Three cards of the same component with three different `onFavoriteClick` lambdas;
        // a swap unstars the wrong catalogue's item and the card the reader tapped stays lit.
        prophetsState.value = CatalogListState(
            favorites = listOf(prophet(4, nameEnglish = "Moses", isFavorite = true)),
            isLoading = false,
        )

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.remove_from_favorites))
            .performClick()

        assertThat(prophetEvents).containsExactly(CatalogEvent.ToggleFavorite(4))
        assertThat(allahEvents).isEmpty()
    }
}
