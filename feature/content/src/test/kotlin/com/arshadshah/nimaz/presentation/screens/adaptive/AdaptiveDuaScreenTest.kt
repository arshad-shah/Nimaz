package com.arshadshah.nimaz.presentation.screens.adaptive

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaCategoryUiState
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaCollectionUiState
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaFavoritesUiState
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaReaderUiState
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The dua library on a phone and on a tablet.
 *
 * Same shape as `AdaptiveHadithScreenTest`, and worth asserting separately because the wiring is
 * hand-written per feature rather than shared: opening a category pushes `Route.DuaCategory` on
 * a phone and moves the scaffold's detail pane on a tablet, and the detail pane then renders
 * either the category or the reader depending on whether its args carry a dua.
 *
 * The one difference from hadith is the **args carry the category forward**: opening a dua from
 * the detail pane rebuilds `DuaDetailArgs` with both ids, so going back from the reader lands on
 * the category it came from rather than on an empty pane. Dropping `categoryId` there compiles
 * and looks right until someone navigates back.
 */
@RunWith(RobolectricTestRunner::class)
class AdaptiveDuaScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val collectionState = MutableStateFlow(
        DuaCollectionUiState(
            categories = listOf(morning, evening),
            filteredCategories = listOf(morning, evening),
            isLoading = false,
        )
    )
    private val categoryState = MutableStateFlow(
        DuaCategoryUiState(category = morning, duas = listOf(wakingDua), isLoading = false)
    )
    private val readerState = MutableStateFlow(
        DuaReaderUiState(duas = listOf(wakingDua), isLoading = false)
    )
    private val events = mutableListOf<DuaEvent>()

    private val viewModel: DuaViewModel = mockk(relaxed = true) {
        every { this@mockk.collectionState } returns this@AdaptiveDuaScreenTest.collectionState
        every { this@mockk.categoryState } returns this@AdaptiveDuaScreenTest.categoryState
        every { this@mockk.readerState } returns this@AdaptiveDuaScreenTest.readerState
        every { favoritesState } returns MutableStateFlow(DuaFavoritesUiState(isLoading = false))
        every { onEvent(any()) } answers { events += firstArg<DuaEvent>() }
        every { isDuaFavorite(any()) } returns flowOf(false)
    }

    private val navigated = mutableListOf<Route>()

    private fun setContent() {
        composeRule.setThemedContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides seededOwner()) {
                AdaptiveDuaScreen(
                    onNavigate = { navigated += it },
                    onNavigateBack = {},
                    onNavigateToBookmarks = {},
                    onNavigateToSearch = {},
                )
            }
        }
    }

    private fun string(@StringRes res: Int): String = context.getString(res)

    @Test
    @Config(qualifiers = "w411dp-h2200dp")
    fun `on a phone the collection is the whole screen`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.daily_adhkar)).assertExists()
        composeRule.onNodeWithText("Morning Adhkar").assertExists()
    }

    @Test
    @Config(qualifiers = "w411dp-h2200dp")
    fun `on a phone opening a category pushes its own destination`() {
        setContent()

        composeRule.onNodeWithText("Evening Adhkar").performClick()

        assertThat(navigated).containsExactly(Route.DuaCategory("evening"))
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `on a tablet opening a category moves the detail pane instead of navigating`() {
        setContent()

        composeRule.onNodeWithText("Morning Adhkar").performClick()
        composeRule.waitForIdle()

        assertThat(navigated).isEmpty()
        // The category's own duas, in the pane beside the still-visible list.
        composeRule.onNodeWithText("Upon waking").assertExists()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `on a tablet the detail pane shows the reader once a dua is chosen`() {
        setContent()

        composeRule.onNodeWithText("Morning Adhkar").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Upon waking").performClick()
        composeRule.waitForIdle()

        // The reader's own body, which the category list only ever shows two lines of.
        composeRule.onNodeWithText("Praise be to Allah who gave us life").assertExists()
        assertThat(navigated).isEmpty()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `on a tablet nothing is in the detail pane until something is chosen`() {
        setContent()

        composeRule.onNodeWithText("Upon waking").assertDoesNotExist()
    }

    private fun seededOwner(): ViewModelStoreOwner {
        val store = ViewModelStore()
        seed(store, DuaViewModel::class.java, viewModel)
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

    private companion object {
        val morning = DuaCategory(
            id = "morning",
            nameArabic = "أذكار الصباح",
            nameEnglish = "Morning Adhkar",
            description = "Supplications for the morning",
            iconName = "🌅",
            displayOrder = 1,
            duaCount = 12,
        )
        val evening = morning.copy(
            id = "evening",
            nameEnglish = "Evening Adhkar",
            nameArabic = "أذكار المساء",
            iconName = "🌙",
        )
        val wakingDua = Dua(
            id = "dua_1",
            categoryId = "morning",
            titleArabic = "دعاء",
            titleEnglish = "Upon waking",
            textArabic = "الحمد لله الذي أحيانا",
            textTransliteration = "Alhamdulillahilladhi ahyana",
            textEnglish = "Praise be to Allah who gave us life",
            reference = "Sahih al-Bukhari 6312",
            occasion = DuaOccasion.WAKING_UP,
            benefits = null,
            repeatCount = null,
            audioUrl = null,
            displayOrder = 1,
        )
    }
}
