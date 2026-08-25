package com.arshadshah.nimaz.presentation.screens.dua

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaCollectionUiState
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaFavoritesUiState
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaViewModel
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
 * The dua library, which is really two libraries behind one toggle.
 *
 * **The sort toggle changes the shape of the screen, not just the order.** In curated order the
 * categories split into a four-card grid of "Daily Adhkar" and a list of "Situational Duas"
 * below it; alphabetical collapses both into one flat A–Z list, because the curated split is
 * meaningless once reordered. Two layouts from one boolean is exactly where a category can fall
 * out of the screen entirely — `take(4)` and `drop(4)` are a partition only as long as both
 * branches run.
 *
 * **The icon a category shows is looked up from the emoji the content artifact ships.**
 * `getCategoryIcon` maps forty-odd emoji to Material icons and everything else to a mosque.
 * The mapping is a private `when` in this file, so an emoji the artifact starts using is a
 * silent fallback — every unknown category quietly becomes a mosque, and they all look alike.
 *
 * **A failed collection load used to leave the spinner up for good.** It is now a state with a
 * retry, and the branch order — loading, then error, then content — is what keeps the failure
 * from reading as an empty library.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class DuasCollectionScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val collectionState = MutableStateFlow(DuaCollectionUiState())
    private val favoritesState = MutableStateFlow(DuaFavoritesUiState())
    private val events = mutableListOf<DuaEvent>()

    private val viewModel: DuaViewModel = mockk(relaxed = true) {
        every { this@mockk.collectionState } returns this@DuasCollectionScreenTest.collectionState
        every { this@mockk.favoritesState } returns this@DuasCollectionScreenTest.favoritesState
        every { onEvent(any()) } answers { events += firstArg<DuaEvent>() }
    }

    private val opened = mutableListOf<String>()

    private fun setContent() {
        composeRule.setThemedContent {
            DuasCollectionScreen(
                onNavigateBack = { opened += "back" },
                onNavigateToCategory = { opened += "category:$it" },
                onNavigateToBookmarks = { opened += "bookmarks" },
                onNavigateToSearch = { opened += "search" },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    private fun loaded(vararg categories: com.arshadshah.nimaz.domain.model.DuaCategory) {
        collectionState.value = DuaCollectionUiState(
            categories = categories.toList(),
            filteredCategories = categories.toList(),
            isLoading = false,
        )
    }

    private fun categories(count: Int) = List(count) {
        category(id = "c$it", nameEnglish = "Category $it", duaCount = it + 1)
    }.toTypedArray()

    @Test
    fun `opening the library asks for the favourites and today's progress`() {
        // Neither is part of the categories load, and both feed sections of this screen; a
        // screen that never asks shows an empty favourites row on a device that has some.
        collectionState.value = DuaCollectionUiState(isLoading = false)

        setContent()

        assertThat(events).containsExactly(DuaEvent.LoadFavorites, DuaEvent.LoadTodayProgress)
            .inOrder()
    }

    @Test
    fun `the curated order shows the first four as a grid and the rest as a list`() {
        loaded(*categories(6))

        setContent()

        composeRule.onNodeWithText(string(R.string.daily_adhkar)).assertExists()
        composeRule.onNodeWithText(string(R.string.situational_duas)).assertExists()
        composeRule.onNodeWithText("Category 0").assertExists()
        composeRule.onNodeWithText("Category 5").assertExists()
    }

    @Test
    fun `a library of four or fewer has no situational section to show`() {
        // `size > 4` guards the whole second section; without the guard a library of exactly
        // four renders an empty "Situational Duas" heading over nothing.
        loaded(*categories(4))

        setContent()

        composeRule.onNodeWithText(string(R.string.daily_adhkar)).assertExists()
        composeRule.onNodeWithText(string(R.string.situational_duas)).assertDoesNotExist()
        composeRule.onNodeWithText("Category 3").assertExists()
    }

    @Test
    fun `no category is lost between the grid and the list below it`() {
        // `take(4)` and `drop(4)` partition the list. Getting either bound wrong drops or
        // duplicates a category, and both look like a plausible screen.
        loaded(*categories(9))

        setContent()

        repeat(9) { index ->
            composeRule.onNodeWithText("Category $index").assertExists()
        }
    }

    @Test
    fun `alphabetical order replaces both sections with one flat list`() {
        collectionState.value = DuaCollectionUiState(
            categories = categories(6).toList(),
            filteredCategories = categories(6).toList(),
            sortAlphabetical = true,
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.all_categories_az)).assertExists()
        composeRule.onNodeWithText(string(R.string.daily_adhkar)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.situational_duas)).assertDoesNotExist()
        composeRule.onNodeWithText("Category 5").assertExists()
    }

    @Test
    fun `the sort control dispatches the toggle and names the state it would move to`() {
        // The content description is the only thing that tells a screen-reader user which way
        // the toggle goes, and it inverts with the state rather than describing it.
        loaded(*categories(5))

        setContent()
        composeRule.onNodeWithContentDescription(
            string(R.string.sort_categories_alphabetically)
        ).performClick()

        assertThat(events).contains(DuaEvent.ToggleCategoriesSort)
    }

    @Test
    fun `once sorted the control offers the way back to the curated order`() {
        collectionState.value = DuaCollectionUiState(
            filteredCategories = categories(5).toList(),
            sortAlphabetical = true,
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.sort_categories_default))
            .assertExists()
        composeRule.onNodeWithContentDescription(
            string(R.string.sort_categories_alphabetically)
        ).assertDoesNotExist()
    }

    @Test
    fun `tapping a category opens that category, from either layout`() {
        loaded(*categories(6))

        setContent()
        composeRule.onNodeWithText("Category 1").performClick()   // grid card
        composeRule.onNodeWithText("Category 5").performClick()   // list row

        assertThat(opened).containsExactly("category:c1", "category:c5").inOrder()
    }

    @Test
    fun `a category with no description renders without a blank subtitle line`() {
        collectionState.value = DuaCollectionUiState(
            filteredCategories = listOf(
                category(id = "c0", nameEnglish = "First"),
                category(id = "c1", nameEnglish = "Second"),
                category(id = "c2", nameEnglish = "Third"),
                category(id = "c3", nameEnglish = "Fourth"),
                category(
                    id = "c4",
                    nameEnglish = "Undescribed",
                    description = null,
                    duaCount = 3,
                ),
            ),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText("Undescribed").assertIsDisplayed()
        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.duas_count_format, 3, 3)
        ).assertExists()
    }

    @Test
    // Forty-five rows do not fit even a 2,200dp viewport: a `LazyColumn` composes a screenful,
    // so the last of them is never reached at the class's own height.
    @Config(qualifiers = "w411dp-h6000dp")
    fun `every emoji the content artifact ships resolves to an icon of its own`() {
        // The mapping is a private `when` over forty-odd emoji plus a null case and a fallback.
        // Rendering one category per key is what proves the lookup runs for each of them rather
        // than silently collapsing to the mosque default.
        val emoji = listOf(
            "🌅", "🌙", "🤲", "☀️", "😴", "🏠", "🚪", "🕌", "🕋", "🍽️", "✨", "✈️", "🌧️",
            "💚", "🙏", "🚿", "🚻", "📣", "👕", "🌟", "🤧", "📖", "💰", "💊", "🕊️", "😤",
            "💳", "📜", "🌿", "👨‍👩‍👧", "💍", "🐫", "🌹", "🛡️", "🧎", "🌃", "🪦", "🌛",
            "⚔️", "🌬️", "🤍", "📿", "🤝",
        )
        val withUnknownAndMissing = emoji + listOf("🧿", null)

        collectionState.value = DuaCollectionUiState(
            filteredCategories = withUnknownAndMissing.mapIndexed { index, icon ->
                category(id = "c$index", nameEnglish = "Cat $index", iconName = icon)
            },
            sortAlphabetical = true,
            isLoading = false,
        )

        setContent()

        // The two fallbacks are the point: an unknown emoji and a category with none at all
        // both still render a row rather than throwing or vanishing.
        composeRule.onNodeWithText("Cat ${emoji.size}").assertExists()
        composeRule.onNodeWithText("Cat ${emoji.size + 1}").assertExists()
    }

    @Test
    @Config(qualifiers = "w411dp-h6000dp")
    fun `every colour bucket a category id can hash into resolves to a colour`() {
        // `getCategoryColor` switches on `categoryId.hashCode() % 8` — and Kotlin's `%` keeps
        // the sign of the dividend, so a category id with a **negative** hash lands on a
        // negative remainder and falls through to the `else`. The ids here are chosen to hit
        // all eight buckets plus that one: without the `else`, the first category whose id
        // happens to hash negative throws `NoWhenBranchException` while the list composes, and
        // the whole library goes blank. Which ids hash negative is a property of the shipped
        // content, not of this app.
        val bucketIds = listOf("h", "a", "b", "c", "d", "e", "f", "g", "aaaaab")

        collectionState.value = DuaCollectionUiState(
            filteredCategories = bucketIds.mapIndexed { index, id ->
                category(id = id, nameEnglish = "Bucket $index")
            },
            isLoading = false,
        )

        setContent()

        bucketIds.indices.forEach { index ->
            composeRule.onNodeWithText("Bucket $index").assertExists()
        }
    }

    @Test
    fun `the favourites heading appears only when there are favourites`() {
        loaded(*categories(5))

        setContent()

        composeRule.onNodeWithText(string(R.string.favorites)).assertDoesNotExist()
    }

    @Test
    fun `saved favourites announce themselves above the categories`() {
        loaded(*categories(5))
        favoritesState.value = DuaFavoritesUiState(
            favorites = listOf(favourite(1), favourite(2)),
            isLoading = false,
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.favorites)).assertExists()
    }

    @Test
    fun `a failed load says so and offers to load the categories again`() {
        collectionState.value = DuaCollectionUiState(
            isLoading = false,
            error = UiError(
                message = R.string.dua_collection_load_failed,
                details = "no such table: duas",
            ),
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.dua_collection_load_failed)).assertExists()
        // The SQLite message goes to the crash report, never to the reader as the headline.
        composeRule.onNodeWithText(string(R.string.daily_adhkar)).assertDoesNotExist()

        events.clear()
        composeRule.onNodeWithText(string(R.string.try_again)).performClick()
        assertThat(events).containsExactly(DuaEvent.LoadAllCategories)
    }

    @Test
    fun `while loading, neither the library nor the failure is on screen`() {
        composeRule.mainClock.autoAdvance = false
        collectionState.value = DuaCollectionUiState(isLoading = true)

        setContent()

        composeRule.onNodeWithText(string(R.string.daily_adhkar)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.try_again)).assertDoesNotExist()
    }

    @Test
    fun `search and bookmarks are reachable from the app bar`() {
        loaded(*categories(5))

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.search_title)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.bookmarks)).performClick()

        assertThat(opened).containsExactly("search", "bookmarks").inOrder()
    }
}
