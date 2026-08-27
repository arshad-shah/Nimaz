package com.arshadshah.nimaz.presentation.screens.adaptive

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.navigation.NamesTab
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.AsmaUlHusna
import com.arshadshah.nimaz.domain.model.AsmaUnNabi
import com.arshadshah.nimaz.domain.model.Prophet
import com.arshadshah.nimaz.presentation.screens.names.divineName
import com.arshadshah.nimaz.presentation.screens.names.prophet
import com.arshadshah.nimaz.presentation.screens.names.prophetName
import com.arshadshah.nimaz.presentation.viewmodel.content.AsmaUlHusnaViewModel
import com.arshadshah.nimaz.presentation.viewmodel.content.AsmaUnNabiViewModel
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogDetailState
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
 * Names on a phone and on a tablet — the one adaptive wrapper serving **three** catalogues.
 *
 * It replaced `AdaptiveAsmaUlHusnaScreen`, `AdaptiveAsmaUnNabiScreen` and
 * `AdaptiveProphetsScreen`, which differed only in which list and which detail they named. One
 * scaffold serves all three because the pane is keyed by `NameDetailArgs`, which carries the
 * **catalogue** as well as the id — and that is exactly what is worth pinning: the ids overlap
 * completely (there is a name 1, a name of the Prophet 1 and a prophet 1), so a pane that
 * dropped the catalogue would open a plausible, wrong page every time rather than failing.
 *
 * As with the dua and hadith wrappers, the phone branch must **navigate** and the tablet branch
 * must not; here the phone branch also `return`s early rather than falling through, so the
 * scaffold below it never composes on a phone.
 */
@RunWith(RobolectricTestRunner::class)
class AdaptiveNamesScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val allahList = MutableStateFlow(
        CatalogListState(
            items = listOf(divineName(1, nameTransliteration = "Ar-Rahman")),
            filteredItems = listOf(divineName(1, nameTransliteration = "Ar-Rahman")),
            isLoading = false,
        )
    )
    private val prophetNameList = MutableStateFlow(
        CatalogListState(
            items = listOf(prophetName(1, nameTransliteration = "Muhammad")),
            filteredItems = listOf(prophetName(1, nameTransliteration = "Muhammad")),
            isLoading = false,
        )
    )
    private val prophetList = MutableStateFlow(
        CatalogListState(
            items = listOf(prophet(1, nameEnglish = "Abraham")),
            filteredItems = listOf(prophet(1, nameEnglish = "Abraham")),
            isLoading = false,
        )
    )

    private val allahDetail = MutableStateFlow(
        CatalogDetailState(
            isLoading = false,
            item = divineName(1, nameTransliteration = "Ar-Rahman", meaning = "Mercy for all"),
        )
    )
    private val prophetNameDetail = MutableStateFlow(
        CatalogDetailState(
            isLoading = false,
            item = prophetName(1, nameTransliteration = "Muhammad", meaning = "Much praised"),
        )
    )
    private val prophetDetail = MutableStateFlow(
        CatalogDetailState(
            isLoading = false,
            item = prophet(1, nameEnglish = "Abraham", storySummary = "Left the idols of his people"),
        )
    )

    private val allahViewModel: AsmaUlHusnaViewModel = mockk(relaxed = true) {
        every { listState } returns allahList
        every { detailState } returns allahDetail
    }
    private val prophetNameViewModel: AsmaUnNabiViewModel = mockk(relaxed = true) {
        every { listState } returns prophetNameList
        every { detailState } returns prophetNameDetail
    }
    private val prophetViewModel: ProphetViewModel = mockk(relaxed = true) {
        every { listState } returns prophetList
        every { detailState } returns prophetDetail
    }

    private val navigated = mutableListOf<Route>()

    private fun setContent(initialTab: NamesTab = NamesTab.ASMA_UL_HUSNA) {
        composeRule.setThemedContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides seededOwner()) {
                AdaptiveNamesScreen(
                    onNavigate = { navigated += it },
                    initialTab = initialTab,
                    onNavigateBack = {},
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h2200dp")
    fun `on a phone the tabbed list is the whole screen`() {
        setContent()

        composeRule.onNodeWithText("Ar-Rahman").assertExists()
        // No detail pane on a phone: the branch returns before the scaffold.
        composeRule.onNodeWithText("Mercy for all").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w411dp-h2200dp")
    fun `on a phone opening a name pushes its own detail route`() {
        setContent()

        composeRule.onNodeWithText("Ar-Rahman").performClick()

        assertThat(navigated).containsExactly(Route.AsmaUlHusnaDetail(1))
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `on a tablet a name opens beside the list rather than over it`() {
        setContent()

        composeRule.onNodeWithText("Ar-Rahman").performClick()
        composeRule.waitForIdle()

        assertThat(navigated).isEmpty()
        composeRule.onNodeWithText("Mercy for all").assertExists()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `the pane opens the catalogue that was asked for, not the id alone`() {
        // Every catalogue has an item 1. `NameDetailArgs` carries the catalogue for exactly
        // this reason — without it the pane opens a plausible, wrong page and never fails.
        setContent(initialTab = NamesTab.PROPHETS)

        composeRule.onNodeWithText("Abraham").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Left the idols of his people").assertExists()
        composeRule.onNodeWithText("Mercy for all").assertDoesNotExist()
        composeRule.onNodeWithText("Much praised").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `a name of the Prophet opens its own pane too`() {
        setContent(initialTab = NamesTab.ASMA_UN_NABI)

        composeRule.onNodeWithText("Muhammad").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Much praised").assertExists()
        composeRule.onNodeWithText("Left the idols of his people").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `favourites navigates from the tablet branch as well`() {
        // Favourites is a destination in its own right, not a pane — it spans all three
        // catalogues, so it has nowhere to sit beside one of them.
        setContent()

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.cd_open_favourites)
        ).performClick()

        assertThat(navigated).containsExactly(Route.Favourites)
    }

    private fun seededOwner(): ViewModelStoreOwner {
        val store = ViewModelStore()
        seed(store, AsmaUlHusnaViewModel::class.java, allahViewModel)
        seed(store, AsmaUnNabiViewModel::class.java, prophetNameViewModel)
        seed(store, ProphetViewModel::class.java, prophetViewModel)
        return object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        }
    }

    private fun <VM : ViewModel> seed(store: ViewModelStore, type: Class<VM>, instance: VM) {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                instance as T
        }
        ViewModelProvider.create(store, factory)[type]
    }
}
